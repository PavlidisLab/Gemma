/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.analysis.expression.diff;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.TransformerUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.MathUtil;
import ubic.basecode.math.linearmodels.*;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrixUtil;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.core.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Handles fitting linear models with continuous or fixed-level covariates. Data are always log-transformed.
 * Interactions can be included if a DifferentialExpressionAnalysisConfig is passed as an argument to 'run'. Currently
 * we only support interactions if there are two factors in the model (no more).
 * One factor can be constant (the same value for all samples); such a factor will be analyzed by looking at the
 * intercept in the fitted model. This is only appropriate for 'non-reference' designs on ratiometric arrays.
 * This also supports subsetting the data based on a factor. For example, a data set with "tissue" as a factor could be
 * analyzed per-tissue rather than with tissue as a covariate.
 * This only handles the analysis, not the persistence or output of the results.
 *
 * @author paul
 */
@Component
@Scope(value = "prototype")
public class LinearModelAnalyzer extends AbstractDifferentialExpressionAnalyzer implements DisposableBean {

    /**
     * Preset levels for which we will store the HitListSizes.
     */
    private static final double[] qValueThresholdsForHitLists = new double[] { 0.001, 0.005, 0.01, 0.05, 0.1 };
    private static final Log log = LogFactory.getLog( LinearModelAnalyzer.class );

    /**
     * Factors that are always excluded from analysis
     */
    private static final List<String> EXCLUDE_CHARACTERISTICS_VALUES = new ArrayList<String>() {
        {
            this.add( "DE_Exclude" );
        }
    };

    private static final String EXCLUDE_WARNING = "Found Factor Value with DE_Exclude characteristic. Skipping current subset.";

