package ubic.gemma.core.analysis.preprocess.filter;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.List;
import java.util.Optional;

/**
 * Filter low-variance or repetitive design elements.
 * <p>
 * The filter expects log2-transformed data. You can use {@link LowVarianceFilter#ensureLog2ScaleAndFilterLowVarianceDesignElements(ExpressionDataDoubleMatrix)}
 * as a convenience.
 * <p>
 * If log2cpm data is encountered, the filter will attempt to use the library sizes from {@link BioAssay#getSequenceReadCount()}
 * to remove the library size correction which introduces variance. This trick does not work if the data has been
 * quantile-normalized or batch-corrected.
 * @author paul
 */
@CommonsLog
public class LowVarianceFilter implements Filter<ExpressionDataDoubleMatrix> {

    /**
     * This threshold is used to determine if a row has too many identical value; a value of N means that the number of distinct values in the
     * expression vector of length M must be at least N * M.
     */
    private static final double MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT = 0.3;

    /**
     * We don't apply the "unique values" filter to matrices with fewer columns than this.
     */
    private static final int MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER = 4;

    /**
     * Ensure that a given data matrix is on a log2 scale as per {@link QuantitationTypeConversionUtils#ensureLog2Scale(ExpressionDataDoubleMatrix, boolean)}
     * and filter out low variance or repetitive design elements (rows).
     * <p>
     * Log2 transform if necessary, do any required filtering prior to analysis. Count data is converted to log2CPM (but
     * we store log2cpm as the processed data, so that is what would generally be used).
     *
     * @throws QuantitationTypeConversionException if data cannot be converted to log2 scale
     */
    public static ExpressionDataDoubleMatrix ensureLog2ScaleAndFilterLowVarianceDesignElements( ExpressionDataDoubleMatrix dmatrix ) throws QuantitationTypeConversionException {
        return new LowVarianceFilter().filter( QuantitationTypeConversionUtils.ensureLog2Scale( dmatrix ) );
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dmatrix ) {
        QuantitationType qt = dmatrix.getQuantitationType();
        Assert.isTrue( qt.getScale() == ScaleType.LOG2, "Data must be on a log2 scale to apply the low-variance filter." );
        if ( QuantitationTypeDetectionUtils.isLog2cpm( qt ) ) {
            if ( qt.getIsBatchCorrected() ) {
                log.warn( "Matrix contains batch-corrected log2cpm data, filtering of repetitive rows might not work very well." );
                return filterLowVarianceDesignElements( dmatrix );
            }
            if ( qt.getIsNormalized() ) {
                log.info( "Matrix contains normalized log2cpm data, filtering of repetitive rows might not work very well." );
                return filterLowVarianceDesignElements( dmatrix );
            }
            return dmatrix.getBestBioAssayDimension()
                    .flatMap( this::getLibrarySize )
                    .map( librarySize -> {
                        log.info( "Matrix contains log2cpm data with known library sizes, the low-variance filter will use values that are not corrected for library size." );
                        DoubleMatrix<CompositeSequence, BioMaterial> unnormalizedMatrix = dmatrix.getMatrix().copy();
                        double[] log2LibrarySize = new double[librarySize.length];
                        for ( int j = 0; j < librarySize.length; j++ ) {
                            log2LibrarySize[j] = Math.log( librarySize[j] + 1.0 ) / Math.log( 2 );
                        }
                        // undo the log2cpm transformation, but keep values in the log2 scale
                        for ( int i = 0; i < unnormalizedMatrix.rows(); i++ ) {
                            for ( int j = 0; j < unnormalizedMatrix.columns(); j++ ) {
                                unnormalizedMatrix.set( i, j, unnormalizedMatrix.get( i, j ) + log2LibrarySize[j] );
                            }
                        }
                        ExpressionDataDoubleMatrix cm2 = new ExpressionDataDoubleMatrix( dmatrix, unnormalizedMatrix );
                        DoubleMatrix<CompositeSequence, BioMaterial> filteredMatrix = filterLowVarianceDesignElements( cm2 ).getMatrix().copy();
                        // reapply the log2cpm transformation
                        for ( int i = 0; i < filteredMatrix.rows(); i++ ) {
                            for ( int j = 0; j < filteredMatrix.columns(); j++ ) {
                                filteredMatrix.set( i, j, filteredMatrix.get( i, j ) - log2LibrarySize[j] );
                            }
                        }
                        return new ExpressionDataDoubleMatrix( dmatrix, filteredMatrix );
                    } )
                    .orElseGet( () -> {
                        log.warn( dmatrix + " contains is log2cpm, but no library sizes were found in the BioAssays, filtering of repetitive rows might not work very well." );
                        return filterLowVarianceDesignElements( dmatrix );
                    } );
        } else {
            return filterLowVarianceDesignElements( dmatrix );
        }
    }

    /**
     * Obtain the library sizes from a BAD
     */
    private Optional<long[]> getLibrarySize( BioAssayDimension bioAssayDimension ) {
        long[] librarySize = new long[bioAssayDimension.getBioAssays().size()];
        List<BioAssay> bioAssays = bioAssayDimension.getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            if ( ba.getSequenceReadCount() != null ) {
                librarySize[i] = ba.getSequenceReadCount();
            } else {
                return Optional.empty();
            }
        }
        return Optional.of( librarySize );
    }

    /**
     * We do this second because doing it first causes some kind of subtle problem ... (round off? I could not
     * really track this down).
     * <p>
     * Remove zero-variance rows, but also rows that have lots of equal values even if variance is non-zero.
     * Filtering out rows that have many identical values helps avoid p-values that clump around specific values in the data.
     * This happens especially for lowly-expressed genes in RNA-seq but can be observed in microarray
     * data that has been manipulated by the submitter e.g. when data is "clipped" (e.g., all values under 10 set to 10).
     */
    private ExpressionDataDoubleMatrix filterLowVarianceDesignElements( ExpressionDataDoubleMatrix dmatrix ) {
        int r = dmatrix.rows();
        dmatrix = ExpressionExperimentFilter.zeroVarianceFilter( dmatrix );
        if ( dmatrix.rows() < r ) {
            log.info( ( r - dmatrix.rows() ) + " rows removed due to low variance" );
        }
        r = dmatrix.rows();

        if ( dmatrix.columns() > MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER ) {
            /* This threshold had been 10^-5, but it's probably too stringent. Also remember
             * the data are log transformed the threshold should be transformed as well (it's not that simple),
             * but that's a minor effect.
             * To somewhat counter the effect of lowering this stringency, increasing the stringency on VALUES_LIMIT may help */
            dmatrix = ExpressionExperimentFilter.tooFewDistinctValues( dmatrix, MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT, 0.001 );
            if ( dmatrix.rows() < r ) {
                log.info( ( r - dmatrix.rows() ) + " rows removed due to too many identical values" );
            }
        }

        return dmatrix;
    }
}
