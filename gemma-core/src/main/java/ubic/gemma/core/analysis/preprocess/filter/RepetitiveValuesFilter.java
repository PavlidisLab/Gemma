package ubic.gemma.core.analysis.preprocess.filter;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.util.MatrixStats;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils.ensureLog2Scale;

/**
 * Filter design elements with repetitive values across bioassays.
 * <p>
 * If log2cpm data is encountered, the filter will attempt to use the library sizes from {@link BioAssay#getSequenceReadCount()}
 * to remove the library size correction which introduces variance. This trick does not work if the data has been
 * quantile-normalized or batch-corrected.
 * <p>
 * For quantile-normalized data, we exploit the fact that values of same ranks across assays are deemed to be the same.
 * However, ties are not valued the same because it depends on the number of ties in each assay for that particular
 * rank. To work around this annoyance, we rank-transform the data without averaging ties and apply the the unique value
 * filter.
 * @author paul
 */
@CommonsLog
public class RepetitiveValuesFilter implements Filter<ExpressionDataDoubleMatrix> {

    private int minimumColumnsToApplyUniqueValuesFilter = 4;

    private double minimumUniqueValuesFractionPerElement = 0.3;

    /**
     * Minimum number of samples (columns) in the matrix to apply the unique values filter.
     * <p>
     * We don't apply the "unique values" filter to matrices with fewer columns than this.
     */
    public void setMinimumColumnsToApplyUniqueValuesFilter( int minimumColumnsToApplyUniqueValuesFilter ) {
        Assert.isTrue( minimumColumnsToApplyUniqueValuesFilter >= 0 );
        this.minimumColumnsToApplyUniqueValuesFilter = minimumColumnsToApplyUniqueValuesFilter;
    }

    /**
     * This threshold is used to determine if a row has too many identical value; a value of N means that the number of
     * distinct values in the expression vector of length M must be at least N * M.
     */
    public void setMinimumUniqueValuesFractionPerElement( double minimumUniqueValuesFractionPerElement ) {
        Assert.isTrue( minimumUniqueValuesFractionPerElement >= 0 && minimumUniqueValuesFractionPerElement <= 1.0,
                "Minimum unique values fraction per element must be between 0 and 1." );
        this.minimumUniqueValuesFractionPerElement = minimumUniqueValuesFractionPerElement;
    }

