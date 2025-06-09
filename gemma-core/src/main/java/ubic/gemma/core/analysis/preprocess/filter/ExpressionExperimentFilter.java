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
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.InsufficientProbesException;
import ubic.gemma.core.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.Map;

/**
 * Methods to handle filtering expression experiments for analysis.
 *
 * @author Paul
 */
public class ExpressionExperimentFilter implements Filter<ExpressionDataDoubleMatrix> {

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

    /**
     * Provides a ready-to-use expression data matrix that is transformed and filtered. The processes that are applied,
     * in this order:
     * <ol>
     * <li>Log transform, if requested and not already done
     * <li>Use the missing value data to mask the preferred data (ratiometric data only)
     * <li>Remove rows that don't have biosequences (always applied)
     * <li>Remove Affymetrix control probes (Affymetrix only)
     * <li>Remove rows that have too many missing values (as configured)
     * <li>Remove rows with low variance (ratiometric) or CV (one-color) (as configured)
     * <li>Remove rows with very high or low expression (as configured)
     * </ol>
     *
     * @param dataVectors data vectors
     * @return filtered matrix
     * @throws NoRowsLeftAfterFilteringException if filtering results in no row left in the expression matrix
     */
    public ExpressionDataDoubleMatrix transformAndFilter( Collection<ProcessedExpressionDataVector> dataVectors ) throws FilteringException {
        ExpressionDataDoubleMatrix eeDoubleMatrix = new ExpressionDataDoubleMatrix( dataVectors );
        this.transform( eeDoubleMatrix );
        return this.filter( eeDoubleMatrix );
    }

