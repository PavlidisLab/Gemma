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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.Map;

/**
 * Default filter used for various analyses of expression experiments.
 * <p>
 * For DEA and mean-variance analyses, a much less stringent filter, {@link RepetitiveValuesFilter}, is used.
 *
 * @author Paul
 */
public class ExpressionExperimentFilter implements Filter<ExpressionDataDoubleMatrix> {

    /**
     * Fewer rows than this, and we bail.
     */
    public static final int MINIMUM_ROWS_TO_BOTHER = 50;
    /**
     * How many samples a dataset has to have before we consider analyzing it.
     *
     * @see ExpressionExperimentFilter#MIN_NUMBER_OF_SAMPLES_PRESENT for a related setting.
     */
    public final static int MINIMUM_SAMPLE = 20;

    /**
     * Minimum number of samples for keeping rows when min-present filtering. Rows with more missing values
     * than this are always removed. This can be increased by the use of the min fraction present filter which sets a
     * fraction.
     */
    static final int MIN_NUMBER_OF_SAMPLES_PRESENT = 7;

    private static final Log log = LogFactory.getLog( ExpressionExperimentFilter.class.getName() );
    private final FilterConfig config;
    private final Collection<ArrayDesign> arrayDesignsUsed;

    /**
     * @param config           configuration used for all filtering. This must be defined at construction and cannot be changed
     *                         afterwards.
     * @param arrayDesignsUsed collection of ADs used
     */
    public ExpressionExperimentFilter( Collection<ArrayDesign> arrayDesignsUsed, FilterConfig config ) {
        this.arrayDesignsUsed = arrayDesignsUsed;
        this.config = config;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix matrix ) throws InsufficientDesignElementsException, InsufficientSamplesException {
        return filter( matrix, new FilterResult() );
    }

