/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.InsufficientProbesException;
import ubic.gemma.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.analysis.preprocess.filter.RowLevelFilter.Method;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;

/**
 * Methods to handle filtering expression experiments for analysis.
 * 
 * @author Paul
 * @version $Id$
 */
public class ExpressionExperimentFilter {

    /**
     * Minimum number of samples for keeping rows when min-present filtering. Note that this should be set to be the
     * same as {@link  ubic.gemma.analysis.preprocess.filter.FilterConfig.MINIMUM_SAMPLE}. Rows with more missing
     * values than this are always removed. This can be increased by the use of the min fractio npresent filter which
     * sets a fraction.
     * 
     * @see ubic.gemma.analysis.preprocess.filter.FilterConfig.MINIMUM_SAMPLE
     */
    public static final int MIN_NUMBER_OF_SAMPLES_PRESENT = 7;

    private static Log log = LogFactory.getLog( ExpressionExperimentFilter.class.getName() );

    /**
     * @param probesToGenesMap Map of "clusters"
     * @return
     */
    public static Collection<DesignElement> getProbesForKnownGenes(
            Map<CompositeSequence, Collection<Collection<Gene>>> probesToGenesMap ) {
        Collection<DesignElement> keepers = new HashSet<DesignElement>();
        for ( CompositeSequence cs : probesToGenesMap.keySet() ) {
            cluster: for ( Collection<Gene> cluster : probesToGenesMap.get( cs ) ) {
                for ( Gene g : cluster ) {
                    if ( g instanceof PredictedGene || g instanceof ProbeAlignedRegion ) {
                        continue;
                    }
                    keepers.add( cs );
                    break cluster;
                }
            }
        }
        return keepers;
    }

    Collection<ArrayDesign> arrayDesignsUsed;

    ExpressionExperiment ee;

    private final FilterConfig config;

    /**
     * @param ee
     * @param config configuration used for all filtering. This must be defined at construction and cannot be changed
     *        afterwards.
     */
    public ExpressionExperimentFilter( ExpressionExperiment ee, Collection<ArrayDesign> arrayDesignsUsed,
            FilterConfig config ) {
        this.ee = ee;
        this.arrayDesignsUsed = arrayDesignsUsed;
        this.config = config;
    }

