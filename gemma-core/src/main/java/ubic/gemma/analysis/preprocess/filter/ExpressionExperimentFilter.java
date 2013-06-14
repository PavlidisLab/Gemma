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
package ubic.gemma.analysis.preprocess.filter;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.Constants;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.analysis.preprocess.InsufficientProbesException;
import ubic.gemma.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.analysis.preprocess.filter.RowLevelFilter.Method;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Methods to handle filtering expression experiments for analysis.
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionExperimentFilter {

    /**
     * Minimum number of samples for keeping rows when min-present filtering. Note that this should be set to be the
     * same as {@link ubic.gemma.analysis.preprocess.filter.FilterConfig.MINIMUM_SAMPLE} . Rows with more missing values
     * than this are always removed. This can be increased by the use of the min fractio npresent filter which sets a
     * fraction.
     * 
     * @see ubic.gemma.analysis.preprocess.filter.FilterConfig.MINIMUM_SAMPLE
     */
    public static final int MIN_NUMBER_OF_SAMPLES_PRESENT = 7;

    private static Log log = LogFactory.getLog( ExpressionExperimentFilter.class.getName() );

    /**
     * @param matrix
     * @return
     */
    public static ExpressionDataDoubleMatrix doNothingFilter( ExpressionDataDoubleMatrix matrix ) {
        return new ExpressionDataDoubleMatrix( matrix, matrix.getRowNames() );
    }

    /**
     * Remove rows that have a low variance, below the stated quantile
     * 
     * @param matrix
     * @param quantile e.g. 10 to remove 10% lowest variance rows.
     * @return
     */
    public static ExpressionDataDoubleMatrix lowVarianceFilter( ExpressionDataDoubleMatrix matrix, int quantile ) {
        if ( quantile <= 0 || quantile >= 100 ) {
            throw new IllegalArgumentException( "quantile must be between 1 and 100" );
        }
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setMethod( Method.VAR );
        rowLevelFilter.setLowCut( quantile / 100.0 );
        rowLevelFilter.setRemoveAllNegative( false );
        rowLevelFilter.setUseAsFraction( true );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * Remove rows that have a low diversity of values (equality judged based on tolerancee set in RowLevelFilter). This
     * happens when people "set values less than 10 equal to 10" for example. This effectively filters rows that have
     * too many missing values, because missing values are counted as a single value.
     * 
     * @param matrix
     * @param threshold fraction of values that must be distinct. Thus if set to 0.5, a vector of 10 values must have at
     *        least 5 distinct values.
     * @return
     */
    public static ExpressionDataDoubleMatrix tooFewDistinctValues( ExpressionDataDoubleMatrix matrix, double threshold ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setMethod( Method.DISTINCTVALUES );
        rowLevelFilter.setTolerance( Constants.SMALLISH );
        rowLevelFilter.setRemoveAllNegative( false );

        /*
         * 0.5 means: 1/2 of the values must be distinct. Close to zero means none of the values are distinct. 1.0 means
         * they are all distinct.
         */
        rowLevelFilter.setLowCut( threshold );
        rowLevelFilter.setUseAsFraction( false );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * Remove rows that have a variance of zero (within a small constant)
     * 
     * @param matrix
     * @return
     * @see Constants.SMALLISH
     */
    public static ExpressionDataDoubleMatrix zeroVarianceFilter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setMethod( Method.VAR );
        rowLevelFilter.setLowCut( Constants.SMALLISH );
        rowLevelFilter.setRemoveAllNegative( false );
        rowLevelFilter.setUseAsFraction( false );
        return rowLevelFilter.filter( matrix );
    }

    Collection<ArrayDesign> arrayDesignsUsed;

    private final FilterConfig config;

    /**
     * @param arrayDesignsUsed
     * @param config configuration used for all filtering. This must be defined at construction and cannot be changed
     *        afterwards.
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
     * @param ee
     * @param dataVectors
     * @return
     */
    public ExpressionDataDoubleMatrix getFilteredMatrix( Collection<ProcessedExpressionDataVector> dataVectors ) {
        ExpressionDataDoubleMatrix eeDoubleMatrix = new ExpressionDataDoubleMatrix( dataVectors );
        transform( eeDoubleMatrix );
        return filter( eeDoubleMatrix );
    }

    /**
     * Remove probes that are on the array as hybridization or RNA quality controls (AFFX*)
     * 
     * @param ee
     * @param filteredMatrix
     * @param arrayDesign
     * @return
     */
    private ExpressionDataDoubleMatrix affyControlProbeFilter( ExpressionDataDoubleMatrix matrix ) {
        AffyProbeNameFilter affyProbeNameFilter = new AffyProbeNameFilter( new Pattern[] { Pattern.AFFX } );
        return affyProbeNameFilter.filter( matrix );
    }

    /**
     * Apply filters as configured by the command line parameters and technology type. See getFilteredMatrix for the
     * details of what filters are applied and the ordering.
     * 
     * @param dataMatrix
     * @param eeDoubleMatrix , already masked for missing values.
     * @param ee
     * @return A data matrix in which filters have been applied and missing values (in the PRESENTABSENT quantitation
     *         type, if present) are masked
     */
    private ExpressionDataDoubleMatrix doFilter( ExpressionDataDoubleMatrix eeDoubleMatrix ) {

        ExpressionDataDoubleMatrix filteredMatrix = eeDoubleMatrix;

        int startingRows = eeDoubleMatrix.rows();
        config.setStartingRows( startingRows );

        if ( config.isRequireSequences() ) {
            filteredMatrix = noSequencesFilter( eeDoubleMatrix );
            if ( filteredMatrix.rows() == 0 ) {
                throw new IllegalStateException( "No rows left after removing elements without sequences" );
            }
        }

        int afterSequenceRemovalRows = filteredMatrix.rows();

        // boolean twoColor = isTwoColor();

        int afterAffyControlsFilter = afterSequenceRemovalRows;
        int afterMinPresentFilter = afterSequenceRemovalRows;
        int afterLowVarianceCut = afterSequenceRemovalRows;
        int afterLowExpressionCut = afterSequenceRemovalRows;
        int afterZeroVarianceCut = afterSequenceRemovalRows;
        int afterDistinctValueCut = afterSequenceRemovalRows;

        if ( usesAffymetrix() ) {
            log.info( "Filtering Affymetrix controls" );
            filteredMatrix = affyControlProbeFilter( filteredMatrix );
            afterAffyControlsFilter = filteredMatrix.rows();
        }
        config.setAfterInitialFilter( afterAffyControlsFilter );

        if ( config.isMinPresentFractionIsSet() && !config.isIgnoreMinimumSampleThreshold() ) {
            log.info( "Filtering for missing data" );
            filteredMatrix = minPresentFilter( filteredMatrix );
            afterMinPresentFilter = filteredMatrix.rows();
            config.setAfterMinPresentFilter( afterMinPresentFilter );

            if ( filteredMatrix.rows() == 0 ) {
                throw new IllegalStateException( "No rows left after minimum non-missing data filtering" );
            }
        }

        /*
         * Always remove rows that have a variance of zero.
         */
        log.info( "Filtering rows with zero variance" );
        filteredMatrix = zeroVarianceFilter( filteredMatrix );
        afterZeroVarianceCut = filteredMatrix.rows();
        config.setAfterZeroVarianceCut( afterZeroVarianceCut );
        if ( filteredMatrix.rows() == 0 ) {
            throw new IllegalStateException( "No rows left after filtering rows with zero variance" );
        }

        if ( config.isDistinctValueThresholdSet() ) {
            log.info( "Filtering rows that have too few distincti values" );
            filteredMatrix = lowDistinctValueFilter( filteredMatrix );
            afterDistinctValueCut = filteredMatrix.rows();
            config.setAfterDistinctValueCut( afterDistinctValueCut );

            if ( filteredMatrix.rows() == 0 ) {
                throw new IllegalStateException( "No rows left after distinct value filtering" );
            }
        }

        /*
         * Note that variance filtering is a little tricky. For ratiometric arrays, you clearly should use the variance.
         * For 'signal' arrays, we used the CV, but this has problems when the mean is near zero. We could use a
         * regularized CV, but it not really clear how to do this. If the data are on a log scale, and furthermore
         * variance-stabilized (RMA for example), this is less of an issue. The variance is probably the safest bet and
         * seems to be what others use. For example see Hackstadt and Hess, BMC Bioinformatics 2009 10:11. Iny any case,
         * Vaneet has not found variance filtering to be all that effective, at least for coexpression analysis.
         */
        if ( config.isLowVarianceCutIsSet() ) {
            log.info( "Filtering for low variance " );
            filteredMatrix = lowVarianceFilter( filteredMatrix );
            afterLowVarianceCut = filteredMatrix.rows();
            config.setAfterLowVarianceCut( afterLowVarianceCut );

            if ( filteredMatrix.rows() == 0 ) {
                throw new IllegalStateException( "No rows left after variance filtering" );
            }
        }

        if ( config.isLowExpressionCutIsSet() ) {
            log.info( "Filtering for low or too high expression" );
            Map<CompositeSequence, Double> ranks = eeDoubleMatrix.getRanks();
            filteredMatrix = lowExpressionFilter( filteredMatrix, ranks );
            afterLowExpressionCut = filteredMatrix.rows();
            config.setAfterLowExpressionCut( afterLowExpressionCut );

            if ( filteredMatrix.rows() == 0 ) {
                throw new IllegalStateException( "No rows left after expression level filtering" );
            }
        }

        if ( log.isInfoEnabled() ) {
            StringBuilder buf = new StringBuilder();

            buf.append( "================================================================\n" );
            buf.append( "Filter summary for " + eeDoubleMatrix.getExpressionExperiment() + ":\n" );
            buf.append( "Started with\t" + startingRows + "\n" );
            buf.append( "After Sequence filtering\t" + afterSequenceRemovalRows + "\n" );
            buf.append( "After removing Affy controls\t" + afterAffyControlsFilter + "\n" );
            buf.append( "After MinPresent\t" + afterMinPresentFilter + "\n" );
            buf.append( "AFter ZeroVar\t" + afterZeroVarianceCut + "\n" );
            buf.append( "After LowVar\t" + afterLowVarianceCut + "\n" );
            buf.append( "After LowExpr\t" + afterLowExpressionCut + "\n" );
            buf.append( "================================================================\n" );
            log.info( buf.toString() );
        }

        return filteredMatrix;
    }

    /**
     * @param eeDoubleMatrix , already masked for missing values.
     * @param ranks
     * @param arrayDesignsUsed
     * @return
     */
    private ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        if ( eeDoubleMatrix == null || eeDoubleMatrix.rows() == 0 )
            throw new IllegalArgumentException( "No data found!" );

        if ( !config.isIgnoreMinimumSampleThreshold() ) {
            if ( eeDoubleMatrix.columns() < FilterConfig.MINIMUM_SAMPLE ) {
                throw new InsufficientSamplesException( "Not enough samples, must have at least "
                        + FilterConfig.MINIMUM_SAMPLE + " to be eligble for link analysis." );
            } else if ( !config.isIgnoreMinimumRowsThreshold()
                    && eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
                throw new InsufficientProbesException( "To few rows in (" + eeDoubleMatrix.rows()
                        + ") prior to filtering, data sets are not analyzed unless they have at least "
                        + FilterConfig.MINIMUM_ROWS_TO_BOTHER + " to be eligble for analysis." );
            }
        }

        eeDoubleMatrix = this.doFilter( eeDoubleMatrix );

        if ( eeDoubleMatrix == null )
            throw new IllegalStateException( "Failed to get filtered data matrix, it was null" );

        if ( eeDoubleMatrix.rows() == 0 ) {
            log.info( "No rows left after filtering" );
            throw new InsufficientProbesException( "No rows left after filtering" );
        } else if ( !config.isIgnoreMinimumRowsThreshold()
                && eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( "To few rows in   (" + eeDoubleMatrix.rows()
                    + ") after filtering, data sets are not analyzed unless they have at least "
                    + FilterConfig.MINIMUM_ROWS_TO_BOTHER + " rows" );
        } else if ( !config.isIgnoreMinimumSampleThreshold() && eeDoubleMatrix.columns() < FilterConfig.MINIMUM_SAMPLE ) {
            throw new InsufficientSamplesException( "Not enough samples, must have at least "
                    + FilterConfig.MINIMUM_SAMPLE + " to be eligible for link analysis." );
        }

        return eeDoubleMatrix;
    }

    /**
     * @param eeDoubleMatrix
     * @return true if the data looks like it is already log transformed, false otherwise. This is based on the
     *         quantitation types and, as a check, looking at the data itself.
     */
    private boolean isLogTransformed( ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        Collection<QuantitationType> quantitationTypes = eeDoubleMatrix.getQuantitationTypes();
        for ( QuantitationType qt : quantitationTypes ) {
            ScaleType scale = qt.getScale();
            if ( scale.equals( ScaleType.LN ) || scale.equals( ScaleType.LOG10 ) || scale.equals( ScaleType.LOG2 )
                    || scale.equals( ScaleType.LOGBASEUNKNOWN ) ) {
                log.info( "Quantitationtype says the data is already log transformed" );
                return true;
            }
        }

        if ( this.isTwoColor() ) {
            log.info( "Data is from a two-color array, assuming it is log transformed" );
            return true;
        }

        for ( int i = 0; i < eeDoubleMatrix.rows(); i++ ) {
            for ( int j = 0; j < eeDoubleMatrix.columns(); j++ ) {
                double v = eeDoubleMatrix.get( i, j );
                if ( v > 20 ) {
                    log.info( "Data has large values, doesn't look log transformed" );
                    return false;
                }
            }
        }

        log.info( "Data looks log-transformed, but not sure...assuming it is" );
        return true;

    }

    /**
     * Determine if the expression experiment uses two-color arrays.
     * 
     * @param ee
     * @return
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

    /**
     * @param filteredMatrix
     * @return
     */
    private ExpressionDataDoubleMatrix lowDistinctValueFilter( ExpressionDataDoubleMatrix filteredMatrix ) {
        return tooFewDistinctValues( filteredMatrix, config.getLowDistinctValueCut() );
    }

    /**
     * @param matrix
     * @param ranks
     * @return
     */
    private ExpressionDataDoubleMatrix lowExpressionFilter( ExpressionDataDoubleMatrix matrix,
            Map<CompositeSequence, Double> ranks ) {
        // check for null ranks, in which case we can't use this.
        for ( Double d : ranks.values() ) {
            if ( d == null ) {
                log.info( "Ranks are null -- skipping expression level"
                        + " filtering (This is okay if ranks cannot be computed)" );
                return matrix;
            }
        }

        RowLevelFilter rowLevelFilter = new RowLevelFilter( ranks );
        rowLevelFilter.setLowCut( config.getLowExpressionCut() );
        rowLevelFilter.setHighCut( config.getHighExpressionCut() );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * @param matrix
     * @return
     */
    private ExpressionDataDoubleMatrix lowVarianceFilter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setMethod( Method.VAR );
        rowLevelFilter.setLowCut( config.getLowVarianceCut() );
        rowLevelFilter.setRemoveAllNegative( false );
        rowLevelFilter.setUseAsFraction( true );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * Remove rows that have too many missing values.
     * 
     * @param matrix with missing values masked already
     * @return filtered matrix
     */
    private ExpressionDataDoubleMatrix minPresentFilter( ExpressionDataDoubleMatrix matrix ) {
        log.info( "Filtering out genes that are missing too many values" );

        RowMissingValueFilter rowMissingFilter = new RowMissingValueFilter();
        rowMissingFilter.setMinPresentFraction( config.getMinPresentFraction() );

        /*
         * Always require at least 5 samples.
         */
        rowMissingFilter.setMinPresentCount( MIN_NUMBER_OF_SAMPLES_PRESENT );

        return rowMissingFilter.filter( matrix );
    }

    /**
     * Filter rows that lack BioSequences associated with the probes.
     * 
     * @param eeDoubleMatrix
     * @return
     */
    private ExpressionDataDoubleMatrix noSequencesFilter( ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        RowsWithSequencesFilter f = new RowsWithSequencesFilter();
        return f.filter( eeDoubleMatrix );
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
            if ( StringUtils.isNotBlank( arrayDesign.getName() )
                    && arrayDesign.getName().toUpperCase().contains( "AFFYMETRIX" ) ) {
                return true;
            }
        }
        return false;
    }

}