    /**
     * @param eeDoubleMatrix , already masked for missing values.
     * @return filtered matrix
     */
    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix eeDoubleMatrix ) throws NoRowsLeftAfterFilteringException, InsufficientSamplesException, InsufficientProbesException {
        if ( eeDoubleMatrix.rows() == 0 )
            throw new IllegalArgumentException( "No data found!" );

        if ( !config.isIgnoreMinimumSampleThreshold() ) {
            if ( eeDoubleMatrix.columns() < FilterConfig.MINIMUM_SAMPLE ) {
                throw new InsufficientSamplesException(
                        String.format( "Not enough samples, must have at least %d to be eligible for link analysis.", FilterConfig.MINIMUM_SAMPLE ) );
            } else if ( !config.isIgnoreMinimumRowsThreshold()
                    && eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
                throw new InsufficientProbesException( eeDoubleMatrix.getExpressionExperiment(),
                        String.format( "To few rows in (%d) prior to filtering, data sets are not analyzed unless they have at least %d to be eligible for analysis.",
                                eeDoubleMatrix.rows(), FilterConfig.MINIMUM_ROWS_TO_BOTHER ) );
            }
        }

        eeDoubleMatrix = this.doFilter( eeDoubleMatrix );

        if ( eeDoubleMatrix == null )
            throw new IllegalStateException( "Failed to get filtered data matrix, it was null" );

        if ( eeDoubleMatrix.rows() == 0 ) {
            ExpressionExperimentFilter.log.info( "No rows left after filtering" );
            throw new InsufficientProbesException( eeDoubleMatrix.getExpressionExperiment(), "No rows left after filtering." );
        } else if ( !config.isIgnoreMinimumRowsThreshold()
                && eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( eeDoubleMatrix.getExpressionExperiment(),
                    String.format( "To few rows (%d) after filtering, data sets are not analyzed unless they have at least %d rows.",
                            eeDoubleMatrix.rows(), FilterConfig.MINIMUM_ROWS_TO_BOTHER ) );
        } else if ( !config.isIgnoreMinimumSampleThreshold()
                && eeDoubleMatrix.columns() < FilterConfig.MINIMUM_SAMPLE ) {
            throw new InsufficientSamplesException(
                    String.format( "Not enough samples, must have at least %d to be eligible for link analysis.",
                            FilterConfig.MINIMUM_SAMPLE ) );
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
    private ExpressionDataDoubleMatrix doFilter( ExpressionDataDoubleMatrix eeDoubleMatrix ) throws NoRowsLeftAfterFilteringException {

        ExpressionDataDoubleMatrix filteredMatrix = eeDoubleMatrix;

        int startingRows = eeDoubleMatrix.rows();
        config.setStartingRows( startingRows );

        if ( config.isRequireSequences() ) {
            filteredMatrix = this.noSequencesFilter( eeDoubleMatrix );
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
            filteredMatrix = this.affyControlProbeFilter( filteredMatrix );
            afterAffyControlsFilter = filteredMatrix.rows();
        }
        config.setAfterInitialFilter( afterAffyControlsFilter );

        if ( config.isMinPresentFractionIsSet() && !config.isIgnoreMinimumSampleThreshold() ) {
            ExpressionExperimentFilter.log.debug( "Filtering for missing data" );
            filteredMatrix = this.minPresentFilter( filteredMatrix );
            afterMinPresentFilter = filteredMatrix.rows();
            config.setAfterMinPresentFilter( afterMinPresentFilter );

            if ( filteredMatrix.rows() == 0 ) {
                throw new NoRowsLeftAfterFilteringException( "No rows left after minimum non-missing data filtering" );
            }
        }

        /*
         * Always remove rows that have a variance of zero.
         */
        ExpressionExperimentFilter.log.debug( "Filtering rows with zero variance" );
        filteredMatrix = new ZeroVarianceFilter().filter( filteredMatrix );
        afterZeroVarianceCut = filteredMatrix.rows();
        config.setAfterZeroVarianceCut( afterZeroVarianceCut );
        if ( filteredMatrix.rows() == 0 ) {
            throw new NoRowsLeftAfterFilteringException( "No rows left after filtering rows with zero variance" );
        }

        /*
         * Filtering lowly expressed genes.
         */
        if ( config.isLowExpressionCutIsSet() ) {
            ExpressionExperimentFilter.log.debug( "Filtering for low or too high expression" );
            Map<CompositeSequence, Double> ranks = eeDoubleMatrix.getRanks();
            filteredMatrix = this.lowExpressionFilter( filteredMatrix, ranks );
            afterLowExpressionCut = filteredMatrix.rows();
            config.setAfterLowExpressionCut( afterLowExpressionCut );

            if ( filteredMatrix.rows() == 0 ) {
                throw new NoRowsLeftAfterFilteringException( "No rows left after expression level filtering" );
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
        if ( config.isLowVarianceCutIsSet() ) {
            ExpressionExperimentFilter.log.debug( "Filtering for low variance " );
            filteredMatrix = this.lowVarianceFilter( filteredMatrix );
            afterLowVarianceCut = filteredMatrix.rows();
            config.setAfterLowVarianceCut( afterLowVarianceCut );

            if ( filteredMatrix.rows() == 0 ) {
                throw new NoRowsLeftAfterFilteringException( "No rows left after variance filtering" );
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
            if ( config.isMinPresentFractionIsSet() && !config.isIgnoreMinimumSampleThreshold() )
                buf.append( "After MinPresent\t" ).append( afterMinPresentFilter ).append( "\n" );
            buf.append( "After ZeroVar\t" ).append( afterZeroVarianceCut ).append( "\n" );
            if ( config.isLowExpressionCutIsSet() )
                buf.append( "After LowExpr\t" ).append( afterLowExpressionCut ).append( "\n" );
            if ( config.isLowVarianceCutIsSet() )
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
    private ExpressionDataDoubleMatrix affyControlProbeFilter( ExpressionDataDoubleMatrix matrix ) {
        return new AffyProbeNameFilter( new Pattern[] { Pattern.AFFX } ).filter( matrix );
    }

    /**
     * @param eeDoubleMatrix the matrix
     * @return true if the data looks like it is already log transformed, false otherwise. This is based on the
     * quantitation types and, as a check, looking at the data itself.
     */
    private boolean isLogTransformed( ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        Collection<QuantitationType> quantitationTypes = eeDoubleMatrix.getQuantitationTypes();
        for ( QuantitationType qt : quantitationTypes ) {
            ScaleType scale = qt.getScale();
            if ( scale.equals( ScaleType.LN ) || scale.equals( ScaleType.LOG10 ) || scale.equals( ScaleType.LOG2 )
                    || scale.equals( ScaleType.LOGBASEUNKNOWN ) ) {
                ExpressionExperimentFilter.log.info( "Quantitationtype says the data is already log transformed" );
                return true;
            }
        }

        if ( this.isTwoColor() ) {
            ExpressionExperimentFilter.log.info( "Data is from a two-color array, assuming it is log transformed" );
            return true;
        }

        for ( int i = 0; i < eeDoubleMatrix.rows(); i++ ) {
            for ( int j = 0; j < eeDoubleMatrix.columns(); j++ ) {
                double v = eeDoubleMatrix.getAsDouble( i, j );
                if ( v > 20 ) {
                    ExpressionExperimentFilter.log.info( "Data has large values, doesn't look log transformed" );
                    return false;
                }
            }
        }

        ExpressionExperimentFilter.log.info( "Data looks log-transformed, but not sure...assuming it is" );
        return true;

    }

    /**
     * Determine if the expression experiment uses two-color arrays.
     *
     * @throws UnsupportedOperationException if the ee uses both two color and one-color technologies.
     */
    private Boolean isTwoColor() {
        Boolean answer = null;

        if ( arrayDesignsUsed.isEmpty() ) {
            throw new IllegalStateException();
        }

        for ( ArrayDesign arrayDesign : arrayDesignsUsed ) {
            TechnologyType techType = arrayDesign.getTechnologyType();
            boolean isTwoC = techType.equals( TechnologyType.TWOCOLOR ) || techType.equals( TechnologyType.DUALMODE );
            if ( answer != null && !answer.equals( isTwoC ) ) {
                throw new UnsupportedOperationException(
                        "Gemma cannot handle experiments that mix one- and two-color arrays" );
            }
            answer = isTwoC;
        }
        return answer;
    }

    private ExpressionDataDoubleMatrix lowExpressionFilter( ExpressionDataDoubleMatrix matrix,
            Map<CompositeSequence, Double> ranks ) {
        // check for null ranks, in which case we can't use this.
        for ( Double d : ranks.values() ) {
            if ( d == null ) {
                ExpressionExperimentFilter.log.info( "Ranks are null -- skipping expression level"
                        + " filtering (This is okay if ranks cannot be computed)" );
                return matrix;
            }
        }

        RowLevelFilter rowLevelFilter = new RowLevelFilter( ranks );
        rowLevelFilter.setLowCut( config.getLowExpressionCut() );
        rowLevelFilter.setHighCut( config.getHighExpressionCut() );
        return rowLevelFilter.filter( matrix );
    }

    private ExpressionDataDoubleMatrix lowVarianceFilter( ExpressionDataDoubleMatrix matrix ) {
        return new LowVarianceFilter( config.getLowVarianceCut() ).filter( matrix );
    }

    /**
     * Remove rows that have too many missing values.
     *
     * @param matrix with missing values masked already
     * @return filtered matrix
     */
    private ExpressionDataDoubleMatrix minPresentFilter( ExpressionDataDoubleMatrix matrix ) {
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
     * Filter rows that lack BioSequences associated with the probes.
     */
    private ExpressionDataDoubleMatrix noSequencesFilter( ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        return new RowsWithSequencesFilter().filter( eeDoubleMatrix );
    }

    /**
     * Transform the data as configured (e.g., take log) -- which could mean no action
     */
    private void transform( ExpressionDataDoubleMatrix datamatrix ) {

        if ( !config.isLogTransform() ) {
            return;
        }

        boolean alreadyLogged = this.isLogTransformed( datamatrix );
        if ( !alreadyLogged ) {
            MatrixStats.logTransform( datamatrix.getMatrix() );
        }

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
