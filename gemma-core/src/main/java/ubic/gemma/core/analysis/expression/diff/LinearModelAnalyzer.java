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

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.MathUtil;
import ubic.basecode.math.MultipleTestCorrection;
import ubic.basecode.math.Rank;
import ubic.basecode.math.linearmodels.*;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.io.MatrixWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils.createBADMap;
import static ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils.makeDataMatrix;
import static ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils.filterAndLog2Transform;
import static ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort.orderByExperimentalDesign;

/**
 * Handles fitting linear models with continuous or fixed-level covariates. Data are always log-transformed.
 * <p>
 * Interactions can be included if a DifferentialExpressionAnalysisConfig is passed as an argument to 'run'. Currently
 * we only support interactions if there are two factors in the model (no more).
 * <p>
 * One factor can be constant (the same value for all samples); such a factor will be analyzed by looking at the
 * intercept in the fitted model. This is only appropriate for 'non-reference' designs on ratiometric arrays.
 * <p>
 * This also supports subsetting the data based on a factor. For example, a data set with "tissue" as a factor could be
 * analyzed per-tissue rather than with tissue as a covariate.
 * <p>
 * This only handles the analysis, not the persistence or output of the results.
 *
 * @author paul
 */
@Component
@CommonsLog
@ParametersAreNonnullByDefault
public class LinearModelAnalyzer implements DiffExAnalyzer {

    /**
     * Preset levels for which we will store the HitListSizes.
     */
    private static final double[] qValueThresholdsForHitLists = new double[] { 0.001, 0.005, 0.01, 0.05, 0.1 };

    /**
     * Factors that are always excluded from analysis
     */
    private static final List<String> EXCLUDE_CHARACTERISTICS_VALUES = new ArrayList<String>() {
        {
            this.add( "DE_Exclude" );
        }
    };

    private static final String EXCLUDE_WARNING = "Found Factor Value with DE_Exclude characteristic. Skipping current subset.";

    private static final int MAX_SUBSET_NAME_LENGTH = 255;

    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private AsyncTaskExecutor taskExecutor;
    @Autowired
    private EntityUrlBuilder entityUrlBuilder;
    @Autowired
    private BuildInfo buildInfo;

