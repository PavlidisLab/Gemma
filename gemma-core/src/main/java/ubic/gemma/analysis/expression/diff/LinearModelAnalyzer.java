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

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrixImpl;
import ubic.basecode.util.r.type.LinearModelSummary;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
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

    private static Log log = LogFactory.getLog( LinearModelAnalyzer.class );

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
     * .ExpressionExperiment, ubic.gemma.model.expression.experiment.ExperimentalFactor[])
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            ExperimentalFactor... experimentalFactors ) {

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( Arrays.asList( experimentalFactors ) );

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

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();

        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisHelperService
                .getBioMaterialsForBioAssays( dmatrix );

        Collection<ExperimentalFactor> factors = config.getFactorsToInclude();

        if ( samplesUsed.size() <= factors.size() ) {
            throw new IllegalArgumentException( "Must have more samples than factors" );
        }

        /*
         * if possible use this to order the samples to get effect size - treatment contrasts.
         */
        // FactorValue controlGroup = determineControlGroup( factorValues );
        /*
         * Assign the matrix in R.
         */
        DoubleMatrix<DesignElement, Integer> namedMatrix = dmatrix.getMatrix();

        /*
         * We need a list of factors...
         */
        List<ExperimentalFactor> factorList = new ArrayList<ExperimentalFactor>();
        factorList.addAll( factors );

        /*
         * Create names we can use to refer to our factors in a predictable way, and see if we need the intercept.
         */
        ExperimentalFactor interceptFactor = null;
        final Map<String, Collection<ExperimentalFactor>> factorNameMap = new LinkedHashMap<String, Collection<ExperimentalFactor>>();
        List<String> factorNames = new ArrayList<String>();
        for ( ExperimentalFactor experimentalFactor : factorList ) {
            String factName = "fact." + experimentalFactor.getId();
            factorNameMap.put( factName, new HashSet<ExperimentalFactor>() );
            factorNameMap.get( factName ).add( experimentalFactor );
            factorNames.add( factName );

            /*
             * Do we need to treat the intercept as a factor?
             */
            if ( experimentalFactor.getFactorValues().size() == 1 ) {
                if ( quantitationType.getIsRatio() ) {
                    interceptFactor = experimentalFactor;
                } else {
                    throw new IllegalArgumentException(
                            "Cannot deal with constant factors unless the data are ratiometric non-reference design" );
                }
            }

        }

        /*
         * Build our factor terms, with interactions handled specially.
         */
        List<String[]> interactionFactorLists = new ArrayList<String[]>();
        ObjectMatrix<String, String, Object> designMatrix = buildDesignMatrix( factors, samplesUsed, factorList,
                factorNames );
        String designMatrixVar = rc.dataFrame( designMatrix );

        String modelFormula = "";
        if ( interceptFactor != null && factorList.size() == 1 ) {
            // special case of one-sample t-test.
            modelFormula = "x ~ 1 ,data=" + designMatrixVar;
        } else {

            String factTerm = StringUtils.join( factorNameMap.keySet(), "+" );

            boolean hasInteractionTerms = !config.getInteractionsToInclude().isEmpty();
            if ( hasInteractionTerms ) {
                for ( Collection<ExperimentalFactor> interactionTerms : config.getInteractionsToInclude() ) {

                    List<String> interactionFactorNames = new ArrayList<String>();
                    for ( ExperimentalFactor factor : interactionTerms ) {
                        interactionFactorNames.add( "fact." + factor.getId() ); // see above for naming convention.
                    }

                    factTerm = factTerm + " + " + StringUtils.join( interactionFactorNames, "*" ); // in the R statement
                    interactionFactorLists.add( interactionFactorNames.toArray( new String[] {} ) );

                    // In the coefficients table.
                    String factTableLabel = StringUtils.join( interactionFactorNames, ":" );
                    factorNameMap.put( factTableLabel, new HashSet<ExperimentalFactor>() );
                    factorNameMap.get( factTableLabel ).addAll( interactionTerms );
                }
            }

            modelFormula = "x ~ " + factTerm + ",data=" + designMatrixVar;
        }

        if ( log.isDebugEnabled() ) {
            log.debug( namedMatrix );
            log.debug( designMatrix );
            log.debug( modelFormula );
        }

        final Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );

        final Map<String, LinearModelSummary> rawResults = runAnalysis( namedMatrix, factorNameMap, modelFormula,
                rowNameExtractor );

        /*
         * Initialize some data structures we need to hold results
         */
        DifferentialExpressionAnalysis expressionAnalysis = super.initAnalysisEntity( expressionExperiment );
        Map<String, List<DifferentialExpressionAnalysisResult>> resultLists = new HashMap<String, List<DifferentialExpressionAnalysisResult>>();
        Map<String, List<Double>> pvaluesForQvalue = new HashMap<String, List<Double>>();
        for ( String factorName : factorNameMap.keySet() ) {
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
        for ( DesignElement el : namedMatrix.getRowNames() ) {

            CompositeSequence cs = ( CompositeSequence ) el;

            LinearModelSummary lm = rawResults.get( rowNameExtractor.transform( el ).toString() );

            assert lm != null;

            for ( String factorName : factorNameMap.keySet() ) {

                if ( factorName.contains( ":" ) ) continue; // interaction - FIXME make this cleaner.

                Double pvalue = null;
                Double score = null;

                Collection<ExperimentalFactor> factorsForName = factorNameMap.get( factorName );
                if ( interceptFactor != null && factorsForName.size() == 1
                        && factorsForName.iterator().next().equals( interceptFactor ) ) {
                    pvalue = lm.getInterceptP();
                    score = lm.getInterceptT();
                } else {
                    pvalue = lm.getMainEffectP( factorName );
                    score = lm.getMainEffectT( factorName )[0]; // FIXME!!!! There can be multiple values.
                }

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                probeAnalysisResult.setPvalue( nan2Null( pvalue ) );

                // TODO get a directionality on the score:
                // probeAnalysisResult.setUpRegulated( score > 0 );
                probeAnalysisResult.setScore( nan2Null( score ) );
                pvaluesForQvalue.get( factorName ).add( pvalue );

                resultLists.get( factorName ).add( probeAnalysisResult );

            }

            for ( String[] fa : interactionFactorLists ) {

                String intF = StringUtils.join( fa, ":" );
                Double interactionEffectP = lm.getInteractionEffectP( fa );

                // FIXME Double interactionEffectT = lm.getInteractionEffectT( fa )[0]; // ??? multiple ??

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                probeAnalysisResult.setPvalue( nan2Null( interactionEffectP ) );

                // TODO get a directionality on the score:
                // probeAnalysisResult.setUpRegulated( score > 0 );
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
                pr.setCorrectedPvalue( qvalues[i] );
                pr.setRank( ranks[i] );
                i++;
            }
        }

        /*
         * Result sets
         */
        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        for ( String fName : resultLists.keySet() ) {
            Collection<ExperimentalFactor> factorsUsed = new HashSet<ExperimentalFactor>();
            factorsUsed.addAll( factorNameMap.get( fName ) );
            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance(
                    expressionAnalysis, resultLists.get( fName ), factorsUsed );
            resultSets.add( resultSet );

        }

        /*
         * Complete analysis config
         */
        expressionAnalysis.setResultSets( resultSets );
        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( "Linear model with " + factorNameMap.size() + " factors"
                + ( interceptFactor == null ? "" : " with intercept treated as factor" )
                + ( interactionFactorLists.isEmpty() ? "" : " with interaction" ) );

        disconnectR();
        return expressionAnalysis;
    }

    /**
     * Important bit. Actually run the analysis.
     * 
     * @param namedMatrix
     * @param factorNameMap
     * @param modelFormula
     * @param rowNameExtractor
     * @return results
     */
    private Map<String, LinearModelSummary> runAnalysis( DoubleMatrix<DesignElement, Integer> namedMatrix,
            final Map<String, Collection<ExperimentalFactor>> factorNameMap, String modelFormula,
            final Transformer rowNameExtractor ) {
        // important part.
        // This can be parallelized very easily in principle but R is not threadsafe. this would have to
        // be done via snow?
        // int numThreads = 8;
        // int rowsPerThread = ( int ) Math.ceil( namedMatrix.rows() / numThreads );
        // int j = 0;
        // int k = j + rowsPerThread;
        final Map<String, LinearModelSummary> rawResults = new ConcurrentHashMap<String, LinearModelSummary>();
        // ExecutorService service = Executors.newFixedThreadPool( numThreads );
        // final String modelFormulaF = modelFormula;
        // Collection<Future<?>> futures = new HashSet<Future<?>>();
        // for ( int i = 0; i < numThreads; i++ ) {
        //
        // log.info( "Starting thread " + i );
        // if ( k >= namedMatrix.rows() ) {
        // k = namedMatrix.rows() - 1;
        //
        // /*
        // * corner cases can fall through.
        // */
        // }
        // final DoubleMatrix<DesignElement, Integer> chunk = namedMatrix.getRowRange( j, k );
        // Future<?> future = service.submit( new Runnable() {
        // public void run() {
        String matrixName = rc.assignMatrix( namedMatrix, rowNameExtractor );
        rawResults.putAll( rc.rowApplyLinearModelWithLogging( matrixName, modelFormula, factorNameMap.keySet().toArray(
                new String[] {} ) ) );
        // }
        // } );
        //
        // futures.add( future );
        //
        // }
        //
        // service.shutdown();
        // try {
        // service.awaitTermination( 20, TimeUnit.MINUTES );
        // Thread.sleep( 100 );
        // log.info( "Analysis still running ..." );
        // } catch ( InterruptedException e ) {
        // throw new RuntimeException( "Analysis timed out or was terminated" );
        // }

        assert rawResults.size() == namedMatrix.rows();
        return rawResults;
    }

    /**
     * Convert factors to a design matrix usable in R.
     * 
     * @param factors
     * @param samplesUsed
     * @param factorList
     * @param factorNames
     * @return
     */
    private ObjectMatrix<String, String, Object> buildDesignMatrix( Collection<ExperimentalFactor> factors,
            List<BioMaterial> samplesUsed, List<ExperimentalFactor> factorList, List<String> factorNames ) {

        ObjectMatrix<String, String, Object> designMatrix = new ObjectMatrixImpl<String, String, Object>( samplesUsed
                .size(), factors.size() );

        designMatrix.setColumnNames( factorNames );

        List<String> rowNames = new ArrayList<String>();

        int row = 0;
        for ( BioMaterial samp : samplesUsed ) {

            rowNames.add( "biomat_" + samp.getId() );

            int col = 0;
            for ( ExperimentalFactor factor : factorList ) {

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

                        Measurement measurement = fv.getMeasurement();
                        if ( measurement != null ) {
                            try {
                                value = Double.parseDouble( measurement.getValue() );
                            } catch ( NumberFormatException e ) {
                                log.warn( "Failed to parse measurement as number: " + measurement.getValue() );
                                value = Double.NaN;
                            }
                        } else {
                            /*
                             * We always just use a dummy value. It's not human-readable but at least we're sure it is
                             * unique. (assuming the fv is persistent!)
                             */
                            value = "fv_" + fv.getId();
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
}
