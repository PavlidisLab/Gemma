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
package ubic.gemma.analysis.expression.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.math.DesignMatrix;
import ubic.basecode.math.LeastSquaresFit;
import ubic.basecode.math.MatrixStats;
import ubic.basecode.util.r.type.LinearModelSummary;
import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.analysis.Direction;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;

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
 * 
 * @author paul
 * @version $Id$
 */
@Component
@Scope(value = "prototype")
public class LinearModelAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    private static Log log = LogFactory.getLog( LinearModelAnalyzer.class );

    /**
     * Threshold below which contrasts will be stored for a given Result.
     */
    private static final double PVALUE_CONTRAST_SELECT_THRESHOLD = ConfigUtils
            .getDouble( "gemma.linearmodels.pvaluethresh" );

    private static final boolean USE_R = ConfigUtils.getBoolean( "gemma.linearmodels.useR" );

    /**
     * Preset levels for which we will store the HitListSizes.
     */
    private static final double[] qValueThresholdsForHitLists = new double[] { 0.001, 0.005, 0.01, 0.05, 0.1 };

    /**
     * Determine if any factor should be treated as the intercept term.
     * 
     * @param factors
     * @param quantitationType
     * @return
     */
    @Override
    public ExperimentalFactor determineInterceptFactor( Collection<ExperimentalFactor> factors,
            QuantitationType quantitationType ) {
        ExperimentalFactor interceptFactor = null;
        for ( ExperimentalFactor experimentalFactor : factors ) {

            /*
             * Check if we need to treat the intercept as a factor.
             */
            boolean useI = checkIfNeedToTreatAsIntercept( experimentalFactor, quantitationType );

            if ( useI && interceptFactor != null ) {
                throw new IllegalStateException( "Can only deal with one constant factor (intercept)" );
            } else if ( useI ) {
                interceptFactor = experimentalFactor;
            }
        }
        return interceptFactor;
    }

    /**
     * @param matrix on which to perform regression.
     * @param config containing configuration of factors to include. Any interactions or subset configuration is
     *        ignored. Data are <em>NOT</em> log transformed unless they come in that way.
     * @param retainScale if true, the data retain the global mean (intercept)
     * @return residuals from the regression.
     */
    @Override
    public ExpressionDataDoubleMatrix regressionResiduals( ExpressionDataDoubleMatrix matrix,
            DifferentialExpressionAnalysisConfig config, boolean retainScale ) {

        if ( config.getFactorsToInclude().isEmpty() ) {
            log.warn( "No factors" );
            return matrix; // FIXME perhaps return a copy instead. OR throw an ex.
        }

        /*
         * Note that this method relies on similar code to doAnalysis, for the setup stages.
         */

        List<ExperimentalFactor> factors = config.getFactorsToInclude();

        List<BioMaterial> samplesUsed = ExperimentalDesignUtils.getOrderedSamples( matrix, factors );

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils.getBaselineConditions(
                samplesUsed, factors );

        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils.buildDesignMatrix( factors,
                samplesUsed, baselineConditions );

        DesignMatrix properDesignMatrix = new DesignMatrix( designMatrix, true );

        ExpressionDataDoubleMatrix dmatrix = new ExpressionDataDoubleMatrix( samplesUsed, matrix );
        DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix = dmatrix.getMatrix();

        DoubleMatrix<String, String> sNamedMatrix = makeDataMatrix( designMatrix, namedMatrix );
        LeastSquaresFit fit = new LeastSquaresFit( properDesignMatrix, sNamedMatrix );

        DoubleMatrix2D residuals = fit.getResiduals();

        if ( retainScale ) {
            DoubleMatrix1D intercept = fit.getCoefficients().viewRow( 0 );
            for ( int i = 0; i < residuals.rows(); i++ ) {
                residuals.viewRow( i ).assign( Functions.plus( intercept.get( i ) ) );
            }
        }

        DoubleMatrix<CompositeSequence, BioMaterial> f = new DenseDoubleMatrix<CompositeSequence, BioMaterial>(
                residuals.toArray() );
        f.setRowNames( dmatrix.getMatrix().getRowNames() );
        f.setColumnNames( dmatrix.getMatrix().getColNames() );
        return new ExpressionDataDoubleMatrix( dmatrix, f );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment ) {
        return run( expressionExperiment, expressionExperiment.getExperimentalDesign().getExperimentalFactors() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.util.Collection)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( factors );

        return this.run( expressionExperiment, config );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        return run( expressionExperiment, dmatrix, config );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.model.expression.experiment.ExperimentalFactor[])
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExperimentalFactor... experimentalFactors ) {

        return this.run( expressionExperiment, Arrays.asList( experimentalFactors ) );

    }

    /**
     * @param expressionExperiment
     * @param dmatrix
     * @param config
     * @return
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config ) {
        try {

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

            if ( USE_R ) {
                connectToR();
            }

            /*
             * Initialize our matrix and factor lists...
             */
            List<ExperimentalFactor> factors = config.getFactorsToInclude();

            List<BioMaterial> samplesUsed = ExperimentalDesignUtils.getOrderedSamples( dmatrix, factors );

            dmatrix = new ExpressionDataDoubleMatrix( samplesUsed, dmatrix ); // enforce ordering

            /*
             * Do the analysis, by subsets if requested
             */
            Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();

            ExperimentalFactor subsetFactor = config.getSubsetFactor();
            if ( subsetFactor != null ) {

                if ( factors.contains( subsetFactor ) ) {
                    throw new IllegalStateException(
                            "Subset factor cannot also be included in the analysis [ Factor was: " + subsetFactor + "]" );
                }

                Map<FactorValue, ExpressionDataDoubleMatrix> subsets = makeSubSets( config, dmatrix, samplesUsed,
                        subsetFactor );

                /*
                 * Now analyze each subset
                 */
                for ( FactorValue subsetFactorValue : subsets.keySet() ) {

                    log.info( "Analyzing subset: " + subsetFactorValue );

                    List<BioMaterial> bioMaterials = ExperimentalDesignUtils.getOrderedSamples(
                            subsets.get( subsetFactorValue ), factors );

                    /*
                     * make a EESubSet
                     */
                    ExpressionExperimentSubSet eesubSet = ExpressionExperimentSubSet.Factory.newInstance();
                    eesubSet.setSourceExperiment( expressionExperiment );
                    eesubSet.setName( "Subset for " + subsetFactorValue );
                    Collection<BioAssay> bioAssays = new HashSet<BioAssay>();
                    for ( BioMaterial bm : bioMaterials ) {
                        bioAssays.addAll( bm.getBioAssaysUsedIn() );
                    }
                    eesubSet.getBioAssays().addAll( bioAssays );

                    Collection<ExperimentalFactor> subsetFactors = fixFactorsForSubset(
                            subsets.get( subsetFactorValue ), eesubSet, factors );

                    DifferentialExpressionAnalysisConfig subsetConfig = fixConfigForSubset( factors, config );

                    if ( subsetFactors.isEmpty() ) {
                        log.warn( "Experimental design is not valid for subset: " + subsetFactorValue + "; skipping" );
                        continue;
                    }

                    /*
                     * Run analysis on the subset.
                     */
                    DifferentialExpressionAnalysis analysis = doAnalysis( eesubSet, subsetConfig,
                            subsets.get( subsetFactorValue ), bioMaterials, factors, subsetFactorValue );

                    if ( analysis == null ) {
                        log.warn( "No analysis results were obtained for subset: " + subsetFactorValue );
                        continue;
                    }

                    results.add( analysis );

                }

            } else {

                /*
                 * Analyze the whole thing as one
                 */
                DifferentialExpressionAnalysis analysis = doAnalysis( expressionExperiment, config, dmatrix,
                        samplesUsed, factors, null );
                if ( analysis == null ) {
                    log.warn( "No analysis results were obtained" );
                } else {
                    results.add( analysis );
                }
            }
            return results;

        } finally {
            disconnectR();
        }
    }

    /**
     * Remove factors which are no longer usable, based on the subset.
     * 
     * @param expressionDataDoubleMatrix
     * @param eesubSet
     * @param factors
     * @return
     */
    private Collection<ExperimentalFactor> fixFactorsForSubset( ExpressionDataDoubleMatrix dmatrix,
            ExpressionExperimentSubSet eesubSet, List<ExperimentalFactor> factors ) {

        List<ExperimentalFactor> result = new ArrayList<ExperimentalFactor>();

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();
        ExperimentalFactor interceptFactor = determineInterceptFactor( factors, quantitationType );

        /*
         * Remove any constant factors, unless they are that intercept.
         */
        for ( ExperimentalFactor f : factors ) {

            if ( f.getType().equals( FactorType.CONTINUOUS ) ) {
                result.add( f );
            } else if ( interceptFactor != null && interceptFactor.equals( f ) ) {
                result.add( f );
            } else {

                Collection<FactorValue> levels = new HashSet<FactorValue>();
                for ( BioAssay ba : eesubSet.getBioAssays() ) {
                    for ( BioMaterial bm : ba.getSamplesUsed() ) {
                        for ( FactorValue fv : bm.getFactorValues() ) {
                            if ( fv.getExperimentalFactor().equals( f ) ) {
                                levels.add( fv );
                            }
                        }
                    }
                }

                if ( levels.size() > 1 ) {
                    result.add( f );
                }

            }

        }

        return result;

    }

    /**
     * Remove all configurations that have to do with factors that aren't in the selected factors
     * 
     * @param factors the factors that will be incluced
     * @param config
     * @return an updated config; the baselines are cleared; subset is cleared; interactions are only kept if they only
     *         involve the given factors.
     */
    private DifferentialExpressionAnalysisConfig fixConfigForSubset( List<ExperimentalFactor> factors,
            DifferentialExpressionAnalysisConfig config ) {

        DifferentialExpressionAnalysisConfig newConfig = new DifferentialExpressionAnalysisConfig();

        newConfig.setBaseLineFactorValues( null );

        if ( !config.getInteractionsToInclude().isEmpty() ) {
            Collection<Collection<ExperimentalFactor>> newInteractionsToInclude = new HashSet<Collection<ExperimentalFactor>>();
            for ( Collection<ExperimentalFactor> interactors : config.getInteractionsToInclude() ) {
                if ( factors.containsAll( interactors ) ) {
                    newInteractionsToInclude.add( interactors );
                }
            }

            newConfig.setInteractionsToInclude( newInteractionsToInclude );
        }

        newConfig.setSubsetFactor( null );
        newConfig.setFactorsToInclude( factors );

        return newConfig;

    }

    /**
     * @param experimentalFactor
     * @param quantitationType
     * @return boolean true if we need the intercept.
     */
    protected boolean checkIfNeedToTreatAsIntercept( ExperimentalFactor experimentalFactor,
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
     * 
     * @param dmatrix
     * @param designMatrix
     */
    protected void outputForDebugging( ExpressionDataDoubleMatrix dmatrix,
            ObjectMatrix<String, String, Object> designMatrix ) {
        MatrixWriter<Double> mw = new MatrixWriter<Double>();
        try {
            mw.write( new FileWriter( File.createTempFile( "data.", ".txt" ) ), dmatrix, null, true, false );
            ubic.basecode.io.writer.MatrixWriter<String, String> dem = new ubic.basecode.io.writer.MatrixWriter<String, String>(
                    new FileWriter( File.createTempFile( "design.", ".txt" ) ) );
            dem.writeMatrix( designMatrix, true );

        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Assigns the design matrix columns as factors, defines contrasts. We use treatment contrasts, comparing to a
     * baseline condition.
     * 
     * @param designMatrix
     * @param baselineConditions
     */
    protected void setupFactors( ObjectMatrix<String, String, Object> designMatrix,
            Map<ExperimentalFactor, FactorValue> baselineConditions ) {

        for ( ExperimentalFactor factor : baselineConditions.keySet() ) {

            if ( factor.getFactorValues().size() < 2 ) {
                continue;
            }

            String factorName = ExperimentalDesignUtils.nameForR( factor );
            Object[] column = designMatrix.getColumn( designMatrix.getColIndexByName( factorName ) );

            if ( ExperimentalDesignUtils.isContinuous( factor ) ) {
                double[] colD = new double[column.length];
                for ( int i = 0; i < column.length; i++ ) {
                    colD[i] = ( Double ) column[i];
                }
                if ( USE_R ) rc.assign( factorName, colD );
            } else {
                String[] colS = new String[column.length];
                for ( int i = 0; i < column.length; i++ ) {
                    colS[i] = ( String ) column[i];
                }

                FactorValue baseLineFV = baselineConditions.get( factor );

                String baselineFvName = ExperimentalDesignUtils.nameForR( baseLineFV, true );

                if ( USE_R ) {
                    rc.assignFactor( factorName, Arrays.asList( colS ) );
                    List<String> stringListEval = rc.stringListEval( ( "levels(" + factorName + ")" ) );

                    /*
                     * The 'base' is the index of the baseline group in the list of levels: the result of
                     * 'levels(factor)'. Default base is 1.
                     */

                    int indexOfBaseline = stringListEval.indexOf( baselineFvName ) + 1; // R is 1-based.

                    rc.voidEval( "contrasts(" + factorName + ")<-contr.treatment(levels(" + factorName + "), base="
                            + indexOfBaseline + ")" );
                }
            }
        }
    }

    /**
     * Build the formula, omitting the factor taking the place of the intercept, if need be. (Mostly for R but with side
     * effect of populating the interactionFactorLists and label2Factors)
     * 
     * @param config
     * @param label2Factors
     * @param interceptFactor
     * @param interactionFactorLists
     * @return
     */
    private String buildModelFormula( final DifferentialExpressionAnalysisConfig config,
            final Map<String, Collection<ExperimentalFactor>> label2Factors, final ExperimentalFactor interceptFactor,
            final List<String[]> interactionFactorLists ) {
        String modelFormula;

        String factTerm = "";
        for ( String nameInR : label2Factors.keySet() ) {
            if ( interceptFactor != null && label2Factors.get( nameInR ).size() == 1
                    && label2Factors.get( nameInR ).iterator().next().equals( interceptFactor ) ) {
                continue;
            }
            factTerm = factTerm + " " + nameInR + " +";
        }
        factTerm = factTerm.replaceFirst( "\\+$", "" );

        /*
         * Add interaction terms
         */
        boolean hasInteractionTerms = !config.getInteractionsToInclude().isEmpty();
        if ( hasInteractionTerms ) {
            for ( Collection<ExperimentalFactor> interactionTerms : config.getInteractionsToInclude() ) {

                List<String> interactionFactorNames = new ArrayList<String>();
                for ( ExperimentalFactor factor : interactionTerms ) {

                    interactionFactorNames.add( ExperimentalDesignUtils.nameForR( factor ) );
                }

                factTerm = factTerm + " + " + StringUtils.join( interactionFactorNames, "*" ); // in the R
                // statement

                interactionFactorLists.add( interactionFactorNames.toArray( new String[] {} ) );

                // In the ANOVA table.
                String factTableLabel = StringUtils.join( interactionFactorNames, ":" );
                label2Factors.put( factTableLabel, new HashSet<ExperimentalFactor>() );
                label2Factors.get( factTableLabel ).addAll( interactionTerms );
            }
        }

        modelFormula = " ~ " + factTerm;
        return modelFormula;
    }

    /**
     * Generate HitListSize entities that will be stored to count the number of diff. ex probes at various preset
     * thresholds, to avoid wasting time generating these counts on the fly later.
     * 
     * @param results
     * @return
     */
    private Collection<HitListSize> computeHitListSizes( List<DifferentialExpressionAnalysisResult> results,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {
        Collection<HitListSize> hitListSizes = new HashSet<HitListSize>();

        assert probeToGeneMap != null;

        // maps from Doubles are a bit dodgy...
        Map<Double, Integer> upCounts = new HashMap<Double, Integer>();
        Map<Double, Integer> downCounts = new HashMap<Double, Integer>();
        Map<Double, Integer> eitherCounts = new HashMap<Double, Integer>();

        Map<Double, Integer> upCountGenes = new HashMap<Double, Integer>();
        Map<Double, Integer> downCountGenes = new HashMap<Double, Integer>();
        Map<Double, Integer> eitherCountGenes = new HashMap<Double, Integer>();

        for ( DifferentialExpressionAnalysisResult r : results ) {

            Double corrP = r.getCorrectedPvalue();
            if ( corrP == null ) continue;

            int numGenes = 0;
            if ( r instanceof ProbeAnalysisResult ) {
                CompositeSequence probe = ( ( ProbeAnalysisResult ) r ).getProbe();
                if ( probeToGeneMap.containsKey( probe ) ) {
                    Collection<Gene> genes = probeToGeneMap.get( probe );
                    numGenes = genes.size();
                }
            }

            Collection<ContrastResult> crs = r.getContrasts();
            boolean up = false;
            boolean down = false;
            for ( ContrastResult cr : crs ) {
                Double lf = cr.getLogFoldChange();
                if ( lf < 0 ) {
                    down = true;
                } else if ( lf > 0 ) {
                    up = true;
                }
            }

            for ( double thresh : qValueThresholdsForHitLists ) {

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

        for ( double thresh : qValueThresholdsForHitLists ) {

            // Ensure we don't set values to null.
            Integer up = upCounts.get( thresh ) == null ? 0 : upCounts.get( thresh );
            Integer down = downCounts.get( thresh ) == null ? 0 : downCounts.get( thresh );
            Integer either = eitherCounts.get( thresh ) == null ? 0 : eitherCounts.get( thresh );

            Integer upGenes = upCountGenes.get( thresh ) == null ? 0 : upCountGenes.get( thresh );
            Integer downGenes = downCountGenes.get( thresh ) == null ? 0 : downCountGenes.get( thresh );
            Integer eitherGenes = eitherCountGenes.get( thresh ) == null ? 0 : eitherCountGenes.get( thresh );

            HitListSize upS = HitListSize.Factory.newInstance( thresh, up, Direction.UP, upGenes );
            HitListSize downS = HitListSize.Factory.newInstance( thresh, down, Direction.DOWN, downGenes );
            HitListSize eitherS = HitListSize.Factory.newInstance( thresh, either, Direction.EITHER, eitherGenes );

            hitListSizes.add( upS );
            hitListSizes.add( downS );
            hitListSizes.add( eitherS );
        }
        return hitListSizes;
    }

    /**
     * @param bioAssaySet source data
     * @param config
     * @param dmatrix data
     * @param samplesUsed analyzed
     * @param factors included in the model
     * @param subsetFactorValue null unless analyzing a subset (only used for book-keeping)
     * @return analysis, or null if there was a problem.
     */
    private DifferentialExpressionAnalysis doAnalysis( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config, ExpressionDataDoubleMatrix dmatrix,
            List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors, FactorValue subsetFactorValue ) {

        if ( factors.isEmpty() ) {
            log.error( "Must provide at least one factor" );
            return null;
        }

        if ( samplesUsed.size() <= factors.size() ) {
            log.error( "Must have more samples than factors" );
            return null;
        }

        final Map<String, Collection<ExperimentalFactor>> label2Factors = getRNames( factors );

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils.getBaselineConditions(
                samplesUsed, factors );

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();

        ExperimentalFactor interceptFactor = determineInterceptFactor( factors, quantitationType );

        /*
         * Build our factor terms, with interactions handled specially
         */
        List<String[]> interactionFactorLists = new ArrayList<String[]>();
        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils.buildDesignMatrix( factors,
                samplesUsed, baselineConditions );

        setupFactors( designMatrix, baselineConditions );

        String modelFormula = "";
        boolean oneSampleTtest = interceptFactor != null && factors.size() == 1;
        if ( oneSampleTtest ) {
            modelFormula = " ";
        } else {
            modelFormula = buildModelFormula( config, label2Factors, interceptFactor, interactionFactorLists );
        }

        DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix = dmatrix.getMatrix();

        /*
         * Log transform, if necessary
         */

        ScaleType scaleType = findScale( quantitationType, namedMatrix );

        if ( scaleType.equals( ScaleType.LOG2 ) ) {
            log.info( "Data is already on a log scale" );
        } else if ( scaleType.equals( ScaleType.LN ) ) {
            log.info( "Converting from ln to log2" );
            MatrixStats.convertToLog2( namedMatrix, Math.E );
        } else if ( scaleType.equals( ScaleType.LOG10 ) ) {
            log.info( "Converting from log10 to log2" );
            MatrixStats.convertToLog2( namedMatrix, 10 );
        } else if ( scaleType.equals( ScaleType.LINEAR ) ) {
            log.info( " **** LOG TRANSFORMING **** " );
            MatrixStats.logTransform( namedMatrix );
        } else {
            throw new UnsupportedOperationException( "Can't figure out what scale the data are on" );
        }

        if ( log.isDebugEnabled() ) outputForDebugging( dmatrix, designMatrix );

        /*
         * PREPARATION FOR 'NATIVE' FITTING
         */
        DoubleMatrix<String, String> sNamedMatrix = makeDataMatrix( designMatrix, namedMatrix );
        DesignMatrix properDesignMatrix = makeDesignMatrix( designMatrix, interactionFactorLists, baselineConditions );

        /*
         * Run the analysis NOTE this can be simplified if we strip out R code.
         */
        final Map<String, LinearModelSummary> rawResults = runAnalysis( namedMatrix, sNamedMatrix, label2Factors,
                modelFormula, properDesignMatrix, interceptFactor, interactionFactorLists, baselineConditions );

        if ( rawResults.size() == 0 ) {
            log.error( "Got no results from the analysis" );
            return null;
        }

        /*
         * Initialize data structures we need to hold results
         */

        Map<String, List<DifferentialExpressionAnalysisResult>> resultLists = new HashMap<String, List<DifferentialExpressionAnalysisResult>>();
        Map<String, List<Double>> pvaluesForQvalue = new HashMap<String, List<Double>>();
        for ( String factorName : label2Factors.keySet() ) {
            if ( properDesignMatrix.getDroppedFactors().contains( factorName ) ) {
                continue;
            }
            resultLists.put( factorName, new ArrayList<DifferentialExpressionAnalysisResult>() );
            pvaluesForQvalue.put( factorName, new ArrayList<Double>() );
        }
        addinteraction: for ( String[] fs : interactionFactorLists ) {
            for ( String f : fs ) {
                if ( properDesignMatrix.getDroppedFactors().contains( f ) ) {
                    continue addinteraction;
                }
            }
            String intF = StringUtils.join( fs, ":" );
            resultLists.put( intF, new ArrayList<DifferentialExpressionAnalysisResult>() );
            pvaluesForQvalue.put( intF, new ArrayList<Double>() );
        }

        if ( pvaluesForQvalue.isEmpty() ) {
            log.warn( "No results were obtained for the current stage of analysis, possibly due to dropped factors." );
            return null;
        }

        /*
         * Create result objects for each model fit. Keeping things in order is important.
         */
        final Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );
        boolean warned = false;
        for ( CompositeSequence el : namedMatrix.getRowNames() ) {

            LinearModelSummary lm = rawResults.get( rowNameExtractor.transform( el ).toString() );

            if ( log.isDebugEnabled() ) log.debug( el.getName() + "\n" + lm );

            if ( lm == null ) {
                if ( !warned ) {
                    log.warn( "No result for " + el + ", further warnings suppressed" );
                    warned = true;
                }
                continue;
            }

            for ( String factorName : label2Factors.keySet() ) {

                if ( !pvaluesForQvalue.containsKey( factorName ) ) {
                    // was dropped.
                    continue;
                }

                Double overallPValue = null;
                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( el );
                probeAnalysisResult.setQuantitationType( quantitationType );

                if ( lm.getCoefficients() == null ) {
                    probeAnalysisResult.setPvalue( null );
                    pvaluesForQvalue.get( factorName ).add( overallPValue );
                    resultLists.get( factorName ).add( probeAnalysisResult );
                    continue;
                }

                Collection<ExperimentalFactor> factorsForName = label2Factors.get( factorName );

                if ( factorsForName.size() > 1 ) {
                    /*
                     * Interactions
                     */
                    if ( factorsForName.size() > 2 ) {
                        log.error( "Handling more than two-way interactions is not implemented" );
                        return null;
                    }

                    assert factorName.contains( ":" );
                    String[] factorNames = StringUtils.split( factorName, ":" );
                    assert factorNames.length == factorsForName.size();
                    overallPValue = lm.getInteractionEffectP( factorNames );

                    if ( overallPValue != null && overallPValue <= PVALUE_CONTRAST_SELECT_THRESHOLD ) {

                        Map<String, Double> interactionContrastTStats = lm.getContrastTStats( factorName );
                        Map<String, Double> interactionContrastCoeffs = lm.getContrastCoefficients( factorName );
                        Map<String, Double> interactionContrastPValues = lm.getContrastPValues( factorName );

                        for ( String term : interactionContrastPValues.keySet() ) {
                            Double contrastPvalue = interactionContrastPValues.get( term );

                            if ( contrastPvalue <= PVALUE_CONTRAST_SELECT_THRESHOLD ) {
                                makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                        interactionContrastTStats, interactionContrastCoeffs );
                            }
                        }
                    }

                } else {

                    /*
                     * Main effect
                     */

                    assert factorsForName.size() == 1;
                    ExperimentalFactor experimentalFactor = factorsForName.iterator().next(); // we know there is only
                    // one.

                    /*
                     * Determine critical F statistic
                     */
                    // double
                    // FDistribution f = new FDistributionImpl(/);

                    if ( interceptFactor != null && factorsForName.size() == 1
                            && experimentalFactor.equals( interceptFactor ) ) {
                        overallPValue = lm.getInterceptP();
                    } else {
                        overallPValue = lm.getMainEffectP( factorName );
                    }

                    if ( overallPValue < PVALUE_CONTRAST_SELECT_THRESHOLD ) {

                        /*
                         * Add contrasts, one for each FactorValue which is "significant."
                         */

                        Map<String, Double> mainEffectContrastTStats = lm.getContrastTStats( factorName );

                        Map<String, Double> mainEffectContrastPvalues = lm.getContrastPValues( factorName );
                        Map<String, Double> mainEffectContrastCoeffs = lm.getContrastCoefficients( factorName );

                        for ( String term : mainEffectContrastPvalues.keySet() ) {

                            /*
                             * TODO Idea from limma: retain contrast if the f statistic is significant if you set all
                             * larger t stats to the same value as this one
                             */

                            Double contrastPvalue = mainEffectContrastPvalues.get( term );

                            if ( contrastPvalue < PVALUE_CONTRAST_SELECT_THRESHOLD ) {
                                makeContrast( probeAnalysisResult, factorsForName, term, factorName, contrastPvalue,
                                        mainEffectContrastTStats, mainEffectContrastCoeffs );
                            }
                        }
                    }

                }

                probeAnalysisResult.setPvalue( nan2Null( overallPValue ) );
                pvaluesForQvalue.get( factorName ).add( overallPValue );
                resultLists.get( factorName ).add( probeAnalysisResult );
            }

        }

        getRanksAndQvalues( resultLists, pvaluesForQvalue );

        DifferentialExpressionAnalysis expressionAnalysis = makeAnalysisEntity( bioAssaySet, config, label2Factors,
                baselineConditions, interceptFactor, interactionFactorLists, oneSampleTtest, resultLists,
                subsetFactorValue );
        return expressionAnalysis;
    }

    /**
     * @param resultList
     * @param probeToGeneMap
     * @return
     */
    private int getNumberOfGenesTested( List<DifferentialExpressionAnalysisResult> resultList,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {

        Collection<Gene> gs = new HashSet<Gene>();
        for ( DifferentialExpressionAnalysisResult d : resultList ) {
            CompositeSequence probe = ( ( ProbeAnalysisResult ) d ).getProbe();
            if ( probeToGeneMap.containsKey( probe ) ) {
                gs.addAll( probeToGeneMap.get( probe ) );
            }
        }
        return gs.size();
    }

    /**
     * Needed to compute the number of genes tested/detected.
     * 
     * @param resultLists
     * @return
     */
    private Map<CompositeSequence, Collection<Gene>> getProbeToGeneMap(
            Map<String, List<DifferentialExpressionAnalysisResult>> resultLists ) {
        Map<CompositeSequence, Collection<Gene>> result = new HashMap<CompositeSequence, Collection<Gene>>();

        for ( List<DifferentialExpressionAnalysisResult> resultList : resultLists.values() ) {
            for ( DifferentialExpressionAnalysisResult d : resultList ) {
                CompositeSequence probe = ( ( ProbeAnalysisResult ) d ).getProbe();
                result.put( probe, new HashSet<Gene>() );
            }
        }

        assert !result.isEmpty();
        return compositeSequenceService.getGenes( result.keySet() );

    }

    /**
     * Fill in the ranks and qvalues in the results.
     * 
     * @param resultLists
     * @param pvaluesForQvalue Map of factorName to results.
     */
    private void getRanksAndQvalues( Map<String, List<DifferentialExpressionAnalysisResult>> resultLists,
            Map<String, List<Double>> pvaluesForQvalue ) {
        /*
         * qvalues and ranks, requires second pass over the result objects.
         */
        for ( String fName : pvaluesForQvalue.keySet() ) {
            List<Double> pvals = pvaluesForQvalue.get( fName );

            if ( pvals.isEmpty() ) {
                log.warn( "No pvalues for " + fName + ", ignoring." );
                continue;
            }

            Double[] pvalArray = pvals.toArray( new Double[] {} );
            for ( int i = 0; i < pvalArray.length; i++ ) {
                if ( pvalArray[i] == null ) pvalArray[i] = Double.NaN;
            }

            double[] ranks = super.computeRanks( ArrayUtils.toPrimitive( pvalArray ) );

            if ( ranks == null ) {
                log.error( "Ranks could not be computed " + fName );
                // savePvaluesForDebugging( ArrayUtils.toPrimitive( pvalArray ) );
                continue;
            }

            double[] qvalues = super.benjaminiHochberg( pvalArray );

            if ( qvalues == null ) {
                log.warn( "Corrected pvalues could not be computed for " + fName );
                continue;
            }

            int i = 0;
            for ( DifferentialExpressionAnalysisResult pr : resultLists.get( fName ) ) {
                pr.setCorrectedPvalue( nan2Null( qvalues[i] ) );
                pr.setRank( nan2Null( ranks[i] ) );
                i++;
            }
        }
    }

    /**
     * @param factors
     * @return
     */
    private Map<String, Collection<ExperimentalFactor>> getRNames( List<ExperimentalFactor> factors ) {
        final Map<String, Collection<ExperimentalFactor>> label2Factors = new LinkedHashMap<String, Collection<ExperimentalFactor>>();
        for ( ExperimentalFactor experimentalFactor : factors ) {
            label2Factors.put( ExperimentalDesignUtils.nameForR( experimentalFactor ),
                    new HashSet<ExperimentalFactor>() );
            label2Factors.get( ExperimentalDesignUtils.nameForR( experimentalFactor ) ).add( experimentalFactor );
        }
        return label2Factors;
    }

    /**
     * @param bioAssaySet
     * @param config
     * @param label2Factors
     * @param baselineConditions
     * @param interceptFactor
     * @param interactionFactorLists
     * @param oneSampleTtest
     * @param resultLists
     * @param subsetFactorValue
     * @return Analysis (nonpersistent)
     */
    private DifferentialExpressionAnalysis makeAnalysisEntity( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config,
            final Map<String, Collection<ExperimentalFactor>> label2Factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions, ExperimentalFactor interceptFactor,
            List<String[]> interactionFactorLists, boolean oneSampleTtest,
            Map<String, List<DifferentialExpressionAnalysisResult>> resultLists, FactorValue subsetFactorValue ) {

        DifferentialExpressionAnalysis expressionAnalysis = super.initAnalysisEntity( bioAssaySet );

        Collection<ExpressionAnalysisResultSet> resultSets = makeResultSets( label2Factors, baselineConditions,
                oneSampleTtest, expressionAnalysis, resultLists );

        /*
         * Complete analysis config
         */
        expressionAnalysis.setResultSets( resultSets );
        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( "Linear model with "
                + config.getFactorsToInclude().size()
                + " factors"
                + ( interceptFactor == null ? "" : " with intercept treated as factor" )
                + ( interactionFactorLists.isEmpty() ? "" : " with interaction" )
                + ( subsetFactorValue == null ? "" : "Using subset " + bioAssaySet + " subset value= "
                        + subsetFactorValue ) );
        expressionAnalysis.setSubsetFactorValue( subsetFactorValue );

        return expressionAnalysis;
    }

    /**
     * Add a contrast to the given result.
     * 
     * @param probeAnalysisResult
     * @param experimentalFactor
     * @param term
     * @param factorName
     * @param contrastPvalue
     * @param tstats
     * @param coeffs
     */
    private void makeContrast( ProbeAnalysisResult probeAnalysisResult,
            Collection<ExperimentalFactor> experimentalFactors, String term, String factorName, Double contrastPvalue,
            Map<String, Double> tstats, Map<String, Double> coeffs ) {

        assert experimentalFactors.size() == 1 || experimentalFactors.size() == 2;

        Double contrastTstat = tstats.get( term );
        Double coefficient = coeffs.get( term );
        ContrastResult contrast = ContrastResult.Factory.newInstance();
        contrast.setPvalue( nan2Null( contrastPvalue ) );
        contrast.setTstat( nan2Null( contrastTstat ) );
        contrast.setCoefficient( nan2Null( coefficient ) );

        List<ExperimentalFactor> factorList = new ArrayList<ExperimentalFactor>( experimentalFactors );
        boolean isInteraction = false;
        if ( factorList.size() == 2 ) {
            isInteraction = true;
        }

        /*
         * The coefficient can be treated as fold-change if the data are log-transformed. This is because the
         * coefficient in the contrast is the (fitted;estimated) difference between the means, and log(x) - log(y) =
         * log(x/y). Limma uses this same trick.
         */
        contrast.setLogFoldChange( nan2Null( coefficient ) );

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

            Long factorValueId = null;

            try {
                factorValueId = Long.parseLong( firstTerm.replace( factorNames[0]
                        + ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX, "" ) );
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

            if ( isInteraction ) {
                log.debug( "Interaction term" );
                assert secondTerm != null;

                try {
                    factorValueId = Long.parseLong( secondTerm.replace( factorNames[1]
                            + ExperimentalDesignUtils.FACTOR_VALUE_RNAME_PREFIX, "" ) );
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

            if ( contrast.getSecondFactorValue() != null
                    && contrast.getSecondFactorValue().equals( contrast.getFactorValue() ) ) {
                throw new IllegalStateException(
                        "Contrast for interactions must be for two different factor values, got the same one twice" );
            }
        }

        probeAnalysisResult.getContrasts().add( contrast );

    }

    /**
     * @param designMatrix
     * @param namedMatrix
     * @return
     */
    private DoubleMatrix<String, String> makeDataMatrix( ObjectMatrix<String, String, Object> designMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {
        /*
         * Convert the data into a string-keyed matrix.
         */
        DoubleMatrix<String, String> sNamedMatrix = new DenseDoubleMatrix<String, String>( namedMatrix.asArray() );
        for ( int i = 0; i < namedMatrix.rows(); i++ ) {
            sNamedMatrix.addRowName( namedMatrix.getRowName( i ).getId().toString() );
        }
        sNamedMatrix.setColumnNames( designMatrix.getRowNames() );
        return sNamedMatrix;
    }

    /**
     * @param designMatrix
     * @param interactionFactorLists
     * @param baselineConditions
     * @return
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
            properDesignMatrix.setBaseline( factorName, baselineFactorValue );
        }
        return properDesignMatrix;
    }

    /**
     * @param label2Factors
     * @param baselineConditions
     * @param oneSampleTtest
     * @param expressionAnalysis
     * @param resultLists
     * @return
     */
    private Collection<ExpressionAnalysisResultSet> makeResultSets(
            final Map<String, Collection<ExperimentalFactor>> label2Factors,
            Map<ExperimentalFactor, FactorValue> baselineConditions, boolean oneSampleTtest,
            DifferentialExpressionAnalysis expressionAnalysis,
            Map<String, List<DifferentialExpressionAnalysisResult>> resultLists ) {

        Map<CompositeSequence, Collection<Gene>> probeToGeneMap = getProbeToGeneMap( resultLists );

        /*
         * Result sets
         */
        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        for ( String fName : resultLists.keySet() ) {
            List<DifferentialExpressionAnalysisResult> results = resultLists.get( fName );

            Collection<ExperimentalFactor> factorsUsed = new HashSet<ExperimentalFactor>();
            factorsUsed.addAll( label2Factors.get( fName ) );

            FactorValue baselineGroup = null;
            if ( !oneSampleTtest && factorsUsed.size() == 1 /* interaction */) {
                baselineGroup = baselineConditions.get( factorsUsed.iterator().next() );
            }

            Collection<HitListSize> hitListSizes = computeHitListSizes( results, probeToGeneMap );

            int numberOfProbesTested = results.size();

            int numberOfGenesTested = getNumberOfGenesTested( results, probeToGeneMap );

            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory
                    .newInstance( factorsUsed, numberOfProbesTested, numberOfGenesTested, baselineGroup,
                            expressionAnalysis, results, hitListSizes );
            resultSets.add( resultSet );

        }
        return resultSets;
    }

    /**
     * @param config
     * @param dmatrix
     * @param samplesUsed
     * @param subsetFactor
     * @return
     */
    private Map<FactorValue, ExpressionDataDoubleMatrix> makeSubSets( DifferentialExpressionAnalysisConfig config,
            ExpressionDataDoubleMatrix dmatrix, List<BioMaterial> samplesUsed, ExperimentalFactor subsetFactor ) {
        if ( subsetFactor.getType().equals( FactorType.CONTINUOUS ) ) {
            throw new IllegalArgumentException( "You cannot subset on a continuous factor (has a Measurement)" );
        }

        if ( config.getFactorsToInclude().contains( subsetFactor ) ) {
            throw new IllegalArgumentException(
                    "You cannot analyze a factor and use it for subsetting at the same time." );
        }

        Map<FactorValue, List<BioMaterial>> subSetSamples = new HashMap<FactorValue, List<BioMaterial>>();
        for ( FactorValue fv : subsetFactor.getFactorValues() ) {
            assert fv.getMeasurement() == null;
            subSetSamples.put( fv, new ArrayList<BioMaterial>() );
        }

        for ( BioMaterial sample : samplesUsed ) {
            boolean ok = false;
            for ( FactorValue fv : sample.getFactorValues() ) {
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

        Map<FactorValue, ExpressionDataDoubleMatrix> subMatrices = new HashMap<FactorValue, ExpressionDataDoubleMatrix>();
        for ( FactorValue fv : subSetSamples.keySet() ) {
            List<BioMaterial> samplesInSubset = subSetSamples.get( fv );

            if ( samplesInSubset.isEmpty() ) {
                throw new IllegalArgumentException( "The subset was empty for fv: " + fv );
            }
            assert samplesInSubset.size() < samplesUsed.size();

            samplesInSubset = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesInSubset,
                    config.getFactorsToInclude() );
            ExpressionDataDoubleMatrix subMatrix = new ExpressionDataDoubleMatrix( samplesInSubset, dmatrix );
            subMatrices.put( fv, subMatrix );
        }

        return subMatrices;

    }

    /**
     * @param quantitationType
     * @param namedMatrix
     * @return
     * @see ExpressionExperimentFilter for a related implementation.
     */
    private ScaleType findScale( QuantitationType quantitationType,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {

        if ( quantitationType.getScale() != null ) {
            if ( quantitationType.getScale().equals( ScaleType.LOG2 ) ) {
                return ScaleType.LOG2;
            } else if ( quantitationType.getScale().equals( ScaleType.LOG10 ) ) {
                return ScaleType.LOG10;
            } else if ( quantitationType.getScale().equals( ScaleType.LN ) ) {
                return ScaleType.LN;
            } else if ( quantitationType.getScale().equals( ScaleType.LOGBASEUNKNOWN ) ) {
                throw new UnsupportedOperationException(
                        "Sorry, data on an unknown log scale is not supported. Please check the quantitation types, and make sure the data is expressed in terms of log2 or un-logged data" );
            }
        }

        for ( int i = 0; i < namedMatrix.rows(); i++ ) {
            for ( int j = 0; j < namedMatrix.columns(); j++ ) {
                double v = namedMatrix.get( i, j );
                if ( v > 20 ) {
                    log.debug( "Data has large values, doesn't look log transformed" );
                    return ScaleType.LINEAR;
                }
            }
        }

        throw new UnsupportedOperationException( "Data look log tranformed, not sure about base" );

    }

    /**
     * Important bit. Run the analysis via R
     * 
     * @param namedMatrix
     * @param factorNameMap
     * @param modelFormula
     * @param interactionFactorLists
     * @param interceptFactor
     * @param designMatrix
     * @param baselineConditions
     * @return results
     */
    private Map<String, LinearModelSummary> runAnalysis(
            final DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix,
            final DoubleMatrix<String, String> sNamedMatrix,
            final Map<String, Collection<ExperimentalFactor>> factorNameMap, final String modelFormula,
            DesignMatrix designMatrix, ExperimentalFactor interceptFactor, List<String[]> interactionFactorLists,
            Map<ExperimentalFactor, FactorValue> baselineConditions ) {

        final Map<String, LinearModelSummary> rawResults = new ConcurrentHashMap<String, LinearModelSummary>();

        Future<?> f;
        if ( USE_R ) {
            // Note: this will probably no longer work if we provide constant factors (e.g., in invalid subset
            // situations)
            f = runAnalysisFuture( namedMatrix, factorNameMap, modelFormula, rawResults );
        } else {
            f = runAnalysisFutureJ( designMatrix, sNamedMatrix, rawResults );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        long lasttime = 0;

        double updateIntervalMillis = 60000.00;
        while ( !f.isDone() ) {
            try {
                Thread.sleep( 1000 );

                if ( timer.getTime() - lasttime > updateIntervalMillis ) {
                    log.info( String.format( "Analysis running, %.1f minutes elapsed ...", timer.getTime() / 60000.00 ) );
                    lasttime = timer.getTime();
                }

            } catch ( InterruptedException e ) {
                log.warn( "Analysis interrupted!" );
                return rawResults;
            }
        }

        if ( timer.getTime() > updateIntervalMillis ) {
            log.info( String.format( "Analysis finished in %.1f minutes.", timer.getTime() / 60000.00 ) );
        }

        try {
            f.get();
        } catch ( InterruptedException e ) {
            log.warn( "Job was interrupted" );
            return rawResults;
        } catch ( ExecutionException e ) {
            throw new RuntimeException( e );
        }

        assert rawResults.size() == namedMatrix.rows() : "expected " + namedMatrix.rows() + " results, got "
                + rawResults.size();
        return rawResults;
    }

    /**
     * @param namedMatrix
     * @param factorNameMap
     * @param modelFormula
     * @param rawResults
     * @return
     */
    private Future<?> runAnalysisFuture( final DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix,
            final Map<String, Collection<ExperimentalFactor>> factorNameMap, final String modelFormula,
            final Map<String, LinearModelSummary> rawResults ) {

        if ( rc == null || !rc.isConnected() ) {
            throw new IllegalStateException( "Don't call this method unless R is ready :(" );
        }
        final Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );

        final String matrixName = rc.assignMatrix( namedMatrix, rowNameExtractor );
        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<?> f = service.submit( new Runnable() {
            @Override
            public void run() {
                Map<String, LinearModelSummary> res = rc.rowApplyLinearModel( matrixName, modelFormula, factorNameMap
                        .keySet().toArray( new String[] {} ) );
                rawResults.putAll( res );

            }
        } );

        service.shutdown();
        return f;
    }

    /**
     * Linear models solved using native Java implementation
     * 
     * @param designMatrix
     * @param data
     * @param rawResults
     * @return
     */
    private Future<?> runAnalysisFutureJ( final DesignMatrix designMatrix, final DoubleMatrix<String, String> data,
            final Map<String, LinearModelSummary> rawResults ) {
        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<?> f = service.submit( new Runnable() {
            @Override
            public void run() {
                /*
                 * This part should be straightforward
                 */
                StopWatch timer = new StopWatch();
                timer.start();
                LeastSquaresFit fit = new LeastSquaresFit( designMatrix, data );
                log.info( "Model fit data matrix " + data.rows() + " x " + data.columns() + ": " + timer.getTime()
                        + "ms" );
                timer.reset();
                timer.start();
                Map<String, LinearModelSummary> res = fit.summarizeByKeys( true );
                log.info( "Model summarize/ANOVA: " + timer.getTime() + "ms" );
                rawResults.putAll( res );
            }
        } );

        service.shutdown();
        return f;
    }
}
