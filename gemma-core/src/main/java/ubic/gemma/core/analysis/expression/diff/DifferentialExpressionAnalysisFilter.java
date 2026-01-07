package ubic.gemma.core.analysis.expression.diff;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.text.TextStringBuilder;
import ubic.gemma.core.analysis.preprocess.filter.*;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;

import static ubic.gemma.core.analysis.preprocess.filter.ExpressionDataFilterUtils.getSamplesWithData;

/**
 * Filter used for performing DEA.
 *
 * @author poirigui
 */
@CommonsLog
public class DifferentialExpressionAnalysisFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    public static final int DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_SAMPLE = MinimumCellsFilter.DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_SAMPLE;
    public static final int DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_GENE = MinimumCellsFilter.DEFAULT_MINIMUM_NUMBER_OF_CELLS_PER_GENE;
    public static final RepetitiveValuesFilterMode DEFAULT_REPETITIVE_VALUES_FILTER_MODE = RepetitiveValuesFilterMode.fromRVFM( RepetitiveValuesFilter.DEFAULT_MODE );
    public static final int DEFAULT_MINIMUM_NUMBER_OF_SAMPLES_TO_APPLY_REPETITIVE_VALUES_FILTER = RepetitiveValuesFilter.DEFAULT_MINIMUM_NUMBER_OF_SAMPLES_TO_APPLY_FILTER;
    public static final double DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES = RepetitiveValuesFilter.DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES;
    public static final double DEFAULT_MINIMUM_VARIANCE = 1e-2;

    public enum RepetitiveValuesFilterMode {
        AUTODETECT,
        NOMINAL,
        RANK;

        public static RepetitiveValuesFilterMode fromRVFM( RepetitiveValuesFilter.Mode defaultMode ) {
            switch ( defaultMode ) {
                case AUTODETECT:
                    return AUTODETECT;
                case RANK:
                    return RANK;
                case NOMINAL:
                    return NOMINAL;
                default:
                    throw new IllegalArgumentException( "Unsupported filter mode " + defaultMode + "." );
            }
        }

        private RepetitiveValuesFilter.Mode toRVFM() {
            switch ( this ) {
                case AUTODETECT:
                    return RepetitiveValuesFilter.Mode.AUTODETECT;
                case RANK:
                    return RepetitiveValuesFilter.Mode.RANK;
                case NOMINAL:
                    return RepetitiveValuesFilter.Mode.NOMINAL;
                default:
                    throw new IllegalArgumentException( "Unsupported filter mode " + this + "." );
            }
        }
    }

    private final OutliersFilter outliersFilter = new OutliersFilter();
    private final MinimumCellsFilter minimumCellsFilter;
    private final RepetitiveValuesFilter repetitiveValuesFilter;
    private final LowVarianceFilter lowVarianceFilter;

    public DifferentialExpressionAnalysisFilter( DifferentialExpressionAnalysisConfig config ) {
        minimumCellsFilter = new MinimumCellsFilter();
        if ( config.getMinimumNumberOfCellsPerSample() != null ) {
            minimumCellsFilter.setMinimumNumberOfCellsPerSample( config.getMinimumNumberOfCellsPerSample() );
        }
        if ( config.getMinimumNumberOfCellsPerGene() != null ) {
            minimumCellsFilter.setMinimumNumberOfCellsPerGene( config.getMinimumNumberOfCellsPerGene() );
        }
        repetitiveValuesFilter = new RepetitiveValuesFilter();
        if ( config.getRepetitiveValuesFilterMode() != null ) {
            repetitiveValuesFilter.setMode( config.getRepetitiveValuesFilterMode().toRVFM() );
        }
        if ( config.getMinimumNumberOfSamplesToApplyRepetitiveValuesFilter() != null ) {
            repetitiveValuesFilter.setMinimumNumberOfSamplesToApplyFilter( config.getMinimumNumberOfSamplesToApplyRepetitiveValuesFilter() );
        }
        if ( config.getMinimumFractionOfUniqueValues() != null ) {
            repetitiveValuesFilter.setMinimumFractionOfUniqueValues( config.getMinimumFractionOfUniqueValues() );
        }
        if ( config.getMinimumVariance() != null ) {
            lowVarianceFilter = new LowVarianceFilter( config.getMinimumVariance() );
        } else {
            lowVarianceFilter = new LowVarianceFilter( DEFAULT_MINIMUM_VARIANCE );
        }
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) throws FilteringException {
        return filter( dataMatrix, new DifferentialExpressionAnalysisFilterResult() );
    }

    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix, DifferentialExpressionAnalysisFilterResult filterResult ) throws FilteringException {
        filterResult.setStartingDesignElements( dataMatrix.rows() );
        filterResult.setStartingSamples( getSamplesWithData( dataMatrix ) );
        if ( dataMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( "No design elements in input data matrix." );
        }

        dataMatrix = outliersFilter.filter( dataMatrix );
        filterResult.setOutliersFilterApplied( outliersFilter.appliesTo( dataMatrix ) );
        filterResult.setDesignElementsAfterOutliers( dataMatrix.rows() );
        // the outlier filter works on assays, so this will be affected only if all assays for a biomaterial are masked
        filterResult.setSamplesAfterOutliers( getSamplesWithData( dataMatrix ) );
        if ( filterResult.getSamplesAfterOutliers().isEmpty() ) {
            throw new NoSamplesException( "All samples were filtered out after masking outlier assays." );
        }
        if ( dataMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( "All design elements were filtered out due to minimum cells requirement." );
        }

        dataMatrix = minimumCellsFilter.filter( dataMatrix );
        filterResult.setMinimumCellsFilterApplied( minimumCellsFilter.appliesTo( dataMatrix ) );
        filterResult.setDesignElementsAfterMinimumCells( dataMatrix.rows() );
        filterResult.setSamplesAfterMinimumCells( getSamplesWithData( dataMatrix ) );
        if ( filterResult.getSamplesAfterMinimumCells().isEmpty() ) {
            throw new NoSamplesException( "All samples were filtered out due to minimum cells requirement." );
        }
        if ( dataMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( "All design elements were filtered out due to minimum cells requirement." );
        }

        dataMatrix = repetitiveValuesFilter.filter( dataMatrix );
        filterResult.setRepetitiveValuesFilterApplied( repetitiveValuesFilter.appliesTo( dataMatrix ) );
        filterResult.setDesignElementsAfterRepetitiveValues( dataMatrix.rows() );
        if ( dataMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( "All design elements were filtered out due to repetitive values." );
        }

        if ( QuantitationTypeUtils.isLog2cpm( dataMatrix.getQuantitationType() ) ) {
            dataMatrix = lowVarianceFilter.filter( dataMatrix );
            filterResult.setLowVarianceFilterApplied( true );
            filterResult.setDesignElementsAfterLowVariance( dataMatrix.rows() );
            if ( dataMatrix.rows() == 0 ) {
                throw new NoDesignElementsException( "All design elements were filtered out due to low variance." );
            }
        } else {
            log.info( "Data is not in log2cpm, skipping the low variance filter." );
            filterResult.setLowVarianceFilterApplied( false );
            filterResult.setDesignElementsAfterLowVariance( dataMatrix.rows() );
        }

        filterResult.setFinalDesignElements( dataMatrix.rows() );
        filterResult.setFinalSamples( getSamplesWithData( dataMatrix ) );

        if ( filterResult.getFinalSamples().isEmpty() ) {
            throw new NoSamplesException( "All samples were filtered out." );
        }

        log.info( String.format( "Filter summary for %s of %s:\n%s", dataMatrix.getQuantitationType(),
                dataMatrix.getExpressionExperiment(), describeFilterResult( filterResult ) ) );

        return dataMatrix;
    }

    public String describeFilterResult( DifferentialExpressionAnalysisFilterResult filterResult ) {
        int[] widths = { 23, 15, 7 };
        TextStringBuilder buf = new TextStringBuilder();
        buf.append( "===============================================\n" );
        buf.appendFixedWidthPadRight( "Filter Applied", widths[0], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( "Design Elements", widths[1], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( "Samples", widths[2], ' ' )
                .append( "\n" );
        buf.appendFixedWidthPadRight( "Started with:", widths[0], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( filterResult.getStartingDesignElements(), widths[1], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( filterResult.getStartingSamples().size(), widths[2], ' ' )
                .append( "\n" );
        if ( filterResult.isOutliersFilterApplied() ) {
            buf.appendFixedWidthPadRight( "After MaskOutliers:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getDesignElementsAfterOutliers(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getSamplesAfterOutliers().size(), widths[2], ' ' )
                    .append( "\n" );
        }
        if ( filterResult.isMinimumCellsFilterApplied() )
            buf.appendFixedWidthPadRight( "After MinCells:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getDesignElementsAfterMinimumCells(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getSamplesAfterMinimumCells().size(), widths[2], ' ' )
                    .append( "\n" );
        if ( filterResult.isRepetitiveValuesFilterApplied() )
            buf.appendFixedWidthPadLeft( "After RepetitiveValues:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getDesignElementsAfterRepetitiveValues(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getSamplesAfterMinimumCells().size(), widths[2], ' ' )
                    .append( "\n" );
        if ( filterResult.isRepetitiveValuesFilterApplied() )
            buf.appendFixedWidthPadRight( "After LowVar:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getDesignElementsAfterLowVariance(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( filterResult.getSamplesAfterMinimumCells().size(), widths[2], ' ' )
                    .append( "\n" );
        buf.appendFixedWidthPadRight( "Ended with:", widths[0], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( filterResult.getFinalDesignElements(), widths[1], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( filterResult.getFinalSamples().size(), widths[2], ' ' )
                .append( "\n" );
        buf.append( "===============================================" );
        return buf.toString();
    }

    @Override
    public String toString() {
        return String.format( "DifferentialExpressionAnalysisFilter [%s] -> [%s] -> [%s] -> [%s]",
                outliersFilter, minimumCellsFilter, repetitiveValuesFilter, lowVarianceFilter );
    }
}