    /**
     * Generate HitListSize entities that will be stored to count the number of diff. ex probes at various preset
     * thresholds, to avoid wasting time generating these counts on the fly later. This is done automatically during
     * analysis, so is just here to allow 'backfilling'.
     *
     * @param probeToGeneMap map
     * @param results        results
     * @return hit list sizes
     */
    private Set<HitListSize> computeHitListSizes( Collection<DifferentialExpressionAnalysisResult> results,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {
        Set<HitListSize> hitListSizes = new HashSet<>();
        StopWatch timer = new StopWatch();
        timer.start();
        double maxThreshold = MathUtil.max( LinearModelAnalyzer.qValueThresholdsForHitLists );

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

    /**
     * Utility method
     *
     * @param probeToGeneMap map
     * @param resultList     result list
     * @return number of genes tested
     */
    private int getNumberOfGenesTested( Collection<DifferentialExpressionAnalysisResult> resultList,
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


    /**
     * I apologize for this being so complicated. Basically there are four phases:
     * <ol>
     * <li>Get the data matrix and factors</li>
     * <li>Determine baseline groups; build model and contrasts</li>
     * <li>Run the analysis</li>
     * <li>Postprocess the analysis</li>
     * </ol>
     * By far the most complex is #2 -- it depends on which factors and what kind they are.
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config ) {

        /*
         * Initialize our matrix and factor lists...
         */
        List<ExperimentalFactor> factors = ExperimentalDesignUtils.getOrderedFactors( config.getFactorsToInclude() );

        /*
         * FIXME this is the place to strip put the outliers.
         */
        List<BioMaterial> samplesUsed = orderByExperimentalDesign( dmatrix, factors );

        dmatrix = new ExpressionDataDoubleMatrix( dmatrix, samplesUsed,
                createBADMap( samplesUsed ) ); // enforce ordering

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils
                .getBaselineConditions( samplesUsed, factors );
        dropIncompleteFactors( samplesUsed, factors );

        /*
         * Do the analysis, by subsets if requested
         */
        if ( config.getSubsetFactor() != null ) {
            return doSubSetAnalysis( expressionExperiment, samplesUsed, factors, baselineConditions, config.getSubsetFactor(), dmatrix, config );
        } else {
            /*
             * Analyze the whole thing as one
             */
            return Collections.singleton( doAnalysis( expressionExperiment, dmatrix, samplesUsed, factors, baselineConditions, null, config ) );
        }
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment ee, Map<FactorValue, ExpressionExperimentSubSet> subsets, ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config ) {
        Assert.notNull( config.getSubsetFactor(), "Subset factor must be provided" );
        Assert.isTrue( !subsets.isEmpty(), "No subsets provided" );
        Assert.isTrue( config.getSubsetFactor().getFactorValues().containsAll( subsets.keySet() ), "Subsets must use factor values from " + config.getSubsetFactor() + "." );
        Assert.isTrue( subsets.values().stream().allMatch( ss -> ss.getSourceExperiment().equals( ee ) ), "Subsets must use " + ee + " as source experiment." );
        List<ExperimentalFactor> factors = ExperimentalDesignUtils.getOrderedFactors( config.getFactorsToInclude() );
        List<BioMaterial> samplesUsed = orderByExperimentalDesign( dmatrix, factors );
        Map<FactorValue, ExpressionDataDoubleMatrix> dmatrixBySubSet = makeSubSetMatrices( dmatrix, samplesUsed, factors, config.getSubsetFactor() );
        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils
                .getBaselineConditions( samplesUsed, factors );
        dropIncompleteFactors( samplesUsed, factors );
        return doSubSetAnalysis( subsets, dmatrixBySubSet, factors, baselineConditions, config );
    }

    /**
     * Perform an analysis by subset.
     */
    private Collection<DifferentialExpressionAnalysis> doSubSetAnalysis( ExpressionExperiment expressionExperiment, List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors, Map<ExperimentalFactor, FactorValue> baselineConditions, ExperimentalFactor subsetFactor, ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config ) {
        Assert.isTrue( !factors.contains( subsetFactor ),
                "Subset factor cannot also be included in the analysis [ Factor was: " + subsetFactor + "]" );

        Map<FactorValue, ExpressionDataDoubleMatrix> dmatrixBySubset = makeSubSetMatrices( dmatrix, samplesUsed, factors, subsetFactor );

        /*
         * Now analyze each subset
         */
        Map<FactorValue, ExpressionExperimentSubSet> subsets = new HashMap<>();
        for ( FactorValue subsetFactorValue : dmatrixBySubset.keySet() ) {
            /*
             * Checking for DE_Exclude characteristics, which should not be included in the analysis.
             * As requested in issue #4458 (bugzilla)
             */
            if ( isExcluded( subsetFactorValue ) ) {
                LinearModelAnalyzer.log.warn( LinearModelAnalyzer.EXCLUDE_WARNING );
                continue;
            }

            LinearModelAnalyzer.log.info( "Analyzing subset: " + subsetFactorValue );

            List<BioMaterial> bioMaterials = orderByExperimentalDesign( dmatrixBySubset.get( subsetFactorValue ), factors );

            /*
             * make a EESubSet
             */
            String subsetName = "Subset for " + FactorValueUtils.getSummaryString( subsetFactorValue );
            if ( subsetName.length() > MAX_SUBSET_NAME_LENGTH ) {
                log.warn( "Name for resulting subset of " + subsetFactorValue + " exceeds " + MAX_SUBSET_NAME_LENGTH + " characters, it will be abbreviated." );
                subsetName = StringUtils.abbreviate( subsetName, MAX_SUBSET_NAME_LENGTH );
            }
            ExpressionExperimentSubSet eeSubSet = ExpressionExperimentSubSet.Factory.newInstance( subsetName, expressionExperiment );
            Collection<BioAssay> bioAssays = new HashSet<>();
            for ( BioMaterial bm : bioMaterials ) {
                bioAssays.addAll( bm.getBioAssaysUsedIn() );
            }
            eeSubSet.getBioAssays().addAll( bioAssays );

            subsets.put( subsetFactorValue, eeSubSet );
        }

        LinearModelAnalyzer.log.info( "Total number of subsets: " + subsets.size() );

        return doSubSetAnalysis( subsets, dmatrixBySubset, factors, baselineConditions, config );
    }

    private Collection<DifferentialExpressionAnalysis> doSubSetAnalysis( Map<FactorValue, ExpressionExperimentSubSet> subsets, Map<FactorValue, ExpressionDataDoubleMatrix> dmatrix, List<ExperimentalFactor> factors, Map<ExperimentalFactor, FactorValue> baselineConditions, DifferentialExpressionAnalysisConfig config ) {
        /*
         * Now analyze each subset
         */
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( FactorValue subsetFactorValue : subsets.keySet() ) {
            if ( isExcluded( subsetFactorValue ) ) {
                LinearModelAnalyzer.log.warn( LinearModelAnalyzer.EXCLUDE_WARNING );
                continue;
            }

            LinearModelAnalyzer.log.info( "Analyzing subset: " + subsetFactorValue );

            List<BioMaterial> bioMaterials = orderByExperimentalDesign( dmatrix.get( subsetFactorValue ), factors );

            List<ExperimentalFactor> subsetFactors = this
                    .fixFactorsForSubset( subsets.get( subsetFactorValue ), dmatrix.get( subsetFactorValue ), factors );

            DifferentialExpressionAnalysisConfig subsetConfig = this
                    .fixConfigForSubset( factors, subsetFactorValue, config );

            if ( subsetFactors.isEmpty() ) {
                LinearModelAnalyzer.log
                        .warn( "Experimental design is not valid for subset: " + subsetFactorValue + "; skipping" );
                continue;
            }

            /*
             * Run analysis on the subset.
             */
            results.add( doAnalysis( subsets.get( subsetFactorValue ), dmatrix.get( subsetFactorValue ), bioMaterials,
                    subsetFactors, baselineConditions, subsetFactorValue, subsetConfig ) );
        }

        return results;
    }

    /**
     * Check if a factor value should be excluded from the analysis.
     */
    private boolean isExcluded( FactorValue subsetFactorValue ) {
        for ( Characteristic c : subsetFactorValue.getCharacteristics() ) {
            if ( LinearModelAnalyzer.EXCLUDE_CHARACTERISTICS_VALUES.contains( c.getValue() ) ) {
                return true;
            }
        }
        return false;
    }


    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperimentSubSet subset, ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config ) {
        Assert.notNull( config.getSubsetFactor(), "A subset factor must be set to analyze a subset." );

        /*
         * Start by setting it up like the full experiment.
         */

        ExperimentalFactor ef = config.getSubsetFactor();

        List<BioMaterial> samplesInSubset = subset.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .sorted( Comparator.comparing( BioMaterial::getId ) )
                .collect( Collectors.toList() );

        FactorValue subsetFactorValue = config.getSubsetFactorValue();
        if ( subsetFactorValue == null ) {
            log.info( "No factor value set in the configuration, will determine it from the samples..." );
            subsetFactorValue = determineSubsetFactorValue( ef, subset );
        }

        orderByExperimentalDesign( samplesInSubset, config.getFactorsToInclude() );

        // slice.
        ExpressionDataDoubleMatrix subsetMatrix = new ExpressionDataDoubleMatrix( dmatrix, samplesInSubset,
                createBADMap( samplesInSubset ) );

        List<ExperimentalFactor> factors = ExperimentalDesignUtils.getOrderedFactors( config.getFactorsToInclude() );
        List<ExperimentalFactor> subsetFactors = fixFactorsForSubset( subset, dmatrix, factors );

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils
                .getBaselineConditions( samplesInSubset, factors );
        dropIncompleteFactors( samplesInSubset, factors );

        if ( factors.isEmpty() ) {
            throw new IllegalStateException( "All factors were removed due to incomplete values" );
        }

        if ( subsetFactors.isEmpty() ) {
            LinearModelAnalyzer.log
                    .warn( "Experimental design is not valid for subset: " + subsetFactorValue + "; skipping" );
            return null;
        }

        DifferentialExpressionAnalysisConfig subsetConfig = fixConfigForSubset( subsetFactors, subsetFactorValue, config );

        return doAnalysis( subset, subsetMatrix, samplesInSubset, subsetFactors,
                baselineConditions, subsetFactorValue, subsetConfig );
    }

    /**
     * Determine which factor value is used by a given subset.
     */
    private FactorValue determineSubsetFactorValue( ExperimentalFactor subsetFactor, ExpressionExperimentSubSet subset ) {
        FactorValue subsetFactorValue = null;
        for ( BioAssay ba : subset.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            Set<FactorValue> fvs = bm.getAllFactorValues();
            boolean found = false;
            for ( FactorValue fv : fvs ) {
                if ( fv.getExperimentalFactor().equals( subsetFactor ) ) {
                    if ( subsetFactorValue == null ) {
                        subsetFactorValue = fv;
                    } else if ( subsetFactorValue.equals( fv ) ) {
                        found = true;
                    } else {
                        throw new IllegalStateException( String.format( "%s has more than one factor value for %s.", bm, subsetFactor ) );
                    }
                }
            }
            if ( !found ) {
                throw new IllegalStateException( String.format( "%s does not have a factor value for %s.", bm, subsetFactor ) );
            }
        }
        if ( subsetFactorValue == null ) {
            throw new IllegalStateException( String.format( "Failed to determine subset factor value for %s: none of the sample have a factor value for %s.", subset, subsetFactor ) );
        }
        return subsetFactorValue;
    }

    private DoubleMatrix1D getLibrarySizes( DifferentialExpressionAnalysisConfig config,
            ExpressionDataDoubleMatrix dmatrix ) {

        DoubleMatrix1D librarySize = new DenseDoubleMatrix1D( dmatrix.columns() );
        if ( config.isUseWeights() ) {
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
        MatrixWriter mw = new MatrixWriter( entityUrlBuilder, buildInfo );
        try ( FileWriter writer = new FileWriter( File.createTempFile( "data.", ".txt" ) );
                FileWriter out = new FileWriter( File.createTempFile( "design.", ".txt" ) ) ) {

            mw.write( dmatrix, writer );

            ubic.basecode.io.writer.MatrixWriter<String, String> dem = new ubic.basecode.io.writer.MatrixWriter<>(
                    out );
            dem.writeMatrix( designMatrix, true );

        } catch ( IOException e ) {
            log.error( "An I/O error occurred when producing debugging output.", e );
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

    private int countNumberOfGenesNotSeenAlready( @Nullable Collection<Gene> genesForProbe, Collection<Gene> seenGenes ) {
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
     * @param bioAssaySet        source data, could be a SubSet
     * @param expressionData     data (for the subset, if it's a subset)
     * @param samplesUsed        samples analyzed
     * @param factors            factors included in the model
     * @param baselineConditions for each categorical factor used in the model, the baseline condition
     * @param subsetFactorValue null unless analyzing a subset (only used for book-keeping)
     */
    @Nonnull
    private DifferentialExpressionAnalysis doAnalysis( BioAssaySet bioAssaySet,
            ExpressionDataDoubleMatrix expressionData, List<BioMaterial> samplesUsed,
            List<ExperimentalFactor> factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions,
            @Nullable FactorValue subsetFactorValue,
            DifferentialExpressionAnalysisConfig config ) {

        if ( factors.isEmpty() ) {
            throw new IllegalArgumentException( "Must provide at least one factor" );
        }

        if ( samplesUsed.size() <= factors.size() ) {
            throw new IllegalArgumentException( "Must have more samples than factors" );
        }

        QuantitationType quantitationType = expressionData.getQuantitationTypes().iterator().next();

        if ( config.isUseWeights() ) {
            if ( quantitationType.getScale().equals( ScaleType.COUNT ) ) {
                // just making sure something funny isn't going on.
                throw new IllegalStateException( "We're expecting log-scaled data e.g. log2cpm when using voom, something is broken." );
            }
        }


        ExperimentalFactor interceptFactor = this.determineInterceptFactor( factors, quantitationType );

        /*
         * Build our factor terms, with interactions handled specially
         */
        List<String[]> interactionFactorLists = new ArrayList<>();
        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils
                .buildRDesignMatrix( factors, samplesUsed, baselineConditions, false );

        config.addBaseLineFactorValues( baselineConditions );

        final Map<String, Collection<ExperimentalFactor>> label2Factors = this.getRNames( factors );

        boolean oneSampleTTest = interceptFactor != null && factors.size() == 1;
        if ( !oneSampleTTest ) {
            this.buildModelFormula( config, label2Factors, interactionFactorLists );
        }

        /*
         * FIXME: remove columns that are marked as outliers, this will make some steps cleaner
         */
        try {
            expressionData = filterAndLog2Transform( expressionData );
        } catch ( QuantitationTypeConversionException e ) {
            throw new RuntimeException( e );
        }
        DoubleMatrix<CompositeSequence, BioMaterial> bareFilteredDataMatrix = expressionData.getMatrix();

        DoubleMatrix1D librarySizes = getLibrarySizes( config, expressionData );

        if ( LinearModelAnalyzer.log.isDebugEnabled() )
            this.outputForDebugging( expressionData, designMatrix );

        /*
         * PREPARATION FOR 'NATIVE' FITTING
         */
        DoubleMatrix<String, String> finalDataMatrix = makeDataMatrix( designMatrix, bareFilteredDataMatrix );
        DesignMatrix properDesignMatrix = this
                .makeDesignMatrix( designMatrix, interactionFactorLists, baselineConditions );

        /*
         * Run the analysis
         */
        final Map<String, LinearModelSummary> rawResults = this
                .runAnalysis( bareFilteredDataMatrix, finalDataMatrix, properDesignMatrix, librarySizes, config );

        if ( rawResults.isEmpty() ) {
            throw new IllegalStateException( "Got no results from the analysis" );
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
            throw new IllegalStateException( "No results were obtained for the current stage of analysis." );
        }

        /*
         * Create result objects for each model fit. Keeping things in order is important.
         */
        int warned = 0;
        int processed = 0;
        //  int notUsable = 0;
        for ( CompositeSequence el : bareFilteredDataMatrix.getRowNames() ) {
            LinearModelSummary lm = rawResults.get( DiffExAnalyzerUtils.nameForR( el ) );

            if ( LinearModelAnalyzer.log.isDebugEnabled() )
                LinearModelAnalyzer.log.debug( el.getName() + "\n" + lm );

            if ( lm == null ) {
                warnForElement( el, "No result, skipping.", warned++ );
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
                    warnForElement( el, "Coefficients for " + factorName + " were null, skipping.", warned++ );
                    continue;
                }

                Collection<ExperimentalFactor> factorsForName = label2Factors.get( factorName );

                if ( factorsForName.isEmpty() ) {
                    throw new IllegalStateException( "Expected at least one factor for " + el + " and " + factorName + "." );
                }

                if ( factorsForName.size() == 1 ) {
                    /*
                     * Main effect
                     */
                    ExperimentalFactor experimentalFactor = factorsForName.iterator().next();

                    if ( experimentalFactor.equals( interceptFactor ) ) {
                        overallPValue = lm.getInterceptP();
                    } else {
                        overallPValue = lm.getMainEffectP( factorName );
                    }

                    /*
                     * Add contrasts unless overall pvalue is NaN
                     */
                    if ( overallPValue == null || Double.isNaN( overallPValue ) ) {
                        warnForElement( el, "ANOVA could not be done for " + experimentalFactor + ", the overall P-value is either null or NaN.", warned++ );
                        continue;
                    }

                    Map<String, Double> mainEffectContrastTStats = lm.getContrastTStats( factorName );
                    Map<String, Double> mainEffectContrastPvalues = lm.getContrastPValues( factorName );
                    Map<String, Double> mainEffectContrastCoeffs = lm.getContrastCoefficients( factorName );

                    for ( String term : mainEffectContrastPvalues.keySet() ) {

                        Double contrastPvalue = mainEffectContrastPvalues.get( term );

                        this.makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                mainEffectContrastTStats, mainEffectContrastCoeffs );

                    }
                } else if ( factorsForName.size() == 2 ) {
                    /*
                     * Interactions FIXME: only enter this if the interaction term was retained in the model.
                     */
                    assert factorName.contains( ":" );
                    String[] factorNames = StringUtils.split( factorName, ":" );
                    assert factorNames.length == factorsForName.size();
                    overallPValue = lm.getInteractionEffectP( factorNames );

                    if ( overallPValue == null || Double.isNaN( overallPValue ) ) {
                        warnForElement( el, "Overall P-value for " + factorName + " was either null or NaN, skipping.", warned++ );
                        continue;
                    }

                    Map<String, Double> interactionContrastTStats = lm.getContrastTStats( factorName );
                    Map<String, Double> interactionContrastCoeffs = lm.getContrastCoefficients( factorName );
                    Map<String, Double> interactionContrastPValues = lm.getContrastPValues( factorName );

                    for ( String term : interactionContrastPValues.keySet() ) {
                        Double contrastPvalue = interactionContrastPValues.get( term );

                        this.makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                interactionContrastTStats, interactionContrastCoeffs );

                    }
                } else {
                    throw new UnsupportedOperationException( "Handling more than two-way interactions is not implemented." );
                }

                probeAnalysisResult.setPvalue( this.nan2Null( overallPValue ) );
                pvaluesForQvalue.get( factorName ).add( overallPValue );
                resultLists.get( factorName ).add( probeAnalysisResult );
            } // over terms

            if ( ++processed % 1000 == 0 ) {
                LinearModelAnalyzer.log.info( "Processed results for " + processed + " elements..." );
            }
        } // over probes

        this.fillRanksAndQvalues( resultLists, pvaluesForQvalue );

        DifferentialExpressionAnalysis expressionAnalysis = this
                .makeAnalysisEntity( bioAssaySet, config, label2Factors, baselineConditions, interceptFactor,
                        interactionFactorLists, oneSampleTTest, resultLists, subsetFactorValue );

        LinearModelAnalyzer.log.info( "Analysis processing phase done ..." );

        return expressionAnalysis;
    }

    private void warnForElement( CompositeSequence el, String s, int warned ) {
        if ( warned < 5 ) {
            log.warn( el.getName() + ": " + s );
        } else if ( warned == 5 ) {
            log.warn( el.getName() + ": " + s + "\nFurther warnings will be suppressed, enable debug logging for " + LinearModelAnalyzer.class.getName() + " for details." );
        } else {
            log.debug( el.getName() + ": " + s );
        }
    }

    private void dropIncompleteFactors( List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors ) {
        factors.removeIf( f -> {
            if ( ExperimentalDesignUtils.isComplete( f, samplesUsed ) ) {
                return false; // keep
            }
            String samplesWithMissingValues = ExperimentalDesignUtils.getSampleToFactorValuesMap( f, samplesUsed )
                    .entrySet().stream().filter( e -> e.getValue().isEmpty() )
                    .map( Map.Entry::getKey )
                    .map( BioMaterial::getName )
                    .sorted()
                    .collect( Collectors.joining( ", " ) );
            LinearModelAnalyzer.log.warn( "Dropping " + f + " due to missing values for samples: " + samplesWithMissingValues + "." );
            return true;
        } );
    }

    /**
     * Remove all configurations that have to do with factors that aren't in the selected factors.
     *
     * @param factors the factors that will be included
     * @return an updated config; the baselines are cleared; subset is cleared; interactions are only kept if
     * they only
     * involve the given factors.
     */
    private DifferentialExpressionAnalysisConfig fixConfigForSubset( List<ExperimentalFactor> factors,
            FactorValue subsetFactorValue, DifferentialExpressionAnalysisConfig config ) {
        DifferentialExpressionAnalysisConfig newConfig = new DifferentialExpressionAnalysisConfig();
        newConfig.addFactorsToInclude( factors );
        for ( Collection<ExperimentalFactor> interactors : config.getInteractionsToInclude() ) {
            if ( new HashSet<>( factors ).containsAll( interactors ) ) {
                newConfig.addInteractionToInclude( interactors );
            }
        }
        newConfig.setSubsetFactor( null );
        newConfig.setSubsetFactorValue( subsetFactorValue );
        return newConfig;
    }

    /**
     * Remove factors which are no longer usable, based on the subset.
     */
    private List<ExperimentalFactor> fixFactorsForSubset( ExpressionExperimentSubSet eesubSet, ExpressionDataDoubleMatrix dmatrix,
            List<ExperimentalFactor> factors ) {

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
                DiffExAnalyzerUtils.populateFactorValuesFromBASet( eesubSet, f, levels );

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
    private void fillRanksAndQvalues(
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

            double[] ranks = computeRanks( ArrayUtils.toPrimitive( pvalArray ) );

            if ( pvalArray.length != resultLists.get( fName ).size() )
                throw new AssertionError( pvalArray.length + " != " + resultLists.get( fName ).size() );

            if ( pvalArray.length != ranks.length ) throw new AssertionError();

            double[] qvalues = benjaminiHochberg( pvalArray );
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
            Map<ExperimentalFactor, FactorValue> baselineConditions, @Nullable ExperimentalFactor interceptFactor,
            List<String[]> interactionFactorLists, boolean oneSampleTtest,
            Map<String, ? extends Collection<DifferentialExpressionAnalysisResult>> resultLists,
            @Nullable FactorValue subsetFactorValue ) {

        DifferentialExpressionAnalysis expressionAnalysis = config.toAnalysis();
        expressionAnalysis.setExperimentAnalyzed( bioAssaySet );

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
            @Nullable List<String[]> interactionFactorLists, Map<ExperimentalFactor, FactorValue> baselineConditions ) {
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
            if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
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


    /**
     * Breakdown a data matrix into multiple matrices for each subset.
     */
    private Map<FactorValue, ExpressionDataDoubleMatrix> makeSubSetMatrices( ExpressionDataDoubleMatrix dmatrix, List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors, ExperimentalFactor subsetFactor ) {
        if ( subsetFactor.getType().equals( FactorType.CONTINUOUS ) ) {
            throw new IllegalArgumentException( "You cannot subset on a continuous factor (has a Measurement)" );
        }

        if ( factors.contains( subsetFactor ) ) {
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
            orderByExperimentalDesign( samplesInSubset, factors );
            ExpressionDataDoubleMatrix subMatrix = new ExpressionDataDoubleMatrix( dmatrix, samplesInSubset,
                    createBADMap( samplesInSubset ) );
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
        Future<Map<String, LinearModelSummary>> f = taskExecutor.submit( () -> {
            StopWatch timer = new StopWatch();
            timer.start();
            LeastSquaresFit fit;
            if ( config.isUseWeights() ) {
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
            if ( config.isModerateStatistics() ) {
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

        // this analysis should take just 10 or 20 seconds for most data sets.
        // but there are cases that take longer; addressing https://github.com/PavlidisLab/Gemma/issues/13
        // would help.
        long updateIntervalMillis = 60 * 1000; // 1 minute
        long maxAnalysisTimeMillis = config.getMaxAnalysisTimeMillis();
        while ( true ) {
            try {
                long s = updateIntervalMillis;
                if ( maxAnalysisTimeMillis > 0 ) {
                    s = Math.min( s, maxAnalysisTimeMillis );
                }
                Map<String, LinearModelSummary> rawResults = f.get( s, TimeUnit.MILLISECONDS );
                if ( timer.getTime() >= updateIntervalMillis ) {
                    LinearModelAnalyzer.log.info( String.format( "Analysis finished in %.1f minutes.",
                            timer.getTime( TimeUnit.SECONDS ) / 60.00 ) );
                }
                assert rawResults.size() == namedMatrix.rows() : "expected " + namedMatrix.rows() + " results, got " + rawResults.size();
                return rawResults;
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new RuntimeException( "Analysis was interrupted!", e );
            } catch ( ExecutionException e ) {
                throw new RuntimeException( e.getCause() );
            } catch ( TimeoutException e ) {
                if ( maxAnalysisTimeMillis > 0 && timer.getTime() >= maxAnalysisTimeMillis ) {
                    LinearModelAnalyzer.log.error( "Analysis is taking too long, something bad must have happened; cancelling..." );
                    f.cancel( true );
                    throw new RuntimeException( "Analysis was taking too long, it was cancelled" );
                } else {
                    LinearModelAnalyzer.log.info( String.format( "Analysis running, %.1f minutes elapsed...",
                            timer.getTime( TimeUnit.SECONDS ) / 60.00 ) );
                }
            }
        }
    }

    /**
     * @param pvalues pvalues
     * @return normalized ranks of the pvalues, or null if they were invalid/unusable.
     */
    private double[] computeRanks( double[] pvalues ) {
        Assert.isTrue( pvalues.length > 0, "P-values array cannot be empty." );
        DoubleArrayList ranks = Rank.rankTransform( new DoubleArrayList( pvalues ) );
        if ( ranks == null ) {
            throw new RuntimeException( "Pvalue ranks could not be computed" );
        }
        double[] normalizedRanks = new double[ranks.size()];
        for ( int i = 0; i < ranks.size(); i++ ) {
            normalizedRanks[i] = ranks.get( i ) / ranks.size();
        }
        return normalizedRanks;
    }

    /**
     * @param pvalues pvalues
     * @return Qvalues, or null if they could not be computed.
     */
    @Nullable
    private double[] benjaminiHochberg( Double[] pvalues ) {
        DoubleMatrix1D benjaminiHochberg = MultipleTestCorrection
                .benjaminiHochberg( new ubic.basecode.dataStructure.matrix.DenseDoubleMatrix1D( ArrayUtils.toPrimitive( pvalues ) ) );
        return benjaminiHochberg != null ? benjaminiHochberg.toArray() : null;
    }

    /**
     * Needed to convert NaN or infinity values to a value we can store in the database.
     * <p>
     * These values cannot be stored in a FLOAT column.
     */
    private Double nan2Null( @Nullable Double e ) {
        return e != null && Double.isFinite( e ) ? e : null;
    }

    /**
     * Determine if any factor should be treated as the intercept term.
     */
    private ExperimentalFactor determineInterceptFactor( Collection<ExperimentalFactor> factors,
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
}
