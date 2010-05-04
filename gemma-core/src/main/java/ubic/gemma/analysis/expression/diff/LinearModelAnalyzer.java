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

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.basecode.util.r.type.LinearModelSummary;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Handles fitting linear models with continuous or fixed-level covariates. Interactions can be included if a
 * DifferentialExpressionAnalysisConfig is passed as an argument to 'run'.
 * <p>
 * One factor can be constant (the same value for all samples); such a factor will be analyzed by looking at the
 * intercept in the fitted model. This is only appropriate for 'non-reference' designs on ratiometric arrays.
 * 
 * @author paul
 * @version $Id$
 */
public abstract class LinearModelAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    static Log log = LogFactory.getLog( LinearModelAnalyzer.class );

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment)
     */
    @Override
    public final DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment ) {
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
    public final DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
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
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {
        connectToR();

        assert rc != null;

        /*
         * Initialize our matrix and factor lists...
         */
        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisHelperService
                .getBioMaterialsForBioAssays( dmatrix );

        List<ExperimentalFactor> factors = config.getFactorsToInclude();

        if ( samplesUsed.size() <= factors.size() ) {
            throw new IllegalArgumentException( "Must have more samples than factors" );
        }

        /*
         * Figure out control groups, Reorder the matrix, samplesused also has to be reorganized. this puts the control
         * samples up front if possible.
         */
        samplesUsed = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesUsed, factors );
        dmatrix = new ExpressionDataDoubleMatrix( samplesUsed, dmatrix );

        Map<ExperimentalFactor, FactorValue> baselineConditions = ExpressionDataMatrixColumnSort
                .getBaselineLevels( factors );
        Collection<FactorValue> factorValuesOfFirstSample = samplesUsed.iterator().next().getFactorValues();
        for ( ExperimentalFactor factor : factors ) {
            if ( !baselineConditions.containsKey( factor ) ) {

                for ( FactorValue biomf : factorValuesOfFirstSample ) {
                    /*
                     * the first biomaterial has the values used as baseline in R.
                     */
                    if ( biomf.getExperimentalFactor().equals( factor ) ) {
                        log.debug( "Using default baseline for " + factor + ": " + biomf );
                        baselineConditions.put( factor, biomf );
                    }
                }
            }
        }

        /*
         * TODO make subsets if requested. ( we can do subsets with the model with x / y notation)
         */
        ExperimentalFactor subsetFactor = config.getSubsetFactor();

        if ( subsetFactor != null ) {
            if ( config.getFactorsToInclude().contains( subsetFactor ) ) {
                throw new IllegalArgumentException(
                        "You cannot analyze a factor and use it for subsetting at the same time." );
            }

            Map<FactorValue, List<BioMaterial>> subSetSamples = new HashMap<FactorValue, List<BioMaterial>>();
            for ( FactorValue fv : subsetFactor.getFactorValues() ) {
                if ( fv.getMeasurement() != null ) {
                    throw new IllegalArgumentException( "You cannot subset on a continuous factor (has a Measurement)" );
                }

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
                samplesInSubset = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( samplesInSubset, config
                        .getFactorsToInclude() );
                ExpressionDataDoubleMatrix subMatrix = new ExpressionDataDoubleMatrix( samplesUsed, dmatrix );
                subMatrices.put( fv, subMatrix );
            }

            /*
             * Now use the subMatrices - EESubSet?
             */
        }

        ExperimentalFactor interceptFactor = null;
        final Map<String, Collection<ExperimentalFactor>> label2Factors = new LinkedHashMap<String, Collection<ExperimentalFactor>>();
        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();

        for ( ExperimentalFactor experimentalFactor : factors ) {

            label2Factors.put( nameForR( experimentalFactor ), new HashSet<ExperimentalFactor>() );
            label2Factors.get( nameForR( experimentalFactor ) ).add( experimentalFactor );

            /*
             * Check if we need to treat the intercept as a fator.
             */
            interceptFactor = checkIfNeedToTreatAsIntercept( experimentalFactor, quantitationType, interceptFactor );

        }

        /*
         * Build our factor terms, with interactions handled specially.(TODO: subsets...)
         */
        List<String[]> interactionFactorLists = new ArrayList<String[]>();
        ObjectMatrix<String, String, Object> designMatrix = buildFactorsInR( factors, samplesUsed, baselineConditions );

        setupFactors( designMatrix, baselineConditions );

        String modelFormula = "";
        boolean oneSampleTtest = interceptFactor != null && factors.size() == 1;
        if ( oneSampleTtest ) {
            // special case of one-sample t-test.
            modelFormula = " ";
        } else {

            String factTerm = StringUtils.join( label2Factors.keySet(), "+" );

            boolean hasInteractionTerms = !config.getInteractionsToInclude().isEmpty();
            if ( hasInteractionTerms ) {
                for ( Collection<ExperimentalFactor> interactionTerms : config.getInteractionsToInclude() ) {

                    List<String> interactionFactorNames = new ArrayList<String>();
                    for ( ExperimentalFactor factor : interactionTerms ) {
                        interactionFactorNames.add( nameForR( factor ) ); // see above for naming convention.
                    }

                    factTerm = factTerm + " + " + StringUtils.join( interactionFactorNames, "*" ); // in the R statement
                    interactionFactorLists.add( interactionFactorNames.toArray( new String[] {} ) );

                    // In the ANOVA table.
                    String factTableLabel = StringUtils.join( interactionFactorNames, ":" );
                    label2Factors.put( factTableLabel, new HashSet<ExperimentalFactor>() );
                    label2Factors.get( factTableLabel ).addAll( interactionTerms );
                }
            }

            modelFormula = " ~ " + factTerm;
        }

        DoubleMatrix<DesignElement, Integer> namedMatrix = dmatrix.getMatrix();

        final Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );

        final Map<String, LinearModelSummary> rawResults = runAnalysis( namedMatrix, label2Factors, modelFormula,
                rowNameExtractor );

        if ( rawResults.size() == 0 ) {
            throw new IllegalStateException( "Got no results from the analysis" );
        }

        /*
         * Initialize some data structures we need to hold results
         */
        DifferentialExpressionAnalysis expressionAnalysis = super.initAnalysisEntity( expressionExperiment );
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
        for ( DesignElement el : namedMatrix.getRowNames() ) {

            CompositeSequence cs = ( CompositeSequence ) el;

            LinearModelSummary lm = rawResults.get( rowNameExtractor.transform( el ).toString() );

            if ( log.isDebugEnabled() ) log.debug( el.getName() + "\n" + lm );

            if ( lm == null ) {
                if ( !warned ) {
                    // FIXME this usuall y means we got nothing.
                    log.warn( "No result for " + el + ", further warnings suppressed" );
                    warned = true;
                }
                continue;
            }

            for ( String factorName : label2Factors.keySet() ) {

                if ( factorName.contains( ":" ) ) continue; // interaction, a bit ugly but this is the R way

                Double pvalue = null;
                Double score = null;

                Collection<ExperimentalFactor> factorsForName = label2Factors.get( factorName );
                if ( interceptFactor != null && factorsForName.size() == 1
                        && factorsForName.iterator().next().equals( interceptFactor ) ) {
                    pvalue = lm.getInterceptP();
                    score = lm.getInterceptT();
                } else {
                    pvalue = lm.getMainEffectP( factorName );
                    Double[] mainEffectT = lm.getMainEffectT( factorName );

                    if ( mainEffectT != null && mainEffectT.length > 0 ) score = mainEffectT[0]; // Note: there will be
                    // multiple values if
                    // there are more
                    // than 2 levels in a non-ordered factor.
                }

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                probeAnalysisResult.setPvalue( nan2Null( pvalue ) );

                // get a directionality on the score:
                probeAnalysisResult.setUpRegulated( score != null && score > 0 );
                probeAnalysisResult.setEffectSize( nan2Null( score ) );
                pvaluesForQvalue.get( factorName ).add( pvalue );

                resultLists.get( factorName ).add( probeAnalysisResult );

            }

            for ( String[] fa : interactionFactorLists ) {

                String intF = StringUtils.join( fa, ":" );
                Double interactionEffectP = lm.getInteractionEffectP( fa );

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                probeAnalysisResult.setPvalue( nan2Null( interactionEffectP ) );

                // FIXME interactions are not that straightforward.
                // Double interactionEffectT = lm.getInteractionEffectT( fa )[0]; there can be more than 1.
                // probeAnalysisResult.setUpRegulated( interactionEffectT > 0 );
                // probeAnalysisResult.setScore( nan2Null( interactionEffectT ) );

                pvaluesForQvalue.get( intF ).add( interactionEffectP );

                resultLists.get( intF ).add( probeAnalysisResult );
            }
        }

        /*
         * qvalues and ranks, requires second pass over the result objects.
         */
        for ( String fName : pvaluesForQvalue.keySet() ) {
            List<Double> pvals = pvaluesForQvalue.get( fName );

            double[] qvalues = super.getQValues( pvals.toArray( new Double[] {} ) );
            double[] ranks = super.computeRanks( ArrayUtils.toPrimitive( pvals.toArray( new Double[] {} ) ) );

            int i = 0;
            for ( DifferentialExpressionAnalysisResult pr : resultLists.get( fName ) ) {
                pr.setCorrectedPvalue( nan2Null( qvalues[i] ) );
                pr.setRank( nan2Null( ranks[i] ) );
                i++;
            }
        }

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

            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance( baselineGroup,
                    expressionAnalysis, resultLists.get( fName ), factorsUsed );
            resultSets.add( resultSet );

        }

        /*
         * Complete analysis config
         */
        expressionAnalysis.setResultSets( resultSets );
        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( "Linear model with " + config.getFactorsToInclude().size() + " factors"
                + ( interceptFactor == null ? "" : " with intercept treated as factor" )
                + ( interactionFactorLists.isEmpty() ? "" : " with interaction" ) );

        disconnectR();
        return expressionAnalysis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.model.expression.experiment.ExperimentalFactor[])
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
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
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            ExperimentalFactor subsetFactor, Collection<ExperimentalFactor> factors ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setFactorsToInclude( factors );

        config.setSubsetFactor( subsetFactor );

        return this.run( expressionExperiment, config );
    }

    /**
     * @param experimentalFactor
     * @param quantitationType
     * @param interceptFactor existing value for interceptFactor
     * @return
     */
    protected ExperimentalFactor checkIfNeedToTreatAsIntercept( ExperimentalFactor experimentalFactor,
            QuantitationType quantitationType, ExperimentalFactor interceptFactor ) {
        if ( experimentalFactor.getFactorValues().size() == 1 ) {
            if ( interceptFactor != null ) {
                throw new IllegalArgumentException( "Cannot deal with more than one constant factor" );
            }

            if ( quantitationType.getIsRatio() ) {
                interceptFactor = experimentalFactor;
            } else {
                throw new IllegalArgumentException(
                        "Cannot deal with constant factors unless the data are ratiometric non-reference design" );
            }
        }
        return interceptFactor;
    }

    /**
     * @param factor
     * @return
     */
    protected Boolean isContinuous( ExperimentalFactor factor ) {
        for ( FactorValue fv : factor.getFactorValues() ) {
            if ( fv.getMeasurement() != null ) {
                return true;
            }
        }
        return false;
    }

    protected String nameForR( ExperimentalFactor experimentalFactor ) {
        return "fact." + experimentalFactor.getId();
    }

    protected String nameForR( FactorValue fv, boolean isBaseline ) {
        return "fv_" + fv.getId() + ( isBaseline ? "_base" : "" );
    }

    /**
     * Convert factors to a matrix usable in R.
     * 
     * @param factors
     * @param samplesUsed
     * @param factors in the order they will be used
     * @param factorNames
     * @param baselines
     * @return a design matrix
     */
    protected ObjectMatrix<String, String, Object> buildFactorsInR( List<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, Map<ExperimentalFactor, FactorValue> baselines ) {

        ObjectMatrix<String, String, Object> designMatrix = new ObjectMatrixImpl<String, String, Object>( samplesUsed
                .size(), factors.size() );

        Map<ExperimentalFactor, String> factorNamesInR = new LinkedHashMap<ExperimentalFactor, String>();

        Map<ExperimentalFactor, Boolean> isContinuous = new HashMap<ExperimentalFactor, Boolean>();
        for ( ExperimentalFactor factor : factors ) {
            factorNamesInR.put( factor, nameForR( factor ) );
            isContinuous.put( factor, false );
        }

        designMatrix.setColumnNames( new ArrayList<String>( factorNamesInR.values() ) );

        List<String> rowNames = new ArrayList<String>();

        int row = 0;
        for ( BioMaterial samp : samplesUsed ) {

            rowNames.add( "biomat_" + samp.getId() );

            int col = 0;
            for ( ExperimentalFactor factor : factors ) {

                FactorValue baseLineFV = baselines.get( factor );

                /*
                 * Find this biomaterial's value for the current factor.
                 */
                Object value = null;
                boolean found = false;
                for ( FactorValue fv : samp.getFactorValues() ) {

                    if ( fv.getExperimentalFactor().equals( factor ) ) {

                        if ( found ) {
                            // not unique
                            throw new IllegalStateException( "Biomaterial had more than one value for factor: "
                                    + factor );
                        }

                        boolean isBaseline = baseLineFV != null && fv.equals( baseLineFV );

                        Measurement measurement = fv.getMeasurement();
                        if ( measurement != null ) {

                            isContinuous.put( factor, true );

                            try {
                                value = Double.parseDouble( measurement.getValue() );
                            } catch ( NumberFormatException e ) {
                                log.warn( "Failed to parse measurement as number: " + measurement.getValue() );
                                value = Double.NaN;
                            }
                        } else {
                            /*
                             * We always use a dummy value. It's not as human-readable but at least we're sure it is
                             * unique and R-compliant. (assuming the fv is persistent!)
                             */
                            value = nameForR( fv, isBaseline );
                        }
                        found = true;
                        // could break here but nice to check for uniqueness.
                    }
                }
                if ( !found ) {
                    throw new IllegalStateException( "Biomaterial did not have a matching factor value for: " + factor );
                }

                designMatrix.set( row, col, value );

                col++;

            }
            row++;

        }

        designMatrix.setRowNames( rowNames );
        return designMatrix;
    }

    /**
     * Important bit. Run the analysis via R
     * 
     * @param namedMatrix
     * @param factorNameMap
     * @param modelFormula
     * @param rowNameExtractor
     * @return results
     */
    private Map<String, LinearModelSummary> runAnalysis( final DoubleMatrix<DesignElement, Integer> namedMatrix,
            final Map<String, Collection<ExperimentalFactor>> factorNameMap, final String modelFormula,
            final Transformer rowNameExtractor ) {

        final Map<String, LinearModelSummary> rawResults = new ConcurrentHashMap<String, LinearModelSummary>();

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
     * Assigns the design matrix columns as factors, defines contrasts
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

            String factorName = nameForR( factor );
            Object[] column = designMatrix.getColumn( designMatrix.getColIndexByName( factorName ) );

            if ( isContinuous( factor ) ) {
                double[] colD = new double[column.length];
                for ( int i = 0; i < column.length; i++ ) {
                    colD[i] = ( Double ) column[i];
                }
                rc.assign( factorName, colD );
            } else {
                String[] colS = new String[column.length];
                for ( int i = 0; i < column.length; i++ ) {
                    colS[i] = ( String ) column[i];
                }

                FactorValue baseLineFV = baselineConditions.get( factor );

                String fvName = nameForR( baseLineFV, true );

                rc.assignFactor( factorName, Arrays.asList( colS ) );
                List<String> stringListEval = rc.stringListEval( ( "levels(" + factorName + ")" ) );

                int indexOfBaseline = stringListEval.indexOf( fvName ) + 1; // R is 1-based.

                rc.voidEval( "contrasts(" + factorName + ")<-contr.treatment(levels(" + factorName + "), base="
                        + indexOfBaseline + ")" );
            }
        }
    }
}