    /**
     * @param eeDoubleMatrix , already masked for missing values.
     * @return filtered matrix
     */
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix eeDoubleMatrix, FilterResult result ) throws InsufficientSamplesException, InsufficientDesignElementsException {
        if ( eeDoubleMatrix.rows() == 0 )
            throw new NoDesignElementsException( "No data found!" );

        if ( !config.isIgnoreMinimumSampleThreshold() ) {
            if ( eeDoubleMatrix.columns() < MINIMUM_SAMPLE ) {
                throw new InsufficientSamplesException(
                        String.format( "Not enough samples, must have at least %d to be eligible for link analysis.", MINIMUM_SAMPLE ) );
            } else if ( !config.isIgnoreMinimumRowsThreshold()
                    && eeDoubleMatrix.rows() < MINIMUM_ROWS_TO_BOTHER ) {
                throw new InsufficientDesignElementsException(
                        String.format( "To few rows in (%d) prior to filtering, data sets are not analyzed unless they have at least %d to be eligible for analysis.",
                                eeDoubleMatrix.rows(), MINIMUM_ROWS_TO_BOTHER ) );
            }
        }

        eeDoubleMatrix = this.doFilter( eeDoubleMatrix, result );

        if ( eeDoubleMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( "No rows left after filtering." );
        } else if ( !config.isIgnoreMinimumRowsThreshold()
                && eeDoubleMatrix.rows() < MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientDesignElementsException(
                    String.format( "To few rows (%d) after filtering, data sets are not analyzed unless they have at least %d rows.",
                            eeDoubleMatrix.rows(), MINIMUM_ROWS_TO_BOTHER ) );
        } else if ( !config.isIgnoreMinimumSampleThreshold()
                && eeDoubleMatrix.columns() < MINIMUM_SAMPLE ) {
            throw new InsufficientSamplesException(
                    String.format( "Not enough samples, must have at least %d to be eligible for link analysis.",
                            MINIMUM_SAMPLE ) );
        }

        return eeDoubleMatrix;
    }

    /**
     * Apply filters as configured by the command line parameters and technology type. See getFilteredMatrix for the
     * details of what filters are applied and the ordering.
     *
     * @param eeDoubleMatrix , already masked for missing values.
     * @return A data matrix in which filters have been applied and missing values (in the PRESENTABSENT quantitation
     * type, if present) are masked
     */
    private ExpressionDataDoubleMatrix doFilter( ExpressionDataDoubleMatrix eeDoubleMatrix, FilterResult result ) throws NoDesignElementsException {

        ExpressionDataDoubleMatrix filteredMatrix = eeDoubleMatrix;

        int startingRows = eeDoubleMatrix.rows();
        result.setStartingRows( startingRows );

        if ( config.isRequireSequences() ) {
            filteredMatrix = this.filterNoSequences( eeDoubleMatrix );
            if ( filteredMatrix.rows() == 0 ) {
                // This can happen if the array design is not populated. To avoid problems with useless failures, just
                // skip this step.

                ExpressionExperimentFilter.log
                        .warn( "There were no sequences for the platform(s), but allowing filtering to go forward anyway despite config settings." );
                filteredMatrix = eeDoubleMatrix;
                // throw new IllegalStateException( "No rows left after removing elements without sequences" );
            }
        }

        int afterSequenceRemovalRows = filteredMatrix.rows();

        int afterAffyControlsFilter = afterSequenceRemovalRows;
        int afterMinPresentFilter = afterSequenceRemovalRows;
        int afterLowVarianceCut = afterSequenceRemovalRows;
        int afterLowExpressionCut = afterSequenceRemovalRows;
        int afterZeroVarianceCut;

        if ( this.usesAffymetrix() ) {
            ExpressionExperimentFilter.log.debug( "Filtering Affymetrix controls" );
            filteredMatrix = this.filterAffyControlProbes( filteredMatrix );
            afterAffyControlsFilter = filteredMatrix.rows();
        }
        result.setAfterInitialFilter( afterAffyControlsFilter );

        if ( !config.isIgnoreMinimumSampleThreshold() ) {
            ExpressionExperimentFilter.log.debug( "Filtering for missing data" );
            filteredMatrix = this.filterMissingValues( filteredMatrix );
            afterMinPresentFilter = filteredMatrix.rows();
            result.setAfterMinPresentFilter( afterMinPresentFilter );

            if ( filteredMatrix.rows() == 0 ) {
                throw new NoDesignElementsException( "No rows left after minimum non-missing data filtering" );
            }
        }

        /*
         * Always remove rows that have a variance of zero.
         */
        ExpressionExperimentFilter.log.debug( "Filtering rows with zero variance" );
        filteredMatrix = new ZeroVarianceFilter().filter( filteredMatrix );
        afterZeroVarianceCut = filteredMatrix.rows();
        result.setAfterZeroVarianceCut( afterZeroVarianceCut );
        if ( filteredMatrix.rows() == 0 ) {
            throw new NoDesignElementsException( "No rows left after filtering rows with zero variance" );
        }

        /*
         * Filtering lowly expressed genes.
         */
        if ( config.getLowExpressionCut() > 0.0 ) {
            ExpressionExperimentFilter.log.debug( "Filtering for low or too high expression" );
            Map<CompositeSequence, Double> ranks = eeDoubleMatrix.getRanks();
            filteredMatrix = this.filterLowExpression( filteredMatrix, ranks );
            afterLowExpressionCut = filteredMatrix.rows();
            result.setAfterLowExpressionCut( afterLowExpressionCut );

            if ( filteredMatrix.rows() == 0 ) {
                throw new NoDesignElementsException( "No rows left after expression level filtering" );
            }
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
            filteredMatrix = this.filterLowVariance( filteredMatrix );
            afterLowVarianceCut = filteredMatrix.rows();
            result.setAfterLowVarianceCut( afterLowVarianceCut );

            if ( filteredMatrix.rows() == 0 ) {
                throw new NoDesignElementsException( "No rows left after variance filtering" );
            }
        }

        if ( ExpressionExperimentFilter.log.isInfoEnabled() ) {
            StringBuilder buf = new StringBuilder();

            buf.append( "Filter summary:\n" );
            buf.append( "Filter summary for " ).append( eeDoubleMatrix.getExpressionExperiment() ).append( ":\n" );
            buf.append( "Started with\t" ).append( startingRows ).append( " (" ).append( eeDoubleMatrix.columns() )
                    .append( " columns) " ).append( "\n" );
            if ( config.isRequireSequences() )
                buf.append( "After Seq\t" ).append( afterSequenceRemovalRows ).append( "\n" );
            if ( this.usesAffymetrix() )
                buf.append( "After removing Affy controls\t" ).append( afterAffyControlsFilter ).append( "\n" );
            if ( !config.isIgnoreMinimumSampleThreshold() )
                buf.append( "After MinPresent\t" ).append( afterMinPresentFilter ).append( "\n" );
            buf.append( "After ZeroVar\t" ).append( afterZeroVarianceCut ).append( "\n" );
            if ( config.getLowExpressionCut() > 0.0 )
                buf.append( "After LowExpr\t" ).append( afterLowExpressionCut ).append( "\n" );
            if ( config.getLowVarianceCut() > 0.0 )
                buf.append( "After LowVar\t" ).append( afterLowVarianceCut ).append( "\n" );
            buf.append( "================================================================\n" );
            ExpressionExperimentFilter.log.info( buf.toString() );
        }

        return filteredMatrix;
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

        /*
         * Always require at least 5 samples.
         */
        rowMissingFilter.setMinPresentCount( ExpressionExperimentFilter.MIN_NUMBER_OF_SAMPLES_PRESENT );

        return rowMissingFilter.filter( matrix );
    }

    /**
     * Remove rows that lack a {@link ubic.gemma.model.genome.biosequence.BioSequence} associated to their design
     * element.
     */
    private ExpressionDataDoubleMatrix filterNoSequences( ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        return new RowsWithSequencesFilter().filter( eeDoubleMatrix );
    }

    private boolean usesAffymetrix() {
        for ( ArrayDesign arrayDesign : arrayDesignsUsed ) {
            if ( StringUtils.isNotBlank( arrayDesign.getName() ) && arrayDesign.getName().toUpperCase()
                    .contains( "AFFYMETRIX" ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ExpressionExperimentFilter Platforms= " + arrayDesignsUsed + "\n" + config;
    }
}
