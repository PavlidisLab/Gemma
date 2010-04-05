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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

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
 * Handles fitting linear models without interactions for more than 2 factors, which may be continuous or fixed.
 * <p>
 * One factor can be constant (the same value for all samples); such a factor will be analyzed by looking at the
 * intercept in the fitted model. This is only appropriate for 'non-reference' designs on ratiometric arrays.
 * 
 * @author paul
 * @version $Id$
 */
@Service
@Scope(value = "prototype")
public class AncovaAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    private static Log log = LogFactory.getLog( AncovaAnalyzer.class );

    @Override
    protected Collection<Histogram> generateHistograms( String histFileName, List<ExperimentalFactor> effects,
            int numBins, int min, int max, Double[] pvalues ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment ) {
        throw new UnsupportedOperationException( "Currently you must choose factors for ANCOVA" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.util.Collection)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {
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

        if ( samplesUsed.size() <= factors.size() ) {
            throw new IllegalArgumentException( "Must have more samples than factors" );
        }

        /*
         * Assign the matrix in R.
         */
        DoubleMatrix<DesignElement, Integer> namedMatrix = dmatrix.getMatrix();
        Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );
        String matrixName = rc.assignMatrix( namedMatrix, rowNameExtractor );

        /*
         * We need a list of factors...
         */
        List<ExperimentalFactor> factorList = new ArrayList<ExperimentalFactor>();
        factorList.addAll( factors );

        /*
         * Create names we can use to refer to our factors in a predictable way, and see if we need the intercept.
         */
        ExperimentalFactor interceptFactor = null;
        Map<String, ExperimentalFactor> factorNameMap = new LinkedHashMap<String, ExperimentalFactor>();
        List<String> factorNames = new ArrayList<String>();
        for ( ExperimentalFactor experimentalFactor : factorList ) {
            String factName = "fact." + experimentalFactor.getId();
            factorNameMap.put( factName, experimentalFactor );
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
         * Run the analysis
         */
        String factTerm = StringUtils.join( factorNameMap.keySet(), "+" );

        ObjectMatrix<String, String, Object> designMatrix = buildDesignMatrix( factors, samplesUsed, factorList,
                factorNames );

        String designMatrixVar = rc.dataFrame( designMatrix );
        String command = "apply(" + matrixName + ", 1, function(x) { try( summary(lm( x ~ " + factTerm + ", data="
                + designMatrixVar + " )) , silent=T ) })";
        Map<String, LinearModelSummary> rawResults = rc.linearModelEvalWithLogging( command );

        if ( log.isDebugEnabled() ) {
            log.debug( namedMatrix );
            log.debug( command );
            log.debug( designMatrix );
        }

        /*
         * Initialize some other data structures we need.
         */
        DifferentialExpressionAnalysis expressionAnalysis = super.initAnalysisEntity( expressionExperiment );
        Map<String, List<DifferentialExpressionAnalysisResult>> resultLists = new HashMap<String, List<DifferentialExpressionAnalysisResult>>();
        Map<String, List<Double>> pvaluesForQvalue = new HashMap<String, List<Double>>();
        for ( String factorName : factorNameMap.keySet() ) {
            resultLists.put( factorName, new ArrayList<DifferentialExpressionAnalysisResult>() );
            pvaluesForQvalue.put( factorName, new ArrayList<Double>() );
        }

        /*
         * Create result objects for each model fit. Keeping things in order is important.
         */
        for ( DesignElement el : namedMatrix.getRowNames() ) {

            CompositeSequence cs = ( CompositeSequence ) el;

            LinearModelSummary lm = rawResults.get( rowNameExtractor.transform( el ).toString() );

            assert lm != null;

            for ( String factorName : factorNameMap.keySet() ) {

                Double pvalue;
                Double score;

                if ( interceptFactor != null && factorNameMap.get( factorName ).equals( interceptFactor ) ) {
                    pvalue = lm.getInterceptP();
                    score = lm.getInterceptT();
                } else {
                    pvalue = lm.getP( factorName );
                    score = lm.getT( factorName );
                }

                ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
                probeAnalysisResult.setProbe( cs );
                probeAnalysisResult.setQuantitationType( quantitationType );

                probeAnalysisResult.setPvalue( nan2Null( pvalue ) );

                // TODO get a directionality on the score:
                // probeAnalysisResult.setUpRegulated( true );
                probeAnalysisResult.setScore( nan2Null( score ) );
                pvaluesForQvalue.get( factorName ).add( pvalue );

                resultLists.get( factorName ).add( probeAnalysisResult );

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
            factorsUsed.add( factorNameMap.get( fName ) );
            ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance(
                    expressionAnalysis, resultLists.get( fName ), factorsUsed );
            resultSets.add( resultSet );

        }

        /*
         * Complete analysis config
         */
        expressionAnalysis.setResultSets( resultSets );
        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( "ANCOVA with " + factorNameMap.size() + " factors"
                + ( interceptFactor == null ? "" : " with intercept treated as factor" ) );

        /*
         * TODO histograms.
         */

        disconnectR();
        return expressionAnalysis;

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