    /**
     * We do this second because doing it first causes some kind of subtle problem ... (round off? I could not
     * really track this down).
     * <p>
     * Remove zero-variance rows, but also rows that have lots of equal values even if variance is non-zero.
     * <p>
     * Filtering out rows that have many identical values helps avoid p-values that clump around specific values in the data.
     * This happens especially for lowly-expressed genes in RNA-seq but can be observed in microarray
     * data that has been manipulated by the submitter e.g. when data is "clipped" (e.g., all values under 10 set to 10).
     *
     * @throws InsufficientSamplesException if there are not enough columns in the matrix to apply the unique values
     *                                      filter and the zero-variance filter is not applicable.
     * @throws NoDesignElementsException    if no rows are left after filtering for zero-variance or unique values
     */
    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dmatrix ) throws FilteringException {
        QuantitationType qt = dmatrix.getQuantitationType();

        if ( qt.getIsNormalized() ) {
            log.info( "Matrix contains normalized expression data, filtering of repetitive values will be based on ranks." );
            return filterDistinctValuesByRanks( filterZeroVariance( dmatrix ), rank( dmatrix ) );
        }

        if ( qt.getIsBatchCorrected() ) {
            log.warn( "Matrix contains batch-corrected expression data, filtering of repetitive values will be based on ranks." );
            return filterDistinctValuesByRanks( filterZeroVariance( dmatrix ), rank( dmatrix ) );
        }

        if ( qt.getType() == StandardQuantitationType.COUNT ) {
            log.info( "Matrix contains counts, filtering of repetitive rows will be based on the actual values." );
            return filterDistinctValues( dmatrix );
        }

        if ( QuantitationTypeDetectionUtils.isLog2cpm( qt ) ) {
            long[] librarySize = dmatrix.getBestBioAssayDimension().flatMap( this::getLibrarySize ).orElse( null );
            if ( librarySize != null ) {
                log.info( "Matrix contains log2cpm data with known library sizes, the repetitive values filter will use values that are not corrected for library size." );
                return filterLog2cpm( dmatrix, librarySize );
            } else {
                log.warn( dmatrix + " contains is log2cpm, but no library sizes were found in the BioAssays, filtering of repetitive values will use ranks." );
                return filterDistinctValuesByRanks( dmatrix, rank( dmatrix ) );
            }
        }

        if ( QuantitationTypeDetectionUtils.isLogTransformed( qt ) ) {
            return filterDistinctValuesByRanks( filterZeroVariance( dmatrix ), rank( dmatrix ) );
        } else {
            log.warn( "Matrix does not contain log-transformed data, will attempt to convert it to log2..." );
            try {
                ExpressionDataDoubleMatrix kept = filterDistinctValuesByRanks( filterZeroVariance( ensureLog2Scale( dmatrix ) ), rank( dmatrix ) );
                return dmatrix.sliceRows( kept.getDesignElements() );
            } catch ( QuantitationTypeConversionException e ) {
                throw new FilteringException( "Failed to convert matrix to log2 scale for filtering", e );
            }
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

    private ExpressionDataDoubleMatrix filterLog2cpm( ExpressionDataDoubleMatrix dmatrix, long[] librarySize ) throws NoDesignElementsException {
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
        ExpressionDataDoubleMatrix filteredMatrix = filterZeroVariance( new ExpressionDataDoubleMatrix( dmatrix, unnormalizedMatrix ) );
        List<CompositeSequence> kept = new ArrayList<>( dmatrix.getDesignElements() );
        kept.retainAll( filteredMatrix.getDesignElements() );
        return dmatrix.sliceRows( kept );
    }

    private ExpressionDataDoubleMatrix filterZeroVariance( ExpressionDataDoubleMatrix dmatrix ) throws NoDesignElementsException {
        int r = dmatrix.rows();
        ZeroVarianceFilter zeroVarianceFilter = new ZeroVarianceFilter();
        ExpressionDataDoubleMatrix filteredMatrix = zeroVarianceFilter.filter( dmatrix );
        if ( filteredMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( "No rows left after filtering for repetitive values with " + zeroVarianceFilter + "." );
        } else if ( filteredMatrix.rows() < r ) {
            log.info( ( r - filteredMatrix.rows() ) + " rows removed due to zero variance" );
        }
        return filteredMatrix;
    }

    private ExpressionDataDoubleMatrix filterDistinctValuesByRanks( ExpressionDataDoubleMatrix dmatrix, ExpressionDataDoubleMatrix ranks ) throws NoDesignElementsException {
        List<CompositeSequence> kept = new ArrayList<>( dmatrix.getDesignElements() );
        kept.retainAll( filterDistinctValues( ranks ).getDesignElements() );
        return dmatrix.sliceRows( kept );
    }

    private ExpressionDataDoubleMatrix rank( ExpressionDataDoubleMatrix dmatrix ) {
        DenseDoubleMatrix<CompositeSequence, BioMaterial> rankMatrix = new DenseDoubleMatrix<>( MatrixStats.ranksByColumn( dmatrix.getMatrix() ).toArray() );
        rankMatrix.setRowNames( dmatrix.getMatrix().getRowNames() );
        rankMatrix.setColumnNames( dmatrix.getMatrix().getColNames() );
        return new ExpressionDataDoubleMatrix( dmatrix, rankMatrix );
    }

    /**
     * @throws NoDesignElementsException if no rows are left after filtering and noOtherRowsAreKept is true
     */
    private ExpressionDataDoubleMatrix filterDistinctValues( ExpressionDataDoubleMatrix dmatrix ) throws NoDesignElementsException {
        ExpressionDataDoubleMatrix filteredMatrix = dmatrix;
        if ( dmatrix.columns() >= minimumColumnsToApplyUniqueValuesFilter ) {
            int r = filteredMatrix.rows();
            /* This threshold had been 10^-5, but it's probably too stringent. Also remember
             * the data are log transformed the threshold should be transformed as well (it's not that simple),
             * but that's a minor effect.
             * To somewhat counter the effect of lowering this stringency, increasing the stringency on VALUES_LIMIT may help */
            TooFewDistinctValuesFilter distinctValuesFilter = new TooFewDistinctValuesFilter( minimumUniqueValuesFractionPerElement );
            filteredMatrix = distinctValuesFilter.filter( filteredMatrix );
            if ( filteredMatrix.rows() == 0 ) {
                throw new NoDesignElementsException( "No rows left after filtering for repetitive values with " + distinctValuesFilter + "." );
            } else if ( filteredMatrix.rows() < r ) {
                log.info( ( r - filteredMatrix.rows() ) + " rows removed due to too many identical values" );
            }
        }
        return filteredMatrix;
    }

    @Override
    public String toString() {
        return String.format( "RepetitiveValuesFilter Minimum Samples To Apply Filter=%d Minimum Distinct Values=%f%%",
                minimumColumnsToApplyUniqueValuesFilter, 100 * minimumUniqueValuesFractionPerElement );
    }
}
