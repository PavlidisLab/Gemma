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
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
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
public abstract class LinearModelAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    private static Log log = LogFactory.getLog( LinearModelAnalyzer.class );

    /**
     * Threshold below which contrasts will be stored for a given Result.
     */
    private static final double PVALUE_CONTRAST_SELECT_THRESHOLD = ConfigUtils
            .getDouble( "gemma.linearmodels.pvaluethresh" );

    private static final boolean USE_R = ConfigUtils.getBoolean( "gemma.linearmodels.useR" );

    /**
     * Determine if any factor should be treated as the intercept term.
     * 
     * @param factors
     * @param quantitationType
     * @return
     */
    public ExperimentalFactor determineInterceptFactor( Collection<ExperimentalFactor> factors,
            QuantitationType quantitationType ) {
        ExperimentalFactor interceptFactor = null;
        for ( ExperimentalFactor experimentalFactor : factors ) {

            /*
             * Check if we need to treat the intercept as a fator.
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment)
     */
    @Override
    public final Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment ) {
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
    public final Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
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

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( Arrays.asList( experimentalFactors ) );

        return this.run( expressionExperiment, config );

    }

    /**
     * @param expressionExperiment
     * @param subsetFactor
     * @param factors
     * @return
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExperimentalFactor subsetFactor, Collection<ExperimentalFactor> factors ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setFactorsToInclude( factors );

        config.setSubsetFactor( subsetFactor );

        return this.run( expressionExperiment, config );
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

            /*
             * Initialize our matrix and factor lists...
             */
            if ( USE_R ) {
                connectToR();
            }
            List<ExperimentalFactor> factors = config.getFactorsToInclude();

            List<BioMaterial> samplesUsed = ExperimentalDesignUtils.getOrderedSamples( dmatrix, factors );

            dmatrix = new ExpressionDataDoubleMatrix( samplesUsed, dmatrix );

            /*
             * Do the analysis, by subsets if requested
             */
            Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();

            ExperimentalFactor subsetFactor = config.getSubsetFactor();
            if ( subsetFactor != null ) {

                if ( factors.contains( subsetFactor ) ) {
                    throw new IllegalArgumentException( "Subset factor cannot also be included in the analysis" );
                }

                Map<FactorValue, ExpressionDataDoubleMatrix> subsets = makeSubSets( config, dmatrix, samplesUsed,
                        subsetFactor );

                /*
                 * Now analyze each subset
                 */
                for ( FactorValue subsetFactorValue : subsets.keySet() ) {

                    log.info( "Analyzing subset: " + subsetFactorValue );

                    List<BioMaterial> bioMaterials = ExperimentalDesignUtils.getOrderedSamples( subsets
                            .get( subsetFactorValue ), factors );

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

                    /*
                     * Run analysis on the subset.
                     */
                    DifferentialExpressionAnalysis analysis = doAnalysis( eesubSet, config, subsets
                            .get( subsetFactorValue ), bioMaterials, factors, subsetFactorValue );

                    results.add( analysis );

                }

            } else {

                /*
                 * Analyze the whole thing as one
                 */
                DifferentialExpressionAnalysis analysis = doAnalysis( expressionExperiment, config, dmatrix,
                        samplesUsed, factors, null );
                results.add( analysis );
            }
            return results;

        } finally {
            disconnectR();
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
     * @param bioAssaySet
     * @param config
     * @param dmatrix
     * @param samplesUsed
     * @param factors
     * @param subsetFactorValue
     * @return
     */
    private DifferentialExpressionAnalysis doAnalysis( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config, ExpressionDataDoubleMatrix dmatrix,
            List<BioMaterial> samplesUsed, List<ExperimentalFactor> factors, FactorValue subsetFactorValue ) {
        if ( samplesUsed.size() <= factors.size() ) {
            throw new IllegalArgumentException( "Must have more samples than factors" );
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
        ObjectMatrix<String, String, Object> designMatrix = ExperimentalDesignUtils.buildFactorsForR( factors,
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
        if ( !onLogScale( quantitationType, namedMatrix ) ) {
            MatrixStats.logTransform( namedMatrix );
        }

        if ( log.isDebugEnabled() ) outputForDebugging( dmatrix, designMatrix );

        final Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );

        /*
         * PREPARATION FOR 'NATIVE' FITTING
         */
        DoubleMatrix<String, String> sNamedMatrix = makeDataMatrix( designMatrix, namedMatrix );
        DesignMatrix properDesignMatrix = makeDesignMatrix( designMatrix, interactionFactorLists, baselineConditions );

        /*
         * Run the analysis NOTE this can be simplified if we strip out R code.
         */
        final Map<String, LinearModelSummary> rawResults = runAnalysis( namedMatrix, sNamedMatrix, label2Factors,
                modelFormula, rowNameExtractor, properDesignMatrix, interceptFactor, interactionFactorLists,
                baselineConditions );

        if ( rawResults.size() == 0 ) {
            throw new IllegalStateException( "Got no results from the analysis" );
        }

        /*
         * Initialize data structures we need to hold results
         */

        Map<String, List<DifferentialExpressionAnalysisResult>> resultLists = new HashMap<String, List<DifferentialExpressionAnalysisResult>>();
        Map<String, List<Double>> pvaluesForQvalue = new HashMap<String, List<Double>>();
        for ( String factorName : label2Factors.keySet() ) {
            resultLists.put( factorName, new ArrayList<DifferentialExpressionAnalysisResult>() );
            pvaluesForQvalue.put( factorName, new ArrayList<Double>() );
        }
        for ( String[] fs : interactionFactorLists ) {
            String intF = StringUtils.join( fs, ":" );
            resultLists.put( intF, new ArrayList<DifferentialExpressionAnalysisResult>() );
            pvaluesForQvalue.put( intF, new ArrayList<Double>() );
        }

        /*
         * Create result objects for each model fit. Keeping things in order is important.
         */
        boolean warned = false;
        for ( CompositeSequence el : namedMatrix.getRowNames() ) {

            if ( el.getName().equals( "probe_0" ) ) {
                log.info( "MY GOODNESS" );
            }

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
                        throw new UnsupportedOperationException(
                                "Handling more than two-way interactions is not implemented" );
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
     * @param resultLists
     * @param pvaluesForQvalue
     */
    private void getRanksAndQvalues( Map<String, List<DifferentialExpressionAnalysisResult>> resultLists,
            Map<String, List<Double>> pvaluesForQvalue ) {
        /*
         * qvalues and ranks, requires second pass over the result objects.
         */
        for ( String fName : pvaluesForQvalue.keySet() ) {
            List<Double> pvals = pvaluesForQvalue.get( fName );

            Double[] pvalArray = pvals.toArray( new Double[] {} );
            for ( int i = 0; i < pvalArray.length; i++ ) {
                if ( pvalArray[i] == null ) pvalArray[i] = Double.NaN;
            }

            // savePvaluesForDebugging( ArrayUtils.toPrimitive( pvalArray ) );
            // double[] qvalues = super.getQValues( pvalArray );
            double[] qvalues = super.benjaminiHochberg( pvalArray );
            double[] ranks = super.computeRanks( ArrayUtils.toPrimitive( pvalArray ) );

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
        /*
         * FIXME careful, tricky case of one-sided test not handled yet? Actually seems okay ...
         */
        if ( !interactionFactorLists.isEmpty() ) {
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
        /*
         * Result sets
         */
        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        for ( String fName : resultLists.keySet() ) {
            Collection<ExperimentalFactor> factorsUsed = new HashSet<ExperimentalFactor>();
            factorsUsed.addAll( label2Factors.get( fName ) );

            FactorValue baselineGroup = null;
            if ( !oneSampleTtest && factorsUsed.size() == 1 /* interaction */) {
                baselineGroup = baselineConditions.get( factorsUsed.iterator().next() );
            }

            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance( factorsUsed,
                    baselineGroup, expressionAnalysis, resultLists.get( fName ), null /* hit list sizes */);
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
            for ( FactorValue fv : sample.getFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( subsetFactor ) ) {
                    subSetSamples.get( fv ).add( sample );
                }
            }
        }

        Map<FactorValue, ExpressionDataDoubleMatrix> subMatrices = new HashMap<FactorValue, ExpressionDataDoubleMatrix>();
        for ( FactorValue fv : subSetSamples.keySet() ) {
            List<BioMaterial> samplesInSubset = subSetSamples.get( fv );
            assert samplesInSubset.size() < samplesUsed.size();
            samplesInSubset = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesInSubset, config
                    .getFactorsToInclude() );
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
    private boolean onLogScale( QuantitationType quantitationType,
            DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix ) {
        if ( quantitationType.getScale() != null ) {
            if ( quantitationType.getScale().equals( ScaleType.LOG2 ) ) {
                return true;
            } else if ( quantitationType.getScale().equals( ScaleType.LOG10 ) ) {
                // pretty unlikely
                throw new UnsupportedOperationException( "Sorry, data on log-10 scale is not supported yet" );
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
                    return false;
                }
            }
        }

        log.debug( "Data look log tranformed, not sure about base" );
        return true;

    }

    /**
     * Important bit. Run the analysis via R
     * 
     * @param namedMatrix
     * @param factorNameMap
     * @param modelFormula
     * @param rowNameExtractor
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
            final Transformer rowNameExtractor, DesignMatrix designMatrix, ExperimentalFactor interceptFactor,
            List<String[]> interactionFactorLists, Map<ExperimentalFactor, FactorValue> baselineConditions ) {

        final Map<String, LinearModelSummary> rawResults = new ConcurrentHashMap<String, LinearModelSummary>();

        Future<?> f;
        if ( USE_R ) {
            f = runAnalysisFuture( namedMatrix, factorNameMap, modelFormula, rowNameExtractor, rawResults );
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
                    log
                            .info( String.format( "Analysis running, %.1f minutes elapsed ...",
                                    timer.getTime() / 60000.00 ) );
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
     * @param rowNameExtractor
     * @param rawResults
     * @return
     */
    private Future<?> runAnalysisFuture( final DoubleMatrix<CompositeSequence, BioMaterial> namedMatrix,
            final Map<String, Collection<ExperimentalFactor>> factorNameMap, final String modelFormula,
            final Transformer rowNameExtractor, final Map<String, LinearModelSummary> rawResults ) {

        if ( rc == null || !rc.isConnected() ) {
            throw new IllegalStateException( "Don't call this method unless R is ready :(" );
        }

        final String matrixName = rc.assignMatrix( namedMatrix, rowNameExtractor );
        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<?> f = service.submit( new Runnable() {
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
    @SuppressWarnings("unchecked")
    protected void outputForDebugging( ExpressionDataDoubleMatrix dmatrix,
            ObjectMatrix<String, String, Object> designMatrix ) {
        MatrixWriter<Double> mw = new MatrixWriter<Double>();
        try {
            mw.write( new FileWriter( File.createTempFile( "data.", ".txt" ) ), dmatrix, null, true, false );
            ubic.basecode.io.writer.MatrixWriter dem = new ubic.basecode.io.writer.MatrixWriter( new FileWriter( File
                    .createTempFile( "design.", ".txt" ) ) );
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
}
