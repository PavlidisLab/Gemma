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
import ubic.gemma.analysis.preprocess.filter.InsufficientSamplesException;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
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
     * @param config configuration used for all filtering. This must be defined at constructio and cannot be changed
     *        afterwards.
     */
    public ExpressionExperimentFilter( ExpressionExperiment ee, FilterConfig config ) {
        this.ee = ee;
        this.config = config;
    }

    /**
     * @param ee
     * @param dataVectors
     * @return
     */
    public ExpressionDataDoubleMatrix getFilteredMatrix( Collection<DesignElementDataVector> dataVectors ) {
        log.info( "Getting expression data..." );
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
     * Apply filters as configured by the command line parameters and technology type.
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

        boolean twoColor = isTwoColor();
        if ( config.isMinPresentFractionIsSet() && twoColor ) {
            /* Apply two color missing value filter */
            builder.maskMissingValues( filteredMatrix, null );
            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData( null );

            log.info( "Filtering for missing data" );
            filteredMatrix = minPresentFilter( filteredMatrix, missingValues );

            if ( config.isLowVarianceCutIsSet() ) {
                log.info( "Filtering for low variance" );
                filteredMatrix = lowVarianceFilter( filteredMatrix );
            }
        }

        if ( !twoColor ) {

            // NOTE we do not use the PresentAbsent values here. Only filtering on the basis of the data itself.
            if ( config.isMinPresentFractionIsSet() ) {
                log.info( "Filtering for missing data" );
                filteredMatrix = minPresentFilter( filteredMatrix, null );
            }

            if ( config.isLowExpressionCutIsSet() ) {
                log.info( "Filtering for low expression" );
                filteredMatrix = lowExpressionFilter( filteredMatrix );
            }

            if ( config.isLowVarianceCutIsSet() ) {
                log.info( "Filtering for low variance" );
                filteredMatrix = lowCVFilter( filteredMatrix );
            }

            if ( usesAffymetrix() ) {
                log.info( "Filtering Affymetrix controls" );
                filteredMatrix = affyControlProbeFilter( filteredMatrix );
            }
        }
        return filteredMatrix;
    }

    /**
     * @param ee
     * @param builder
     * @param eeDoubleMatrix
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
     * Determine if the expression experiment uses two-color arrays. This is not guaranteed to give the right answer if
     * the experiment uses both types of technologies.
     * 
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean isTwoColor() {
        ArrayDesign arrayDesign = arrayDesignsUsed.iterator().next();
        TechnologyType techType = arrayDesign.getTechnologyType();
        return techType.equals( TechnologyType.TWOCOLOR ) || techType.equals( TechnologyType.DUALMODE );
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
     * @return
     */
    private ExpressionDataDoubleMatrix lowExpressionFilter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setLowCut( config.getLowExpressionCut() );
        rowLevelFilter.setHighCut( config.getHighExpressionCut() );
        rowLevelFilter.setRemoveAllNegative( true );
        rowLevelFilter.setUseAsFraction( true );
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
     * Remove rows that have too many missing values. Note that we normally only apply this to ratiometric arrays, not
     * one color (e.g., affymetrix) data.
     * 
     * @param matrix
     * @return filtered matrix
     */
    private ExpressionDataDoubleMatrix minPresentFilter( ExpressionDataDoubleMatrix matrix,
            ExpressionDataBooleanMatrix absentPresent ) {
        log.info( "Filtering out genes that are missing too many values" );

        RowMissingValueFilter rowMissingFilter = new RowMissingValueFilter();
        if ( absentPresent != null ) {
            if ( absentPresent.rows() != matrix.rows() ) {
                log.warn( "Missing value matrix has " + absentPresent.rows() + " rows (!=" + matrix.rows() + ")" );
            }

            if ( absentPresent.columns() != matrix.columns() ) {
                throw new IllegalArgumentException( "Missing value matrix has " + absentPresent.columns()
                        + " columns (!=" + matrix.columns() + ")" );
            }

            rowMissingFilter.setAbsentPresentCalls( absentPresent );
        }
        rowMissingFilter.setMinPresentFraction( config.getMinPresentFraction() );

        /*
         * Always require at least 5 samples.
         */
        rowMissingFilter.setMinPresentCount( MIN_NUMBER_OF_SAMPLES_PRESENT );

        return rowMissingFilter.filter( matrix );
    }

    @SuppressWarnings("unchecked")
    private boolean usesAffymetrix() {
        ArrayDesign arrayDesign = arrayDesignsUsed.iterator().next();
        return arrayDesign.getName().toUpperCase().contains( "AFFYMETRIX" );
    }

}
