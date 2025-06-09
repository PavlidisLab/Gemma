package ubic.gemma.core.analysis.preprocess.filter;

import lombok.extern.apachecommons.CommonsLog;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.util.ArrayUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Filter design elements with repetitive values across bioassays.
 * <p>
 * If log2cpm data is encountered, the filter will attempt to use the library sizes from {@link BioAssay#getSequenceReadCount()}
 * to remove the library size correction which introduces variance. This trick does not work if the data has been
 * quantile-normalized or batch-corrected.
 * <p>
 * For quantile-normalized data, we exploit the fact that values of same ranks across assays are deemed to be the same.
 * However, ties are not valued the same because it depends on the number of ties in each assay for that particular
 * rank. To work around this annoyance, we rank-transform the data without averaging ties and apply the low-variance and
 * unique value filter.
 * @author paul
 */
@CommonsLog
public class RepetitiveValuesFilter implements Filter<ExpressionDataDoubleMatrix> {

    /**
     * We don't apply the "unique values" filter to matrices with fewer columns than this.
     */
    private static final int MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER = 4;

    /**
     * This threshold is used to determine if a row has too many identical value; a value of N means that the number of distinct values in the
     * expression vector of length M must be at least N * M.
     */
    private static final double MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT = 0.3;

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dmatrix ) throws NoRowsLeftAfterFilteringException {
        QuantitationType qt = dmatrix.getQuantitationType();

        if ( qt.getIsNormalized() ) {
            log.info( "Matrix contains normalized expression data, filtering of repetitive rows will be based on ranks." );
            return filterRepetitiveValuesByRanks( dmatrix );
        }

        if ( qt.getIsBatchCorrected() ) {
            log.warn( "Matrix contains batch-corrected expression  data, filtering of repetitive will be based on ranks." );
            return filterRepetitiveValuesByRanks( dmatrix );
        }

        if ( QuantitationTypeDetectionUtils.isLog2cpm( qt ) ) {
            long[] librarySize = dmatrix.getBestBioAssayDimension()
                    .flatMap( this::getLibrarySize )
                    .orElse( null );
            if ( librarySize != null ) {
                log.info( "Matrix contains log2cpm data with known library sizes, the repetitive values filter will use values that are not corrected for library size." );
                return filterLog2cpm( dmatrix, librarySize );
            } else {
                log.warn( dmatrix + " contains is log2cpm, but no library sizes were found in the BioAssays, filtering of repetitive rows might not work very well." );
            }
        }

        return filterRepetitiveValues( dmatrix );
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

    private ExpressionDataDoubleMatrix filterLog2cpm( ExpressionDataDoubleMatrix dmatrix, long[] librarySize ) throws NoRowsLeftAfterFilteringException {
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
        DoubleMatrix<CompositeSequence, BioMaterial> filteredMatrix = filterRepetitiveValues( cm2 ).getMatrix().copy();
        // reapply the log2cpm transformation
        for ( int i = 0; i < filteredMatrix.rows(); i++ ) {
            for ( int j = 0; j < filteredMatrix.columns(); j++ ) {
                filteredMatrix.set( i, j, filteredMatrix.get( i, j ) - log2LibrarySize[j] );
            }
        }
        return new ExpressionDataDoubleMatrix( dmatrix, filteredMatrix );
    }

    private ExpressionDataDoubleMatrix filterRepetitiveValuesByRanks( ExpressionDataDoubleMatrix dmatrix ) throws NoRowsLeftAfterFilteringException {
        double[][] ranks = new double[dmatrix.rows()][dmatrix.columns()];
        for ( int j = 0; j < dmatrix.columns(); j++ ) {
            double[] column = dmatrix.getColumnAsDoubles( j );
            double[] sorted = column.clone();
            Arrays.sort( sorted );
            for ( int i = 0; i < column.length; i++ ) {
                // find the rank of the value in the sorted array
                ranks[i][j] = ArrayUtils.binarySearchFirst( sorted, column[i] );
            }
        }
        DenseDoubleMatrix<CompositeSequence, BioMaterial> rankMatrix = new DenseDoubleMatrix<>( ranks );
        rankMatrix.setRowNames( dmatrix.getMatrix().getRowNames() );
        rankMatrix.setColumnNames( dmatrix.getMatrix().getColNames() );
        ExpressionDataDoubleMatrix kept = filterRepetitiveValues( new ExpressionDataDoubleMatrix( dmatrix, rankMatrix ) );
        return new ExpressionDataDoubleMatrix( dmatrix, kept.getDesignElements() );
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
     */
    private ExpressionDataDoubleMatrix filterRepetitiveValues( ExpressionDataDoubleMatrix dmatrix ) throws NoRowsLeftAfterFilteringException {
        int r = dmatrix.rows();
        dmatrix = new ZeroVarianceFilter().filter( dmatrix );
        if ( dmatrix.rows() < r ) {
            log.info( ( r - dmatrix.rows() ) + " rows removed due to low variance" );
        }
        r = dmatrix.rows();
        if ( dmatrix.columns() >= MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER ) {
            /* This threshold had been 10^-5, but it's probably too stringent. Also remember
             * the data are log transformed the threshold should be transformed as well (it's not that simple),
             * but that's a minor effect.
             * To somewhat counter the effect of lowering this stringency, increasing the stringency on VALUES_LIMIT may help */
            dmatrix = new TooFewDistinctValuesFilter( MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT ).filter( dmatrix );
            if ( dmatrix.rows() < r ) {
                log.info( ( r - dmatrix.rows() ) + " rows removed due to too many identical values" );
            }
        }
        if ( dmatrix.rows() == 0 ) {
            throw new NoRowsLeftAfterFilteringException( "No rows left after filtering for repetitive values in " + dmatrix.getQuantitationType() );
        }
        return dmatrix;
    }

    @Override
    public String toString() {
        return String.format( "RepetitiveValuesFilter Minimum Samples=%d Minimum Distinct Values=%f%%",
                MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER, 100 * MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT );
    }
}
