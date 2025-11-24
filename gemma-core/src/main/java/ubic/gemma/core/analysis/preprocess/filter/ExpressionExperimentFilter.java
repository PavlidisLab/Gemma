/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.analysis.preprocess.filter;

import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.TextStringBuilder;
import ubic.gemma.core.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default filter used for various analyses of expression experiments.
 * <p>
 * For DEA and mean-variance analyses, a much less stringent filter, {@link RepetitiveValuesFilter}, is used.
 *
 * @author Paul
 */
public class ExpressionExperimentFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    /**
     * How many design elements a dataset has to have before we consider analyzing it.
     * <p>
     * If at any point during filtering, the number of design elements drop below this number, an
     * {@link InsufficientDesignElementsException} exception will be raised. This can be disabled by setting
     * {@link ExpressionExperimentFilterConfig#setIgnoreMinimumDesignElementsThreshold(boolean)}.
     */
    public static final int MINIMUM_DESIGN_ELEMENTS = 50;
    /**
     * How many samples a dataset has to have before we consider analyzing it.
     * <p>
     * If at any point during filtering, the number of samples drop below this number, an
     * {@link InsufficientSamplesException} will be raised. This can be disabled by setting
     * {@link ExpressionExperimentFilterConfig#setIgnoreMinimumSamplesThreshold(boolean)}.
     */
    public final static int MINIMUM_SAMPLES = 20;

    private static final Log log = LogFactory.getLog( ExpressionExperimentFilter.class.getName() );
    private final ExpressionExperimentFilterConfig config;

    /**
     * @param config configuration used for all filtering. This must be defined at construction and cannot be changed
     *               afterwards.
     */
    public ExpressionExperimentFilter( ExpressionExperimentFilterConfig config ) {
        this.config = config;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix matrix ) throws FilteringException {
        return filter( matrix, getArrayDesignUsed( matrix ), new ExpressionExperimentFilterResult() );
    }

    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix, ExpressionExperimentFilterResult result ) throws FilteringException {
        return filter( dataMatrix, getArrayDesignUsed( dataMatrix ), result );
    }

    /**
     * @param dataMatrix       already masked for missing values.
     * @param arrayDesignsUsed platform used by the experiment, this is used to determine which platform-specific
     *                         filters should be applied, if an empty collection is supplied, only generic filters will
     *                         be applied.
     * @return filtered matrix
     */
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix, Collection<ArrayDesign> arrayDesignsUsed, ExpressionExperimentFilterResult result ) throws FilteringException {
        result.setStartingRows( dataMatrix.rows() );
        result.setStartingColumns( ExpressionDataFilterUtils.countSamplesWithData( dataMatrix ) );

        checkEnoughDesignElementsAndSamples( dataMatrix, null );

        if ( config.isRequireSequences() ) {
            try {
                dataMatrix = this.filterNoSequences( dataMatrix );
                result.setNoSequencesFilterApplied( true );
                result.setAfterNoSequencesFilter( dataMatrix.rows() );
                checkEnoughDesignElementsAndSamples( dataMatrix, "after filtering design elements without biosequences" );
            } catch ( NoDesignElementsException e ) {
                // This can happen if the array design is not populated. To avoid problems with useless failures, just
                // skip this step.
                result.setNoSequencesFilterApplied( false );
                result.setAfterNoSequencesFilter( dataMatrix.rows() );
            }
        } else {
            result.setNoSequencesFilterApplied( false );
            result.setAfterNoSequencesFilter( dataMatrix.rows() );
        }

        if ( this.usesAffymetrix( arrayDesignsUsed ) ) {
            ExpressionExperimentFilter.log.debug( "Filtering Affymetrix controls" );
            dataMatrix = this.filterAffyControlProbes( dataMatrix );
            result.setAffyControlsFilterApplied( true );
            result.setAfterAffyControlsFilter( dataMatrix.rows() );
            checkEnoughDesignElementsAndSamples( dataMatrix, "filtering Affymetrix controls" );
        } else {
            log.debug( "Skipping Affymetrix control probe filtering; no Affymetrix platforms detected." );
            result.setAffyControlsFilterApplied( false );
            result.setAfterAffyControlsFilter( dataMatrix.rows() );
        }

        if ( config.isMaskOutliers() ) {
            dataMatrix = filterOutliers( dataMatrix );
            result.setOutliersFilterApplied( true );
            result.setAfterOutliersFilter( dataMatrix.rows() );
            result.setColumnsAfterOutliersFilter( ExpressionDataFilterUtils.countSamplesWithData( dataMatrix ) );
            checkEnoughDesignElementsAndSamples( dataMatrix, "masking outliers" );
        } else {
            result.setOutliersFilterApplied( false );
            result.setAfterOutliersFilter( dataMatrix.rows() );
            result.setColumnsAfterOutliersFilter( ExpressionDataFilterUtils.countSamplesWithData( dataMatrix ) );
        }

        if ( !config.isIgnoreMinimumSamplesThreshold() ) {
            ExpressionExperimentFilter.log.debug( "Filtering for missing data" );
            dataMatrix = this.filterMissingValues( dataMatrix );
            result.setMinPresentFilterApplied( true );
            result.setAfterMinPresentFilter( dataMatrix.rows() );
            checkEnoughDesignElementsAndSamples( dataMatrix, "filtering for missing data" );
        } else {
            result.setMinPresentFilterApplied( false );
            result.setAfterMinPresentFilter( dataMatrix.rows() );
        }

        // Always remove rows that have a variance of zero.
        ExpressionExperimentFilter.log.debug( "Filtering rows with zero variance" );
        dataMatrix = new ZeroVarianceFilter().filter( dataMatrix );
        result.setZeroVarianceFilterApplied( true );
        result.setAfterZeroVarianceFilter( dataMatrix.rows() );
        checkEnoughDesignElementsAndSamples( dataMatrix, "filtering design elements with zero variance" );

        // Filtering lowly expressed genes.
        if ( config.getLowExpressionCut() > 0.0 ) {
            ExpressionExperimentFilter.log.debug( "Filtering for low or too high expression" );
            Map<CompositeSequence, Double> ranks = dataMatrix.getRanks();
            dataMatrix = this.filterLowExpression( dataMatrix, ranks );
            result.setLowExpressionFilterApplied( true );
            result.setAfterLowExpressionFilter( dataMatrix.rows() );
            checkEnoughDesignElementsAndSamples( dataMatrix, "filtering design elements with low expression levels" );
        } else {
            result.setLowExpressionFilterApplied( false );
            result.setAfterLowExpressionFilter( dataMatrix.rows() );
        }

        /*
         *
         * Variance filtering is a little tricky. For ratiometric arrays, you clearly should use the variance. For
         * 'signal' arrays, we tried using the CV, but this has problems when the mean is near zero (filtering by low
         * expression first helps). If the data are on a log scale, and furthermore variance-stabilized (RMA for
         * example), this is less of an issue.
         *
         * The variance is probably the safest bet and seems to be what others use. For example see Hackstadt and Hess,
         * BMC Bioinformatics 2009 10:11 (http://www.ncbi.nlm.nih.gov/pubmed/19133141)
         */
        if ( config.getLowVarianceCut() > 0.0 ) {
            ExpressionExperimentFilter.log.debug( "Filtering for low variance " );
            dataMatrix = this.filterLowVariance( dataMatrix );
            result.setLowVarianceFilterApplied( true );
            result.setAfterLowVarianceFilter( dataMatrix.rows() );
            checkEnoughDesignElementsAndSamples( dataMatrix, "filtering design elements with low variance" );
        } else {
            result.setLowVarianceFilterApplied( false );
            result.setAfterLowVarianceFilter( dataMatrix.rows() );
        }

        result.setFinalRows( dataMatrix.rows() );
        result.setFinalColumns( ExpressionDataFilterUtils.countSamplesWithData( dataMatrix ) );

        String buf = String.format( "Filter summary for %s of %s:\n%s", dataMatrix.getQuantitationType(),
                dataMatrix.getExpressionExperiment(), describeFilterResult( result ) );
        ExpressionExperimentFilter.log.info( buf );

        return dataMatrix;
    }

    public String describeFilterResult( ExpressionExperimentFilterResult result ) {
        int[] widths = { 18, 15, 7 };
        TextStringBuilder buf = new TextStringBuilder();
        buf.append( "===========================================\n" );
        buf.appendFixedWidthPadRight( "Filter Applied", widths[0], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( "Design Elements", widths[1], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( "Samples", widths[2], ' ' )
                .append( "\n" );
        buf.appendFixedWidthPadRight( "Started with:", widths[0], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( result.getStartingRows(), widths[1], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( result.getStartingColumns(), widths[2], ' ' )
                .append( "\n" );
        if ( result.isNoSequencesFilterApplied() )
            buf.appendFixedWidthPadRight( "After NoSeq:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getAfterNoSequencesFilter(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getStartingColumns(), widths[2], ' ' )
                    .append( "\n" );
        if ( result.isAffyControlsFilterApplied() )
            buf.appendFixedWidthPadRight( "After AffyControls:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getAfterAffyControlsFilter(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getStartingColumns(), widths[2], ' ' )
                    .append( "\n" );
        if ( result.isOutliersFilterApplied() )
            buf.appendFixedWidthPadRight( "After MaskOutliers:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getAfterOutliersFilter(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getColumnsAfterOutliersFilter(), widths[2], ' ' )
                    .append( "\n" );
        if ( result.isMinPresentFilterApplied() )
            buf.appendFixedWidthPadRight( "After MinPresent:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getAfterMinPresentFilter(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getColumnsAfterOutliersFilter(), widths[2], ' ' )
                    .append( "\n" );
        if ( result.isZeroVarianceFilterApplied() )
            buf.appendFixedWidthPadRight( "After ZeroVar:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getAfterZeroVarianceFilter(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getColumnsAfterOutliersFilter(), widths[2], ' ' )
                    .append( "\n" );
        if ( result.isLowExpressionFilterApplied() )
            buf.appendFixedWidthPadRight( "After LowExpr:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getAfterLowExpressionFilter(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getColumnsAfterOutliersFilter(), widths[2], ' ' )
                    .append( "\n" );
        if ( result.isLowVarianceFilterApplied() )
            buf.appendFixedWidthPadRight( "After LowVar:", widths[0], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getAfterLowVarianceFilter(), widths[1], ' ' )
                    .append( "\t" ).appendFixedWidthPadLeft( result.getColumnsAfterOutliersFilter(), widths[2], ' ' )
                    .append( "\n" );
        buf.appendFixedWidthPadRight( "Ended with:\t", widths[0], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( result.getFinalRows(), widths[1], ' ' )
                .append( "\t" ).appendFixedWidthPadLeft( result.getFinalColumns(), widths[2], ' ' )
                .append( "\n" );
        buf.append( "===========================================" );
        return buf.toString();
    }

    /**
     * Remove probes that are on the array as hybridization or RNA quality controls (AFFX*)
     *
     * @param matrix the matrix
     * @return filtered matrix
     */
    private ExpressionDataDoubleMatrix filterAffyControlProbes( ExpressionDataDoubleMatrix matrix ) {
        return new AffyProbeNameFilter( new Pattern[] { Pattern.AFFX } ).filter( matrix );
    }

    /**
     * Filter rows with low expression levels.
     */
    private ExpressionDataDoubleMatrix filterLowExpression( ExpressionDataDoubleMatrix matrix,
            Map<CompositeSequence, Double> ranks ) {
        // check for null ranks, in which case we can't use this.
        for ( Double d : ranks.values() ) {
            if ( d == null ) {
                ExpressionExperimentFilter.log.info( "Ranks are null -- skipping expression level"
                        + " filtering (This is okay if ranks cannot be computed)" );
                return matrix;
            }
        }

        RowLevelFilter rowLevelFilter = new RowLevelFilter( ranks::get );
        rowLevelFilter.setLowCut( config.getLowExpressionCut() );
        rowLevelFilter.setHighCut( config.getHighExpressionCut() );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * Filter rows with low variance.
     */
    private ExpressionDataDoubleMatrix filterLowVariance( ExpressionDataDoubleMatrix matrix ) {
        return new LowVarianceFilter( config.getLowVarianceCut() ).filter( matrix );
    }

    private ExpressionDataDoubleMatrix filterOutliers( ExpressionDataDoubleMatrix matrix ) {
        return new OutliersFilter().filter( matrix );
    }

    /**
     * Remove rows that have too many missing values.
     *
     * @param matrix with missing values masked already
     * @return filtered matrix
     */
    private ExpressionDataDoubleMatrix filterMissingValues( ExpressionDataDoubleMatrix matrix ) {
        ExpressionExperimentFilter.log.info( "Filtering out genes that are missing too many values" );
        RowMissingValueFilter rowMissingFilter = new RowMissingValueFilter();
        rowMissingFilter.setMinPresentFraction( config.getMinPresentFraction() );
        rowMissingFilter.setMinPresentCount( config.getMinPresentCount() );
        return rowMissingFilter.filter( matrix );
    }

    /**
     * Remove rows that lack a {@link ubic.gemma.model.genome.biosequence.BioSequence} associated to their design
     * element.
     */
    private ExpressionDataDoubleMatrix filterNoSequences( ExpressionDataDoubleMatrix matrix ) throws NoDesignElementsException {
        return new RowsWithSequencesFilter().filter( matrix );
    }

    private Collection<ArrayDesign> getArrayDesignUsed( ExpressionDataDoubleMatrix matrix ) {
        Set<ArrayDesign> result = new HashSet<>();
        for ( int j = 0; j < matrix.columns(); j++ ) {
            for ( BioAssay ba : matrix.getBioAssaysForColumn( j ) ) {
                result.add( ba.getArrayDesignUsed() );
            }
        }
        return result;
    }

    private boolean usesAffymetrix( Collection<ArrayDesign> arrayDesignsUsed ) {
        return arrayDesignsUsed.stream()
                .map( ArrayDesign::getName )
                .anyMatch( name -> Strings.CI.contains( name, "AFFYMETRIX" ) );
    }

    /**
     * Ensure that there are enough design elements and samples in the data matrix to keep going.
     */
    private void checkEnoughDesignElementsAndSamples( ExpressionDataDoubleMatrix dataMatrix, @Nullable String afterFilter ) throws InsufficientDesignElementsException, InsufficientSamplesException {
        int numberOfSamples = ExpressionDataFilterUtils.countSamplesWithData( dataMatrix );
        if ( dataMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( String.format( "No design elements%s.",
                    afterFilter != null ? " after " + afterFilter : "" ) );
        } else if ( !config.isIgnoreMinimumDesignElementsThreshold() && dataMatrix.rows() < MINIMUM_DESIGN_ELEMENTS ) {
            throw new InsufficientDesignElementsException( String.format( "To few design elements (%d)%s, must have at least %d.",
                    dataMatrix.rows(), afterFilter != null ? " left after " + afterFilter : "", MINIMUM_DESIGN_ELEMENTS ) );
        } else if ( numberOfSamples == 0 ) {
            throw new NoSamplesException( String.format( "No samples%s.", afterFilter != null ? " left after " + afterFilter : "" ) );
        } else if ( !config.isIgnoreMinimumSamplesThreshold() && numberOfSamples < MINIMUM_SAMPLES ) {
            throw new InsufficientSamplesException( String.format( "Not enough samples (%d)%s, must have at least %d.",
                    numberOfSamples, afterFilter != null ? " left after " + afterFilter : "", MINIMUM_SAMPLES ) );
        }
    }

    @Override
    public String toString() {
        return "ExpressionExperimentFilter Config=" + config;
    }
}