    public static void populateFactorValuesFromBASet( BioAssaySet ee, ExperimentalFactor f,
            Collection<FactorValue> fvs ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            for ( FactorValue fv : bm.getAllFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( f ) ) {
                    fvs.add( fv );
                }
            }
        }
    }

    /**
     * Convert the data into a string-keyed matrix. Assumes that the row names of the designMatrix
     * are concordant with the column names of the namedMatrix
     */
    public static DoubleMatrix<String, String> makeDataMatrix( ObjectMatrix<String, String, Object> designMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {

        DoubleMatrix<String, String> sNamedMatrix = new DenseDoubleMatrix<>( namedMatrix.asArray() );
        for ( int i = 0; i < namedMatrix.rows(); i++ ) {
            sNamedMatrix.addRowName( namedMatrix.getRowName( i ).getId().toString() );
        }
        sNamedMatrix.setColumnNames( designMatrix.getRowNames() );
        return sNamedMatrix;
    }

    /**
     * This bioAssayDimension shouldn't get persisted; it is only for dealing with subset diff ex. analyses.
     *
     * @param  columnsToUse columns to use
     * @return bio assay dimension
     */
    public static BioAssayDimension createBADMap( List<BioMaterial> columnsToUse ) {
        /*
         * Indices of the biomaterials in the original matrix.
         */
        List<BioAssay> bioAssays = new ArrayList<>();
        for ( BioMaterial bm : columnsToUse ) {
            bioAssays.add( bm.getBioAssaysUsedIn().iterator().next() );
        }

        /*
         * fix the upper level column name maps.
         */
        BioAssayDimension reorderedDim = BioAssayDimension.Factory.newInstance();
        reorderedDim.setBioAssays( bioAssays );
        reorderedDim.setName( "For analysis" );
        reorderedDim.setDescription( bioAssays.size() + " bioAssays" );

        return reorderedDim;
    }

    /**
     * Executor used for performing analyses in the background while the current thread is reporting progress.
     * <p>
     * This bean is using the prototype scope, so a single-thread executor is suitable to prevent concurrent analyses.
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void destroy() throws Exception {
        executorService.shutdown();
    }

    /**
     * Determine if any factor should be treated as the intercept term.
     */
    @Override
    public ExperimentalFactor determineInterceptFactor( Collection<ExperimentalFactor> factors,
            QuantitationType quantitationType ) {
        ExperimentalFactor interceptFactor = null;
        for ( ExperimentalFactor experimentalFactor : factors ) {

            /*
             * Check if we need to treat the intercept as a factor.
             */
            boolean useI = this.checkIfNeedToTreatAsIntercept( experimentalFactor, quantitationType );

            if ( useI && interceptFactor != null ) {
                throw new IllegalStateException( "Can only deal with one constant factor (intercept)" );
            } else if ( useI ) {
                interceptFactor = experimentalFactor;
            }
        }
        return interceptFactor;
    }

    @Override
    public Set<HitListSize> computeHitListSizes( Collection<DifferentialExpressionAnalysisResult> results,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {
        Set<HitListSize> hitListSizes = new HashSet<>();
        StopWatch timer = new StopWatch();
        timer.start();
        double maxThreshold = MathUtil.max( LinearModelAnalyzer.qValueThresholdsForHitLists );

        assert probeToGeneMap != null;

        Collection<Gene> allGenes = new HashSet<>();
        for ( Collection<Gene> genes : probeToGeneMap.values() ) {
            allGenes.addAll( genes );
        }

        // maps from Doubles are a bit dodgy...
        Map<Double, Integer> upCounts = new HashMap<>();
        Map<Double, Integer> downCounts = new HashMap<>();
        Map<Double, Integer> eitherCounts = new HashMap<>();

        Map<Double, Integer> upCountGenes = new HashMap<>();
        Map<Double, Integer> downCountGenes = new HashMap<>();
        Map<Double, Integer> eitherCountGenes = new HashMap<>();

        Collection<Gene> seenGenes = new HashSet<>();
        for ( DifferentialExpressionAnalysisResult r : results ) {

            Double corrP = r.getCorrectedPvalue();
            if ( corrP == null || corrP > maxThreshold ) {
                continue;
            }

            CompositeSequence probe = r.getProbe();
            Collection<Gene> genesForProbe = probeToGeneMap.get( probe );
            int numGenes = 0;
            if ( genesForProbe != null ) {
                numGenes = this.countNumberOfGenesNotSeenAlready( genesForProbe, seenGenes );
            }

            // if ( numGenes == 0 ) // This is okay; it might mean we already counted it as differentially expressed.

            Collection<ContrastResult> crs = r.getContrasts();
            boolean up = false;
            boolean down = false;

            /*
             * We set up and down to be true (either or both) if at least on contrast is shown.
             */
            for ( ContrastResult cr : crs ) {
                Double lf = cr.getLogFoldChange();
                //noinspection StatementWithEmptyBody // Better readability
                if ( lf == null ) {
                    /*
                     * A contrast which is actually not valid, so it won't be counted in the hit list.
                     */
                } else if ( lf < 0 ) {
                    down = true;
                } else if ( lf > 0 ) {
                    up = true;
                }
            }

            for ( double thresh : LinearModelAnalyzer.qValueThresholdsForHitLists ) {

                if ( !upCounts.containsKey( thresh ) ) {
                    upCounts.put( thresh, 0 );
                    upCountGenes.put( thresh, 0 );
                }
                if ( !downCounts.containsKey( thresh ) ) {
                    downCounts.put( thresh, 0 );
                    downCountGenes.put( thresh, 0 );
                }
                if ( !eitherCounts.containsKey( thresh ) ) {
                    eitherCounts.put( thresh, 0 );
                    eitherCountGenes.put( thresh, 0 );
                }

                if ( corrP < thresh ) {
                    if ( up ) {
                        upCounts.put( thresh, upCounts.get( thresh ) + 1 );
                        upCountGenes.put( thresh, upCountGenes.get( thresh ) + numGenes );
                    }
                    if ( down ) {
                        downCounts.put( thresh, downCounts.get( thresh ) + 1 );
                        downCountGenes.put( thresh, downCountGenes.get( thresh ) + numGenes );
                    }

                    eitherCounts.put( thresh, eitherCounts.get( thresh ) + 1 );
                    eitherCountGenes.put( thresh, eitherCountGenes.get( thresh ) + numGenes );

                }
            }

        }

        for ( double thresh : LinearModelAnalyzer.qValueThresholdsForHitLists ) {
            // Ensure we don't set values to null.
            int up = upCounts.getOrDefault( thresh, 0 );
            int down = downCounts.getOrDefault( thresh, 0 );
            int either = eitherCounts.getOrDefault( thresh, 0 );

            int upGenes = upCountGenes.getOrDefault( thresh, 0 );
            int downGenes = downCountGenes.getOrDefault( thresh, 0 );
            int eitherGenes = eitherCountGenes.getOrDefault( thresh, 0 );

            assert !( allGenes.size() < upGenes || allGenes.size() < downGenes
                    || allGenes.size() < eitherGenes ) : "There were more genes differentially expressed than exist in the experiment";

            HitListSize upS = HitListSize.Factory.newInstance( thresh, up, Direction.UP, upGenes );
            HitListSize downS = HitListSize.Factory.newInstance( thresh, down, Direction.DOWN, downGenes );
            HitListSize eitherS = HitListSize.Factory.newInstance( thresh, either, Direction.EITHER, eitherGenes );

            hitListSizes.add( upS );
            hitListSizes.add( downS );
            hitListSizes.add( eitherS );

            assert upGenes <= eitherGenes : "More genes upregulated than upregulated+downregulated";
            assert downGenes <= eitherGenes : "More genes downregulated than upregulated+downregulated";

        }

        if ( timer.getTime() > 1000 ) {
            LinearModelAnalyzer.log.info( "Hitlist computation: " + timer.getTime() + "ms" );
        }
        return hitListSizes;
    }

    @Override
    public int getNumberOfGenesTested( Collection<DifferentialExpressionAnalysisResult> resultList,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {

        Collection<Gene> gs = new HashSet<>();
        for ( DifferentialExpressionAnalysisResult d : resultList ) {
            CompositeSequence probe = d.getProbe();
            if ( probeToGeneMap.containsKey( probe ) ) {
                gs.addAll( probeToGeneMap.get( probe ) );
            }
        }
        return gs.size();
    }

    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config ) {

        /*
         * Start by setting it up like the full experiment.
         */
        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( subset.getSourceExperiment() );
        if ( dmatrix == null ) {
            throw new RuntimeException( String.format( "There are no processed EVs for %s.", subset.getSourceExperiment() ) );
        }

        ExperimentalFactor ef = config.getSubsetFactor();
        Collection<BioMaterial> bmTmp = new HashSet<>();
        for ( BioAssay ba : subset.getBioAssays() ) {
            bmTmp.add( ba.getSampleUsed() );
        }

        List<BioMaterial> samplesInSubset = new ArrayList<>( bmTmp );

        FactorValue subsetFactorValue = null;
        for ( BioMaterial bm : samplesInSubset ) {
            Collection<FactorValue> fvs = bm.getAllFactorValues();
            for ( FactorValue fv : fvs ) {
                if ( fv.getExperimentalFactor().equals( ef ) ) {
                    if ( subsetFactorValue == null ) {
                        subsetFactorValue = fv;
                    } else if ( !subsetFactorValue.equals( fv ) ) {
                        throw new IllegalStateException(
                                "This subset has more than one factor value for the supposed subset factor: " + fv
                                        + " and " + subsetFactorValue );
                    }
                }
            }
        }

        samplesInSubset = ExpressionDataMatrixColumnSort
                .orderByExperimentalDesign( samplesInSubset, config.getFactorsToInclude() );

        // slice.
        ExpressionDataDoubleMatrix subsetMatrix = new ExpressionDataDoubleMatrix( dmatrix, samplesInSubset,
                LinearModelAnalyzer.createBADMap( samplesInSubset ) );

        Collection<ExperimentalFactor> subsetFactors = this
                .fixFactorsForSubset( dmatrix, subset, config.getFactorsToInclude() );

        if ( subsetFactors.isEmpty() ) {
            LinearModelAnalyzer.log
                    .warn( "Experimental design is not valid for subset: " + subsetFactorValue + "; skipping" );
            return null;
        }

        DifferentialExpressionAnalysisConfig subsetConfig = this
                .fixConfigForSubset( config.getFactorsToInclude(), config, subsetFactorValue );

        DifferentialExpressionAnalysis analysis = this
                .doAnalysis( subset, subsetConfig, subsetMatrix, samplesInSubset, config.getFactorsToInclude(),
                        subsetFactorValue );

        if ( analysis == null ) {
            throw new IllegalStateException( "Subset could not be analyzed with config: " + config );
        }
        return analysis;
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );
        if ( dmatrix == null ) {
            throw new RuntimeException( String.format( "There are no processed EVs for %s.", expressionExperiment ) );
        }

        return this.run( expressionExperiment, dmatrix, config );

    }

    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config ) {

        /*
         * I apologize for this being so complicated. Basically there are four phases:
         *
         * 1. Get the data matrix and factors
         *
         * 2. Determine baseline groups; build model and contrasts
         *
         * 3. Run the analysis
         *
         * 4. Postprocess the analysis
         *
         * By far the most complex is #2 -- it depends on which factors and what kind they are.
         */

        /*
         * Initialize our matrix and factor lists...
         */
        List<ExperimentalFactor> factors = config.getFactorsToInclude();

        /*
         * FIXME this is the place to strip put the outliers.
         */
        List<BioMaterial> samplesUsed = ExperimentalDesignUtils.getOrderedSamples( dmatrix, factors );

        dmatrix = new ExpressionDataDoubleMatrix( dmatrix, samplesUsed,
                LinearModelAnalyzer.createBADMap( samplesUsed ) ); // enforce ordering

        /*
         * Do the analysis, by subsets if requested
         */
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();

        ExperimentalFactor subsetFactor = config.getSubsetFactor();
        if ( subsetFactor != null ) {

            if ( factors.contains( subsetFactor ) ) {
                throw new IllegalStateException(
                        "Subset factor cannot also be included in the analysis [ Factor was: " + subsetFactor + "]" );
            }

            Map<FactorValue, ExpressionDataDoubleMatrix> subsets = this
                    .makeSubSets( config, dmatrix, samplesUsed, subsetFactor );

            LinearModelAnalyzer.log.info( "Total number of subsets: " + subsets.size() );

            /*
             * Now analyze each subset
             */
            for ( FactorValue subsetFactorValue : subsets.keySet() ) {

                LinearModelAnalyzer.log.info( "Analyzing subset: " + subsetFactorValue );

                /*
                 * Checking for DE_Exclude characteristics, which should not be included in the analysis.
                 * As requested in issue #4458 (bugzilla)
                 */
                boolean include = true;
                for ( Characteristic c : subsetFactorValue.getCharacteristics() ) {
                    if ( LinearModelAnalyzer.EXCLUDE_CHARACTERISTICS_VALUES.contains( c.getValue() ) ) {
                        include = false;
                        break;
                    }
                }
                if ( !include ) {
                    LinearModelAnalyzer.log.warn( LinearModelAnalyzer.EXCLUDE_WARNING );
                    continue;
                }

                List<BioMaterial> bioMaterials = ExperimentalDesignUtils
                        .getOrderedSamples( subsets.get( subsetFactorValue ), factors );

                /*
                 * make a EESubSet
                 */
                ExpressionExperimentSubSet eeSubSet = ExpressionExperimentSubSet.Factory.newInstance();
                eeSubSet.setSourceExperiment( expressionExperiment );
                eeSubSet.setName( "Subset for " + FactorValueUtils.getSummaryString( subsetFactorValue ) );
                Collection<BioAssay> bioAssays = new HashSet<>();
                for ( BioMaterial bm : bioMaterials ) {
                    bioAssays.addAll( bm.getBioAssaysUsedIn() );
                }
                eeSubSet.getBioAssays().addAll( bioAssays );

                Collection<ExperimentalFactor> subsetFactors = this
                        .fixFactorsForSubset( subsets.get( subsetFactorValue ), eeSubSet, factors );

                DifferentialExpressionAnalysisConfig subsetConfig = this
                        .fixConfigForSubset( factors, config, subsetFactorValue );

                if ( subsetFactors.isEmpty() ) {
                    LinearModelAnalyzer.log
                            .warn( "Experimental design is not valid for subset: " + subsetFactorValue + "; skipping" );
                    continue;
                }

                /*
                 * Run analysis on the subset.
                 */
                DifferentialExpressionAnalysis analysis = this
                        .doAnalysis( eeSubSet, subsetConfig, subsets.get( subsetFactorValue ), bioMaterials,
                                new ArrayList<>( subsetFactors ), subsetFactorValue );

                if ( analysis == null ) {
                    LinearModelAnalyzer.log
                            .warn( "No analysis results were obtained for subset: " + subsetFactorValue );
                    continue;
                }

                results.add( analysis );

            }

        } else {

            /*
             * Analyze the whole thing as one
             */
            DifferentialExpressionAnalysis analysis = this
                    .doAnalysis( expressionExperiment, config, dmatrix, samplesUsed, factors, null );
            if ( analysis == null ) {
                LinearModelAnalyzer.log.warn( "No analysis results were obtained" );
            } else {
                results.add( analysis );
            }
        }
        return results;

    }

    /*
     *
     */
    private DoubleMatrix1D getLibrarySizes( DifferentialExpressionAnalysisConfig config,
            ExpressionDataDoubleMatrix dmatrix ) {

        DoubleMatrix1D librarySize = new DenseDoubleMatrix1D( dmatrix.columns() );
        if ( config.getUseWeights() ) {
            for ( int i = 0; i < dmatrix.columns(); i++ ) {
                Collection<BioAssay> bas = dmatrix.getBioAssaysForColumn( i );
                assert bas.size() == 1;
                BioAssay ba = bas.iterator().next();

                Long sequenceReadCount = ba.getSequenceReadCount();
                if ( !ba.getIsOutlier() && ( sequenceReadCount == null || sequenceReadCount == 0 ) ) {
                    // double check.
                    Double[] col = dmatrix.getColumn( i );
                    double maxExpression = DescriptiveWithMissing.max( new cern.colt.list.DoubleArrayList( ArrayUtils.toPrimitive( col ) ) );
                    if ( maxExpression <= 0 ) {
                        throw new IllegalStateException(
                                "Sample has a null or zero read-count, isn't marked as an outlier, and max expression level is " + maxExpression
                                        + ": " + ba );
                    }
                }

                if ( sequenceReadCount == null ) sequenceReadCount = 0L;

                librarySize.set( i, sequenceReadCount );
            }
        }
        return librarySize;
    }

    /**
     * @return boolean true if we need the intercept.
     */
    private boolean checkIfNeedToTreatAsIntercept( ExperimentalFactor experimentalFactor,
            QuantitationType quantitationType ) {
        if ( experimentalFactor.getFactorValues().size() == 1 ) {
            if ( quantitationType.getIsRatio() ) {
                return true;
            }
            throw new IllegalArgumentException(
                    "Cannot deal with constant factors unless the data are ratiometric non-reference design" );
        }
        return false;
    }

    /**
     * Create files that can be used in R to check the results.
     */
    private void outputForDebugging( ExpressionDataDoubleMatrix dmatrix,
            ObjectMatrix<String, String, Object> designMatrix ) {
        MatrixWriter mw = new MatrixWriter();
        try ( FileWriter writer = new FileWriter( File.createTempFile( "data.", ".txt" ) );
                FileWriter out = new FileWriter( File.createTempFile( "design.", ".txt" ) ) ) {

            mw.write( writer, dmatrix, null, true, false );

            ubic.basecode.io.writer.MatrixWriter<String, String> dem = new ubic.basecode.io.writer.MatrixWriter<>(
                    out );
            dem.writeMatrix( designMatrix, true );

        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Populate the interactionFactorLists and label2Factors
     *
     * @param interactionFactorLists gets populated
     */
    private void buildModelFormula( final DifferentialExpressionAnalysisConfig config,
            final Map<String, Collection<ExperimentalFactor>> label2Factors,
            final List<String[]> interactionFactorLists ) {

        /*
         * Add interaction terms
         */
        boolean hasInteractionTerms = config.getInteractionsToInclude() != null && !config.getInteractionsToInclude().isEmpty();

        if ( hasInteractionTerms ) {
            for ( Collection<ExperimentalFactor> interactionTerms : config.getInteractionsToInclude() ) {

                List<String> interactionFactorNames = new ArrayList<>();
                for ( ExperimentalFactor factor : interactionTerms ) {
                    interactionFactorNames.add( ExperimentalDesignUtils.nameForR( factor ) );
                }

                interactionFactorLists.add( interactionFactorNames.toArray( new String[] {} ) );

                // In the ANOVA table.
                String factTableLabel = StringUtils.join( interactionFactorNames, ":" );
                label2Factors.put( factTableLabel, new HashSet<>() );
                label2Factors.get( factTableLabel ).addAll( interactionTerms );
            }
        }
    }

    private int countNumberOfGenesNotSeenAlready( Collection<Gene> genesForProbe, Collection<Gene> seenGenes ) {
        int numGenes = 0;
        if ( genesForProbe != null ) {
            for ( Gene g : genesForProbe ) {
                if ( seenGenes.contains( g ) ) {
                    continue;
                }
                numGenes++;
            }
            seenGenes.addAll( genesForProbe );
        }

        return numGenes;
    }

    /**
     * @param  bioAssaySet       source data, could be a SubSet
     * @param  expressionData           data (for the subset, if it's a subset)
     * @param  samplesUsed       analyzed
     * @param  factors           included in the model
     * @param  subsetFactorValue null unless analyzing a subset (only used for book-keeping)
     * @return analysis, or null if there was a problem.
     */
    private DifferentialExpressionAnalysis doAnalysis( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config, ExpressionDataDoubleMatrix expressionData,
            List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors, FactorValue subsetFactorValue ) {

        if ( factors.isEmpty() ) {
            LinearModelAnalyzer.log.error( "Must provide at least one factor" );
            return null;
        }

        if ( samplesUsed.size() <= factors.size() ) {
            LinearModelAnalyzer.log.error( "Must have more samples than factors" );
            return null;
        }

        QuantitationType quantitationType = expressionData.getQuantitationTypes().iterator().next();

        if ( config.getUseWeights() ) {
            if ( quantitationType.getScale().equals( ScaleType.COUNT ) ) {
                // just making sure something funny isn't going on.
                throw new IllegalStateException( "We're expecting log-scaled data e.g. log2cpm when using voom, something is broken." );
            }
        }

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils
                .getBaselineConditions( samplesUsed, factors );

        this.dropIncompleteFactors( samplesUsed, factors, baselineConditions );

        if ( factors.isEmpty() ) {
            LinearModelAnalyzer.log
                    .error( "All factors were removed due to incomplete values" );
            return null;
        }


        ExperimentalFactor interceptFactor = this.determineInterceptFactor( factors, quantitationType );

        /*
         * Build our factor terms, with interactions handled specially
         */
        List<String[]> interactionFactorLists = new ArrayList<>();
        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils
                .buildDesignMatrix( factors, samplesUsed, baselineConditions );

        config.setBaseLineFactorValues( baselineConditions );

        final Map<String, Collection<ExperimentalFactor>> label2Factors = this.getRNames( factors );

        boolean oneSampleTTest = interceptFactor != null && factors.size() == 1;
        if ( !oneSampleTTest ) {
            this.buildModelFormula( config, label2Factors, interactionFactorLists );
        }

        /*
         * FIXME: remove columns that are marked as outliers, this will make some steps cleaner
         */
        expressionData = ExpressionDataDoubleMatrixUtil.filterAndLog2Transform( expressionData );
        DoubleMatrix<CompositeSequence, BioMaterial> bareFilteredDataMatrix = expressionData.getMatrix();

        DoubleMatrix1D librarySizes = getLibrarySizes( config, expressionData );

        if ( LinearModelAnalyzer.log.isDebugEnabled() )
            this.outputForDebugging( expressionData, designMatrix );

        /*
         * PREPARATION FOR 'NATIVE' FITTING
         */
        DoubleMatrix<String, String> finalDataMatrix = LinearModelAnalyzer.makeDataMatrix( designMatrix, bareFilteredDataMatrix );
        DesignMatrix properDesignMatrix = this
                .makeDesignMatrix( designMatrix, interactionFactorLists, baselineConditions );

        /*
         * Run the analysis
         */
        final Map<String, LinearModelSummary> rawResults = this
                .runAnalysis( bareFilteredDataMatrix, finalDataMatrix, properDesignMatrix, librarySizes, config );

        if ( rawResults.size() == 0 ) {
            LinearModelAnalyzer.log.error( "Got no results from the analysis" );
            return null;
        }

        /*
         * Initialize data structures we need to hold results.
         */

        // this used to be a Set, but a List is much faster.
        Map<String, List<DifferentialExpressionAnalysisResult>> resultLists = new HashMap<>( properDesignMatrix.getTerms().size() );
        Map<String, List<Double>> pvaluesForQvalue = new HashMap<>( properDesignMatrix.getTerms().size() );

        // We use the design matrix to ensure that we only consider terms that actually ended up in the model. 
        for ( String factorName : properDesignMatrix.getTerms() ) {
            if ( !label2Factors.containsKey( factorName ) ) continue; // so we skip the intercept
            log.info( "Setting up results for " + factorName );
            resultLists.put( factorName, new ArrayList<>() );
            pvaluesForQvalue.put( factorName, new ArrayList<>() );
        }

        if ( pvaluesForQvalue.isEmpty() ) {
            LinearModelAnalyzer.log.warn( "No results were obtained for the current stage of analysis." );
            return null;
        }

        /*
         * Create result objects for each model fit. Keeping things in order is important.
         */
        final Transformer<CompositeSequence, Long> rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );
        boolean warned = false;
        //  int notUsable = 0;
        int processed = 0;
        for ( CompositeSequence el : bareFilteredDataMatrix.getRowNames() ) {

            if ( ++processed % 15000 == 0 ) {
                LinearModelAnalyzer.log.info( "Processed results for " + processed + " elements ..." );
            }

            LinearModelSummary lm = rawResults.get( rowNameExtractor.transform( el ).toString() );

            if ( LinearModelAnalyzer.log.isDebugEnabled() )
                LinearModelAnalyzer.log.debug( el.getName() + "\n" + lm );

            if ( lm == null ) {
                if ( !warned ) {
                    LinearModelAnalyzer.log.warn( "No result for " + el + ", further warnings suppressed" );
                    warned = true;
                }
                // notUsable++;
                continue;
            }

            for ( String factorName : label2Factors.keySet() ) {

                if ( !pvaluesForQvalue.containsKey( factorName ) ) {
                    // was dropped.
                    continue;
                }

                Double overallPValue;
                DifferentialExpressionAnalysisResult probeAnalysisResult = DifferentialExpressionAnalysisResult.Factory
                        .newInstance();
                probeAnalysisResult.setProbe( el );

                if ( lm.getCoefficients() == null ) {
                    //     notUsable++;
                    continue;
                }

                Collection<ExperimentalFactor> factorsForName = label2Factors.get( factorName );

                if ( factorsForName.size() > 1 ) {
                    /*
                     * Interactions FIXME: only enter this if the interaction term was retained in the model.
                     */
                    if ( factorsForName.size() > 2 ) {
                        LinearModelAnalyzer.log.error( "Handling more than two-way interactions is not implemented" );
                        return null;
                    }

                    assert factorName.contains( ":" );
                    String[] factorNames = StringUtils.split( factorName, ":" );
                    assert factorNames.length == factorsForName.size();
                    overallPValue = lm.getInteractionEffectP( factorNames );

                    if ( overallPValue != null && !Double.isNaN( overallPValue ) ) {

                        Map<String, Double> interactionContrastTStats = lm.getContrastTStats( factorName );
                        Map<String, Double> interactionContrastCoeffs = lm.getContrastCoefficients( factorName );
                        Map<String, Double> interactionContrastPValues = lm.getContrastPValues( factorName );

                        for ( String term : interactionContrastPValues.keySet() ) {
                            Double contrastPvalue = interactionContrastPValues.get( term );

                            this.makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                    interactionContrastTStats, interactionContrastCoeffs );

                        }
                    } else {
                        //                        if ( !warned ) {
                        //                            LinearModelAnalyzer.log.warn( "Interaction could not be computed for " + el
                        //                                    + ", further warnings suppressed" );
                        //                            warned = true;
                        //                        }
                        //
                        //                        if ( LinearModelAnalyzer.log.isDebugEnabled() )
                        //                            LinearModelAnalyzer.log.debug( "Interaction could not be computed for " + el
                        //                                    + ", further warnings suppressed" );

                        // notUsable++; // will over count?
                        continue;
                    }

                } else {

                    /*
                     * Main effect
                     */
                    assert factorsForName.size() == 1;
                    ExperimentalFactor experimentalFactor = factorsForName.iterator().next();

                    if ( factorsForName.size() == 1 && experimentalFactor.equals( interceptFactor ) ) {
                        overallPValue = lm.getInterceptP();
                    } else {
                        overallPValue = lm.getMainEffectP( factorName );
                    }

                    /*
                     * Add contrasts unless overall pvalue is NaN
                     */
                    if ( overallPValue != null && !Double.isNaN( overallPValue ) ) {

                        Map<String, Double> mainEffectContrastTStats = lm.getContrastTStats( factorName );
                        Map<String, Double> mainEffectContrastPvalues = lm.getContrastPValues( factorName );
                        Map<String, Double> mainEffectContrastCoeffs = lm.getContrastCoefficients( factorName );

                        for ( String term : mainEffectContrastPvalues.keySet() ) {

                            Double contrastPvalue = mainEffectContrastPvalues.get( term );

                            this.makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                    mainEffectContrastTStats, mainEffectContrastCoeffs );

                        }
                    } else {
                        String message = "ANOVA could not be done for " + experimentalFactor + " on " + el + ": the overall P-value is either null or NaN";
                        if ( !warned ) {
                            LinearModelAnalyzer.log.warn( message + ", further warnings suppressed" );
                            warned = true;
                        } else {
                            LinearModelAnalyzer.log.debug( message );
                        }

                        if ( LinearModelAnalyzer.log.isDebugEnabled() )

                            //  notUsable++; // will over count?
                            continue;
                    }
                }

                assert overallPValue != null && !Double.isNaN( overallPValue ) : "We should not be keeping non-number pvalues (null or NaNs)";

                probeAnalysisResult.setPvalue( this.nan2Null( overallPValue ) );
                pvaluesForQvalue.get( factorName ).add( overallPValue );
                resultLists.get( factorName ).add( probeAnalysisResult );
            } // over terms

        } // over probes

        //        if ( notUsable > 0 ) {
        //            LinearModelAnalyzer.log
        //                    .info( notUsable + " elements or results were not usable - model could not be fit, etc." );
        //        }

        this.getRanksAndQvalues( resultLists, pvaluesForQvalue );

        DifferentialExpressionAnalysis expressionAnalysis = this
                .makeAnalysisEntity( bioAssaySet, config, label2Factors, baselineConditions, interceptFactor,
                        interactionFactorLists, oneSampleTTest, resultLists, subsetFactorValue );

        LinearModelAnalyzer.log.info( "Analysis processing phase done ..." );

        return expressionAnalysis;
    }

    private void dropIncompleteFactors( List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions ) {
        Collection<ExperimentalFactor> toDrop = new HashSet<>();
        for ( ExperimentalFactor f : factors ) {
            if ( !ExperimentalDesignUtils.isComplete( f, samplesUsed, baselineConditions ) ) {
                toDrop.add( f );
                LinearModelAnalyzer.log.info( "Dropping " + f + ", missing values" );
            }
        }
        factors.removeAll( toDrop );

    }

    /**
     * Remove all configurations that have to do with factors that aren't in the selected factors.
     *
     * @param  factors the factors that will be included
     * @return an updated config; the baselines are cleared; subset is cleared; interactions are only kept if
     *                 they only
     *                 involve the given factors.
     */
    private DifferentialExpressionAnalysisConfig fixConfigForSubset( List<ExperimentalFactor> factors,
            DifferentialExpressionAnalysisConfig config, FactorValue subsetFactorValue ) {

        DifferentialExpressionAnalysisConfig newConfig = new DifferentialExpressionAnalysisConfig();

        newConfig.setBaseLineFactorValues( null );

        if ( !config.getInteractionsToInclude().isEmpty() ) {
            Collection<Collection<ExperimentalFactor>> newInteractionsToInclude = new HashSet<>();
            for ( Collection<ExperimentalFactor> interactors : config.getInteractionsToInclude() ) {
                if ( new HashSet<>( factors ).containsAll( interactors ) ) {
                    newInteractionsToInclude.add( interactors );
                }
            }

            newConfig.setInteractionsToInclude( newInteractionsToInclude );
        }

        newConfig.setSubsetFactor( null );
        newConfig.setSubsetFactorValue( subsetFactorValue );
        newConfig.setFactorsToInclude( factors );

        return newConfig;
    }

    /**
     * Remove factors which are no longer usable, based on the subset.
     */
    private Collection<ExperimentalFactor> fixFactorsForSubset( ExpressionDataDoubleMatrix dmatrix,
            ExpressionExperimentSubSet eesubSet, List<ExperimentalFactor> factors ) {

        List<ExperimentalFactor> result = new ArrayList<>();

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();
        ExperimentalFactor interceptFactor = this.determineInterceptFactor( factors, quantitationType );

        /*
         * Remove any constant factors, unless they are that intercept.
         */
        for ( ExperimentalFactor f : factors ) {

            if ( f.getType().equals( FactorType.CONTINUOUS ) ) {
                result.add( f );
            } else if ( interceptFactor != null && interceptFactor.equals( f ) ) {
                result.add( f );
            } else {

                Collection<FactorValue> levels = new HashSet<>();
                LinearModelAnalyzer.populateFactorValuesFromBASet( eesubSet, f, levels );

                if ( levels.size() > 1 ) {
                    result.add( f );
                } else {
                    LinearModelAnalyzer.log.info( "Dropping " + f + " from subset" );
                }

            }

        }

        return result;
    }

    /**
     * Needed to compute the number of genes tested/detected.
     */
    private Map<CompositeSequence, Collection<Gene>> getProbeToGeneMap(
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists ) {
        Map<CompositeSequence, Collection<Gene>> result = new HashMap<>();

        for ( Collection<DifferentialExpressionAnalysisResult> resultList : resultLists.values() ) {
            for ( DifferentialExpressionAnalysisResult d : resultList ) {
                CompositeSequence probe = d.getProbe();
                result.put( probe, new HashSet<>() );
            }
        }

        // testing environment, etc.
        if ( result.isEmpty() ) {
            return new HashMap<>();
        }

        return compositeSequenceService.getGenes( result.keySet() );

    }

    /**
     * Fill in the ranks and qvalues in the results.
     *
     * @param pvaluesForQvalue Map of factorName to results.
     */
    private void getRanksAndQvalues(
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists,
            Map<String, List<Double>> pvaluesForQvalue ) {
        /*
         * qvalues and ranks, requires second pass over the result objects.
         */
        for ( String fName : pvaluesForQvalue.keySet() ) {
            Collection<Double> pvals = pvaluesForQvalue.get( fName );

            if ( pvals.isEmpty() ) {
                LinearModelAnalyzer.log.warn( "No pvalues for " + fName + ", ignoring." );
                continue;
            }
            LinearModelAnalyzer.log.info( pvals.size() + " pvalues for " + fName );

            Double[] pvalArray = pvals.toArray( new Double[] {} );
            for ( Double aPvalArray : pvalArray ) {

                assert aPvalArray != null;
                assert !Double.isNaN( aPvalArray );

            }

            double[] ranks = super.computeRanks( ArrayUtils.toPrimitive( pvalArray ) );

            if ( ranks == null ) {
                LinearModelAnalyzer.log.error( "Ranks could not be computed " + fName );
                continue;
            }

            assert pvalArray.length == resultLists.get( fName ).size() : pvalArray.length + " != " + resultLists.get( fName ).size();

            assert pvalArray.length == ranks.length;

            double[] qvalues = super.benjaminiHochberg( pvalArray );
            assert qvalues == null || qvalues.length == resultLists.get( fName ).size();

            int i = 0;
            for ( DifferentialExpressionAnalysisResult pr : resultLists.get( fName ) ) {
                pr.setCorrectedPvalue( this.nan2Null( qvalues != null ? qvalues[i] : null ) );
                pr.setRank( this.nan2Null( ranks[i] ) );
                i++;
            }
        }
    }

    private Map<String, Collection<ExperimentalFactor>> getRNames( List<ExperimentalFactor> factors ) {
        final Map<String, Collection<ExperimentalFactor>> label2Factors = new LinkedHashMap<>();
        for ( ExperimentalFactor experimentalFactor : factors ) {
            label2Factors.computeIfAbsent( ExperimentalDesignUtils.nameForR( experimentalFactor ), k -> new HashSet<>() )
                    .add( experimentalFactor );
        }
        return label2Factors;
    }

    /**
     * @return Analysis (no-npersistent)
     */
    private DifferentialExpressionAnalysis makeAnalysisEntity( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config,
            final Map<String, Collection<ExperimentalFactor>> label2Factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions, ExperimentalFactor interceptFactor,
            List<String[]> interactionFactorLists, boolean oneSampleTtest,
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists,
            FactorValue subsetFactorValue ) {

        DifferentialExpressionAnalysis expressionAnalysis = super.initAnalysisEntity( bioAssaySet, config );

        /*
         * Complete analysis config
         */
        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( "Linear model with " + config.getFactorsToInclude().size() + " factors"
                + ( interceptFactor == null ? "" : " with intercept treated as factor" )
                + ( interactionFactorLists.isEmpty() ? "" : " with interaction" )
                + ( subsetFactorValue == null ? "" : " Using subset " + bioAssaySet + " subset value= " + subsetFactorValue ) );
        expressionAnalysis.setSubsetFactorValue( subsetFactorValue );

        Set<ExpressionAnalysisResultSet> resultSets = this
                .makeResultSets( label2Factors, baselineConditions, oneSampleTtest, expressionAnalysis, resultLists );

        expressionAnalysis.setResultSets( resultSets );

        return expressionAnalysis;
    }

    /**
     * Add a contrast to the given result.
     */
    private void makeContrast( DifferentialExpressionAnalysisResult probeAnalysisResult,
            Collection<ExperimentalFactor> experimentalFactors, String term, String factorName, Double contrastPvalue,
            Map<String, Double> tstats, Map<String, Double> coeffs ) {

        assert experimentalFactors.size() == 1 || experimentalFactors.size() == 2;

        Double contrastTstat = tstats.get( term );
        Double coefficient = coeffs.get( term );

        // no reason to store this. Note: Tiny coefficient could also be treated as a missing value, but hopefully such
        // situations would have been dealt with at a lower level.
        if ( coefficient == null || contrastTstat == null ) {
            return;
        }

        ContrastResult contrast = ContrastResult.Factory.newInstance();
        contrast.setPvalue( this.nan2Null( contrastPvalue ) );
        contrast.setTstat( this.nan2Null( contrastTstat ) );
        contrast.setCoefficient( this.nan2Null( coefficient ) );

        List<ExperimentalFactor> factorList = new ArrayList<>( experimentalFactors );
        boolean isInteraction = factorList.size() == 2;

        /*
         * The coefficient can be treated as fold-change if the data are log-transformed. This is because the
         * coefficient in the contrast is the (fitted;estimated) difference between the means, and log(x) - log(y) =
         * log(x/y). Limma uses this same trick.
         */
        contrast.setLogFoldChange( this.nan2Null( coefficient ) );

        if ( term.contains( ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX ) ) {
            // otherwise, it's continuous, and
            // we don't put in a
            // factorvalue.

            String[] terms = new String[2];
            String[] factorNames = new String[2];
            if ( term.contains( ":" ) ) {
                terms = StringUtils.split( term, ":" );
                factorNames = StringUtils.split( factorName, ":" );
            } else {
                terms[0] = term;
                factorNames[0] = factorName;
            }

            String firstTerm = terms[0];
            String secondTerm = terms[1];

            Long factorValueId;

            try {
                factorValueId = Long.parseLong(
                        firstTerm.replace( factorNames[0] + ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX, "" ) );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Failed to parse: " + firstTerm + " into a factorvalue id" );
            }

            for ( ExperimentalFactor f : factorList ) {
                for ( FactorValue fv : f.getFactorValues() ) {
                    if ( fv.getId().equals( factorValueId ) ) {
                        contrast.setFactorValue( fv );
                        break;
                    }
                }
            }

            assert contrast.getFactorValue() != null;

            if ( isInteraction ) {
                LinearModelAnalyzer.log.debug( "Interaction term" );
                assert secondTerm != null;

                try {
                    factorValueId = Long.parseLong( secondTerm
                            .replace( factorNames[1] + ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX, "" ) );
                } catch ( NumberFormatException e ) {
                    throw new RuntimeException( "Failed to parse: " + secondTerm + " into a factorvalue id" );
                }

                for ( ExperimentalFactor f : factorList ) {
                    for ( FactorValue fv : f.getFactorValues() ) {
                        if ( fv.getId().equals( factorValueId ) ) {
                            contrast.setSecondFactorValue( fv );
                            break;
                        }
                    }
                }

                if ( contrast.getSecondFactorValue() == null ) {
                    throw new IllegalStateException( "Failed to get interaction contrast second factorvalue" );
                }
            }

            if ( contrast.getFactorValue() == null ) {
                throw new IllegalStateException( "Failed to get contrast factorvalue" );
            }

            if ( contrast.getSecondFactorValue() != null && contrast.getSecondFactorValue()
                    .equals( contrast.getFactorValue() ) ) {
                throw new IllegalStateException(
                        "Contrast for interactions must be for two different factor values, got the same one twice" );
            }
        }

        probeAnalysisResult.getContrasts().add( contrast );
    }

    /**
     * Build the design matrix, including interactions if possible
     *
     * @param  designMatrix           partially setup matrix
     * @param  interactionFactorLists interactions to consider
     * @param  baselineConditions     designation of baseline conditions for each factor
     * @return final design matrix
     */
    private DesignMatrix makeDesignMatrix( ObjectMatrix<String, String, Object> designMatrix,
            List<String[]> interactionFactorLists, Map<ExperimentalFactor, FactorValue> baselineConditions ) {
        /*
         * Determine the factors and interactions to include.
         */
        DesignMatrix properDesignMatrix = new DesignMatrix( designMatrix, true );

        if ( !( interactionFactorLists == null ) && !interactionFactorLists.isEmpty() ) {
            for ( String[] in : interactionFactorLists ) {
                // we actually only support (tested etc.) one interaction at a time, but this should actually work for
                // multiple
                properDesignMatrix.addInteraction( in );
            }
        }

        for ( ExperimentalFactor ef : baselineConditions.keySet() ) {
            if ( ExperimentalDesignUtils.isContinuous( ef ) ) {
                continue;
            }
            String factorName = ExperimentalDesignUtils.nameForR( ef );
            String baselineFactorValue = ExperimentalDesignUtils.nameForR( baselineConditions.get( ef ), true );

            /*
             * If this is a subset, it is possible the baseline chosen is not eligible for the subset.
             */
            LinearModelAnalyzer.log.info( ef );

            assert baselineConditions.get( ef ).getExperimentalFactor().equals( ef ) : baselineConditions.get( ef ) + " is not a value of " + ef;
            properDesignMatrix.setBaseline( factorName, baselineFactorValue );
        }
        return properDesignMatrix;
    }

    private Set<ExpressionAnalysisResultSet> makeResultSets(
            final Map<String, Collection<ExperimentalFactor>> label2Factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions, boolean oneSampleTtest,
            DifferentialExpressionAnalysis expressionAnalysis,
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists ) {

        Map<CompositeSequence, Collection<Gene>> probeToGeneMap = this.getProbeToGeneMap( resultLists );

        /*
         * Result sets
         */
        LinearModelAnalyzer.log.info( "Processing " + resultLists.size() + " resultSets" );
        // StopWatch timer = new StopWatch();
        // timer.start();
        Set<ExpressionAnalysisResultSet> resultSets = new HashSet<>();
        for ( String fName : resultLists.keySet() ) {
            Collection<DifferentialExpressionAnalysisResult> results = resultLists.get( fName );

            Set<ExperimentalFactor> factorsUsed = new HashSet<>( label2Factors.get( fName ) );

            FactorValue baselineGroup = null;
            if ( !oneSampleTtest && factorsUsed.size() == 1 /* not interaction */ ) {
                ExperimentalFactor factor = factorsUsed.iterator().next();
                assert baselineConditions.containsKey( factor );
                baselineGroup = baselineConditions.get( factor );
            }

            Set<HitListSize> hitListSizes = this.computeHitListSizes( results, probeToGeneMap );

            int numberOfProbesTested = results.size();
            int numberOfGenesTested = this.getNumberOfGenesTested( results, probeToGeneMap );

            // make List into Set
            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory
                    .newInstance( factorsUsed, numberOfProbesTested, numberOfGenesTested, baselineGroup,
                            new HashSet<>( results ), expressionAnalysis, null, hitListSizes );
            resultSets.add( resultSet );

            LinearModelAnalyzer.log.info( "Finished with result set for " + fName );

        }
        return resultSets;
    }

    private Map<FactorValue, ExpressionDataDoubleMatrix> makeSubSets( DifferentialExpressionAnalysisConfig config,
            ExpressionDataDoubleMatrix dmatrix, List<BioMaterial> samplesUsed, ExperimentalFactor subsetFactor ) {
        if ( subsetFactor.getType().equals( FactorType.CONTINUOUS ) ) {
            throw new IllegalArgumentException( "You cannot subset on a continuous factor (has a Measurement)" );
        }

        if ( config.getFactorsToInclude().contains( subsetFactor ) ) {
            throw new IllegalArgumentException(
                    "You cannot analyze a factor and use it for subsetting at the same time." );
        }

        Map<FactorValue, List<BioMaterial>> subSetSamples = new HashMap<>( subsetFactor.getFactorValues().size() );
        for ( FactorValue fv : subsetFactor.getFactorValues() ) {
            assert fv.getMeasurement() == null;
            subSetSamples.put( fv, new ArrayList<>() );
        }

        for ( BioMaterial sample : samplesUsed ) {
            boolean ok = false;
            for ( FactorValue fv : sample.getAllFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( subsetFactor ) ) {
                    subSetSamples.get( fv ).add( sample );
                    ok = true;
                    break;
                }
            }
            if ( !ok ) {
                throw new IllegalArgumentException(
                        "Cannot subset on a factor unless each sample has a value for it. Missing value for: " + sample
                                + " " + sample.getBioAssaysUsedIn() );
            }
        }

        Map<FactorValue, ExpressionDataDoubleMatrix> subMatrices = new HashMap<>();
        for ( FactorValue fv : subSetSamples.keySet() ) {
            List<BioMaterial> samplesInSubset = subSetSamples.get( fv );

            if ( samplesInSubset.isEmpty() ) {
                throw new IllegalArgumentException( "The subset was empty for fv: " + fv );
            }
            assert samplesInSubset.size() < samplesUsed.size();

            samplesInSubset = ExpressionDataMatrixColumnSort
                    .orderByExperimentalDesign( samplesInSubset, config.getFactorsToInclude() );
            ExpressionDataDoubleMatrix subMatrix = new ExpressionDataDoubleMatrix( dmatrix, samplesInSubset,
                    LinearModelAnalyzer.createBADMap( samplesInSubset ) );
            subMatrices.put( fv, subMatrix );
        }

        return subMatrices;

    }

    /**
     * Important bit. Run the analysis
     *
     * @return results
     */
    private Map<String, LinearModelSummary> runAnalysis( final DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix,
            final DoubleMatrix<String, String> sNamedMatrix, DesignMatrix designMatrix,
            final DoubleMatrix1D librarySize, final DifferentialExpressionAnalysisConfig config ) {

        // perform the analysis in a background thread, so that we can provide feedback and interrupt it if it takes
        // too long
        Future<Map<String, LinearModelSummary>> f = executorService.submit( () -> {
            StopWatch timer = new StopWatch();
            timer.start();
            LeastSquaresFit fit;
            if ( config.getUseWeights() ) {
                MeanVarianceEstimator mv = new MeanVarianceEstimator( designMatrix, sNamedMatrix, librarySize );
                LinearModelAnalyzer.log.info( "Model weights from mean-variance model: " + timer.getTime() + "ms" );
                timer.reset();
                timer.start();
                fit = new LeastSquaresFit( designMatrix, sNamedMatrix, mv.getWeights() );

                // DEBUG CODE
                //                    try {
                // String dir = "/Users/pzoot";
                //                        File file = File.createTempFile( "loess-fit-", ".txt", new File( dir ) );
                //                        OutputStream os = new PrintStream( file );
                //                        ubic.basecode.io.writer.MatrixWriter w = new ubic.basecode.io.writer.MatrixWriter( os );
                //                        w.writeMatrix( mv.getLoess() );
                //
                //                        File f2 = File.createTempFile( "mv-", ".txt", new File( dir ) );
                //                        OutputStream os2 = new PrintStream( f2 );
                //                        ubic.basecode.io.writer.MatrixWriter w2 = new ubic.basecode.io.writer.MatrixWriter( os2 );
                //                        w2.writeMatrix( mv.getMeanVariance() );
                //
                //                        File f3 = File.createTempFile( "prepared-data-", ".txt", new File( dir ) );
                //                        OutputStream os3 = new PrintStream( f3 );
                //                        ubic.basecode.io.writer.MatrixWriter w3 = new ubic.basecode.io.writer.MatrixWriter( os3 );
                //                        w3.writeMatrix( new DenseDoubleMatrix2D( preparedData.asArray() ) );
                //
                //                        File f4 = File.createTempFile( "voom-weights-", ".txt", new File( dir ) );
                //                        OutputStream os4 = new PrintStream( f4 );
                //                        ubic.basecode.io.writer.MatrixWriter w4 = new ubic.basecode.io.writer.MatrixWriter( os4 );
                //                        w4.writeMatrix( new DenseDoubleMatrix2D( preparedData.asArray() ) );
                //
                //                        File f5 = File.createTempFile( "designmatrix-", ".txt", new File( dir ) );
                //                        OutputStream os5 = new PrintStream( f5 );
                //                        ubic.basecode.io.writer.MatrixWriter w5 = new ubic.basecode.io.writer.MatrixWriter( os5 );
                //                        w5.writeMatrix( designMatrix.getMatrix(), true );
                //
                //                        File f6 = File.createTempFile( "libsize-", ".txt", new File( dir ) );
                //                        OutputStream os6 = new PrintStream( f6 );
                //                        ubic.basecode.io.writer.MatrixWriter w6 = new ubic.basecode.io.writer.MatrixWriter( os6 );
                //                        w6.writeMatrix( librarySize );
                //                    } catch ( Exception e ) {
                //                        ///
                //                    }

            } else {
                fit = new LeastSquaresFit( designMatrix, sNamedMatrix );
            }
            LinearModelAnalyzer.log
                    .info( "Model fit preparedData matrix " + sNamedMatrix.rows() + " x " + sNamedMatrix.columns() + ": " + timer.getTime()
                            + "ms" );
            timer.reset();
            timer.start();
            if ( config.getModerateStatistics() ) {
                ModeratedTstat.ebayes( fit );

                // just for printing to logs:
                double rdof = 0.0;
                if ( fit.isHasMissing() ) {
                    List<Integer> dofs = fit.getResidualDofs();
                    for ( Integer k : dofs ) {
                        rdof += k;
                    }
                    rdof = rdof / ( double ) dofs.size();
                } else {
                    rdof = fit.getResidualDof();
                }
                LinearModelAnalyzer.log.info( "Moderate test statistics: " + timer.getTime() + "ms; Mean.residual.dof=" + rdof + " dfPrior=" + fit.getDfPrior() + " varPrior=" + fit.getVarPrior() );
            }

            timer.reset();

            timer.start();
            Map<String, LinearModelSummary> res = fit.summarizeByKeys( true );
            LinearModelAnalyzer.log.info( "Model summarize/ANOVA: " + timer.getTime() + "ms" );
            LinearModelAnalyzer.log.info( "Analysis phase done ..." );
            return res;
        } );

        StopWatch timer = StopWatch.createStarted();
        long lastTime = 0;

        // this analysis should take just 10 or 20 seconds for most data sets.
        // but there are cases that take longer; addressing https://github.com/PavlidisLab/Gemma/issues/13
        // would help.
        // double MAX_ANALYSIS_TIME = 60 * 1000 * 100; // 100 minutes.
        double updateIntervalMillis = 60 * 1000;// 1 minute
        while ( true ) {
            try {
                Map<String, LinearModelSummary> rawResults = f.get( 500, TimeUnit.MILLISECONDS );
                if ( timer.getTime() > updateIntervalMillis ) {
                    LinearModelAnalyzer.log
                            .info( String.format( "Analysis finished in %.1f minutes.", timer.getTime( TimeUnit.SECONDS ) / 60.00 ) );
                }
                assert rawResults.size() == namedMatrix.rows() : "expected " + namedMatrix.rows() + " results, got " + rawResults.size();
                return rawResults;
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                LinearModelAnalyzer.log.warn( "Analysis interrupted!" );
                return Collections.emptyMap();
            } catch ( ExecutionException e ) {
                throw new RuntimeException( e.getCause() );
            } catch ( TimeoutException e ) {
                if ( timer.getTime() - lastTime > updateIntervalMillis ) {
                    LinearModelAnalyzer.log.info( String
                            .format( "Analysis running, %.1f minutes elapsed ...", timer.getTime( TimeUnit.SECONDS ) / 60.00 ) );
                    lastTime = timer.getTime();
                }
                // if ( timer.getTime() > MAX_ANALYSIS_TIME ) {
                //     LinearModelAnalyzer.log
                //             .error( "Analysis is taking too long, something bad must have happened; cancelling" );
                //     f.cancel( true );
                //     throw new RuntimeException( "Analysis was taking too long, it was cancelled" );
                // }
            }
        }
    }
}