    /**
     * Provides a ready-to-use expression data matrix. The filters that are applied, in this order:
     * <ol>
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
    public ExpressionDataDoubleMatrix getFilteredMatrix( Collection<DesignElementDataVector> dataVectors ) {
        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( dataVectors );
        ExpressionDataDoubleMatrix eeDoubleMatrix = builder.getPreferredData();

        eeDoubleMatrix = filter( builder, eeDoubleMatrix );
        return eeDoubleMatrix;
    }

    /**
     * Remove probes which to not map to at least one "known gene" (that is, not just probe-aligned regions or predicted
     * genes).
     * 
     * @param matrix
     * @param probesToGenesMap Map of probes to collection of gene 'clusters'. The gene information associated with each
     *        probe is used to determine if it is filtered; it's up to the caller to populate this properly. Probes that
     *        are not represented in the map will not be retained!
     * @return filtered matrix
     */
    public ExpressionDataDoubleMatrix knownGenesOnlyFilter( ExpressionDataDoubleMatrix matrix,
            Map<CompositeSequence, Collection<Collection<Gene>>> probesToGenesMap ) {
        Collection<DesignElement> keepers = getProbesForKnownGenes( probesToGenesMap );
        RowNameFilter rowNameFilter = new RowNameFilter( keepers );
        return rowNameFilter.filter( matrix );
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
     * @param eeDoubleMatrix
     * @param ee
     * @return A data matrix in which filters have been applied and missing values (in the PRESENTABSENT quantitation
     *         type, if present) are masked
     */
    private ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix eeDoubleMatrix,
            ExpressionDataMatrixBuilder builder ) {

        ExpressionDataDoubleMatrix filteredMatrix = eeDoubleMatrix;

        filteredMatrix = noSequencesFilter( eeDoubleMatrix );

        boolean twoColor = isTwoColor();

        if ( usesAffymetrix() ) {
            log.info( "Filtering Affymetrix controls" );
            filteredMatrix = affyControlProbeFilter( filteredMatrix );
        }

        if ( config.isMinPresentFractionIsSet() ) {
            log.info( "Filtering for missing data" );

            if ( twoColor ) {
                /* Apply two color missing value filter */
                builder.maskMissingValues( filteredMatrix, null );
            }
            filteredMatrix = minPresentFilter( filteredMatrix );
        }

        if ( config.isLowVarianceCutIsSet() ) {
            if ( twoColor ) {
                log.info( "Filtering for low variance (ratiometric)" );
                filteredMatrix = lowVarianceFilter( filteredMatrix );
            } else {
                log.info( "Filtering for low CV (signals)" );
                filteredMatrix = lowCVFilter( filteredMatrix );
            }
        }

        if ( config.isLowExpressionCutIsSet() ) {
            log.info( "Filtering for low or too high expression" );
            Map<DesignElement, Double> ranks = builder.getRanks();
            filteredMatrix = lowExpressionFilter( filteredMatrix, ranks );
        }

        return filteredMatrix;
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
     * @param ee
     * @param builder
     * @param eeDoubleMatrix
     * @param ranks
     * @param arrayDesignsUsed
     * @return
     */
    private ExpressionDataDoubleMatrix filter( ExpressionDataMatrixBuilder builder,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        if ( eeDoubleMatrix == null || eeDoubleMatrix.rows() == 0 )
            throw new IllegalArgumentException( "No data found!" );

        if ( eeDoubleMatrix.columns() < FilterConfig.MINIMUM_SAMPLE ) {
            throw new InsufficientSamplesException( "Not enough samples " + ee.getShortName() + ", must have at least "
                    + FilterConfig.MINIMUM_SAMPLE + " to be eligble for link analysis." );
        } else if ( eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( "To few rows in " + ee.getShortName() + " (" + eeDoubleMatrix.rows()
                    + ") prior to filtering, data sets are not analyzed unless they have at least "
                    + FilterConfig.MINIMUM_SAMPLE + " to be eligble for link analysis." );
        }

        eeDoubleMatrix = this.filter( eeDoubleMatrix, builder );

        if ( eeDoubleMatrix == null )
            throw new IllegalStateException( "Failed to get filtered data matrix, it was null " + ee.getShortName() );

        if ( eeDoubleMatrix.rows() == 0 ) {
            log.info( "No rows left after filtering" );
            throw new InsufficientProbesException( "No rows left after filtering" );
        } else if ( eeDoubleMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( "To few rows in " + ee.getShortName() + " (" + eeDoubleMatrix.rows()
                    + ") after filtering, data sets are not analyzed unless they have at least "
                    + FilterConfig.MINIMUM_ROWS_TO_BOTHER + " rows" );
        } else if ( eeDoubleMatrix.columns() < FilterConfig.MINIMUM_SAMPLE ) {
            throw new InsufficientSamplesException( "Not enough samples " + ee.getShortName() + ", must have at least "
                    + FilterConfig.MINIMUM_SAMPLE + " to be eligble for link analysis." );
        }

        return eeDoubleMatrix;
    }

    /**
     * Determine if the expression experiment uses two-color arrays.
     * 
     * @param ee
     * @return
     * @throws UnsupportedOperationException if the ee uses both two color and one-color technologies.
     */
    @SuppressWarnings("unchecked")
    private boolean isTwoColor() {
        Boolean answer = null;
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
     * @param matrix
     * @return filtered matrix
     */
    private ExpressionDataDoubleMatrix lowCVFilter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setMethod( Method.CV );
        rowLevelFilter.setLowCut( config.getLowVarianceCut() );
        rowLevelFilter.setRemoveAllNegative( false );
        rowLevelFilter.setUseAsFraction( true );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * @param matrix
     * @param ranks
     * @return
     */
    private ExpressionDataDoubleMatrix lowExpressionFilter( ExpressionDataDoubleMatrix matrix,
            Map<DesignElement, Double> ranks ) {
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

    @SuppressWarnings("unchecked")
    private boolean usesAffymetrix() {
        for ( ArrayDesign arrayDesign : arrayDesignsUsed ) {
            if ( arrayDesign.getName().toUpperCase().contains( "AFFYMETRIX" ) ) {
                return true;
            }
        }
        return false;
    }

}
