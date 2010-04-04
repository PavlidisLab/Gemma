/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.r.type.HTest;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A t-test implementation as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * 
 * @author keshav
 * @version $Id$
 */
@Service
@Scope(value = "prototype")
public class TTestAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    Log log = LogFactory.getLog( this.getClass() );

    public Map<FactorValue, Collection<Integer>> byFactorvalue( List<BioMaterial> samplesUsed ) {
        Map<FactorValue, Collection<Integer>> result = new HashMap<FactorValue, Collection<Integer>>();

        int columnNumber = 0;
        for ( BioMaterial biomaterial : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = biomaterial.getFactorValues();

            for ( FactorValue factorValue : factorValuesFromBioMaterial ) {
                if ( !result.containsKey( factorValue ) ) {
                    result.put( factorValue, new HashSet<Integer>() );
                }

                result.get( factorValue ).add( columnNumber );
            }
            columnNumber++;

        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.analysis.diff.AbstractAnalyzer#getExpressionAnalysis(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        return run( expressionExperiment, experimentalFactors );

    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.util.Collection)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {
        if ( experimentalFactors.size() != 1 ) {
            throw new RuntimeException( "T-test supports 1 experimental factor.  Received "
                    + experimentalFactors.size() + "." );
        }

        ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();

        Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
        if ( factorValues.size() > 2 ) {
            throw new RuntimeException( "T-test supports at most 2 factor values.  Received " + factorValues.size()
                    + "." );
        }

        Iterator<FactorValue> iter = factorValues.iterator();

        FactorValue factorValueA = iter.next();

        if ( factorValues.size() == 2 ) {
            FactorValue factorValueB = iter.next();
            return tTest( expressionExperiment, factorValueA, factorValueB );
        } else {
            return tTest( expressionExperiment, factorValueA );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#generateHistograms(java.lang.String,
     * java.util.ArrayList, int, int, int, double[])
     */
    @Override
    protected Collection<Histogram> generateHistograms( String histFileName, ArrayList<ExperimentalFactor> effects,
            int numBins, int min, int max, Double[] pvalues ) {

        if ( pvalues == null ) {
            return null;
        }

        Collection<Histogram> hists = new HashSet<Histogram>();

        Histogram hist = new Histogram( histFileName, numBins, min, max );
        for ( int i = 0; i < pvalues.length; i++ ) {
            hist.fill( pvalues[i] );
        }

        hists.add( hist );

        return hists;
    }

    /**
     * Common processing for one- and two-sample t-tests.
     * 
     * @param expressionExperiment
     * @param dmatrix
     * @param factorValueA
     * @param factorValueB should be null in case of one-sample
     * @param results
     * @return
     */
    private DifferentialExpressionAnalysis processTtestResults( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, FactorValue factorValueA, FactorValue factorValueB, List<HTest> results ) {
        List<Double> pvaluesl = new ArrayList<Double>();
        List<Double> tstatistics = new ArrayList<Double>();
        for ( HTest r : results ) {
            pvaluesl.add( r.getPvalue() );
            tstatistics.add( r.getStatistic() );
        }

        double[] pvalues = new double[pvaluesl.size()];
        int j = 0;
        for ( Double d : pvaluesl ) {
            pvalues[j] = d; // might be NaN
            j++;
        }

        double[] ranks = computeRanks( pvalues );

        /* write out histogram */
        writePValuesHistogram( ArrayUtils.toObject( pvalues ), expressionExperiment, null );

        /* q-value */
        double[] qvalues = super.getQValues( ArrayUtils.toObject( pvalues ) );

        // TODO pass the DifferentialExpressionAnalysisConfig in (see LinkAnalysisService)
        /* Create the expression analysis and pack the results. */
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        DifferentialExpressionAnalysis expressionAnalysis = config.toAnalysis();

        ExpressionExperimentSet eeSet = ExpressionExperimentSet.Factory.newInstance();
        Collection<BioAssaySet> experimentsAnalyzed = new HashSet<BioAssaySet>();
        experimentsAnalyzed.add( expressionExperiment );
        eeSet.setExperiments( experimentsAnalyzed );
        expressionAnalysis.setExpressionExperimentSetAnalyzed( eeSet );

        List<DifferentialExpressionAnalysisResult> analysisResults = new ArrayList<DifferentialExpressionAnalysisResult>();

        for ( int i = 0; i < dmatrix.rows(); i++ ) {
            DesignElement de = dmatrix.getDesignElementForRow( i );
            CompositeSequence cs = ( CompositeSequence ) de;

            ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
            probeAnalysisResult.setProbe( cs );

            // Don't use NaN as we can't save that in the database.
            probeAnalysisResult.setPvalue( nan2Null( pvaluesl.get( i ) ) );
            probeAnalysisResult.setCorrectedPvalue( nan2Null( qvalues[i] ) );
            probeAnalysisResult.setScore( nan2Null( tstatistics.get( i ) ) );
            probeAnalysisResult.setQuantitationType( dmatrix.getQuantitationTypes().iterator().next() );
            probeAnalysisResult.setRank( nan2Null( ranks[i] ) );

            if ( factorValueB == null ) {
                // one sample t-test
                if ( probeAnalysisResult.getScore() > 0 ) {
                    probeAnalysisResult.setUpRegulated( true );
                } else {
                    probeAnalysisResult.setUpRegulated( false );
                }
            } else {
                // TODO - need to know more. probeAnalysisResult.setUpRegulated();
            }

            assert probeAnalysisResult.getPvalue() == null || !Double.isNaN( probeAnalysisResult.getPvalue() );
            assert probeAnalysisResult.getCorrectedPvalue() == null
                    || !Double.isNaN( probeAnalysisResult.getCorrectedPvalue() );

            analysisResults.add( probeAnalysisResult );

        }

        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        factors.add( factorValueA.getExperimentalFactor() );
        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance( expressionAnalysis,
                analysisResults, factors );
        resultSets.add( resultSet );

        expressionAnalysis.setResultSets( resultSets );

        expressionAnalysis.setName( this.getClass().getSimpleName() );
        if ( factorValueB != null ) {
            expressionAnalysis.setDescription( "Two-sample T-test for " + factorValueA + " vs " + factorValueB );
        } else {
            expressionAnalysis.setDescription( "One-sample T-test for " + factorValueA );
        }

        log.info( "T-tests done" );
        return expressionAnalysis;
    }

    /**
     * Perform a one-sample t-test. All biomaterials must have the same factor value.
     * 
     * @param expressionExperiment
     * @param factorValue
     * @return
     */
    private DifferentialExpressionAnalysis tTest( ExpressionExperiment expressionExperiment, FactorValue factorValue ) {
        connectToR();

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisHelperService
                .getBioMaterialsForBioAssays( dmatrix );

        for ( BioMaterial bioMaterial : samplesUsed ) {
            if ( !bioMaterial.getFactorValues().contains( factorValue ) ) {
                throw new IllegalArgumentException(
                        "For one-sample t-test all biomaterials have to have the same factor value: " + factorValue
                                + ", " + bioMaterial + " did not." );
            }

        }

        if ( !dmatrix.getQuantitationTypes().iterator().next().getIsRatio() ) {
            throw new IllegalArgumentException( "One-sample t-test can only be used on ratiometric data" );
        }

        DoubleMatrix<DesignElement, Integer> namedMatrix = dmatrix.getMatrix();

        String matrixName = rc.assignMatrix( namedMatrix );

        StringBuffer rCommand = new StringBuffer();
        rCommand.append( "apply(" );
        rCommand.append( matrixName );
        rCommand.append( ", 1, function(x) try(t.test(x), silent=T)" );
        rCommand.append( ")" );

        if ( log.isDebugEnabled() ) log.debug( namedMatrix );
        if ( log.isDebugEnabled() ) log.debug( rCommand );

        log.info( "Starting t-test analysis ... " );

        try {
            List<HTest> resultus = ( List<HTest> ) rc.listEvalWithLogging( HTest.class, rCommand.toString() );

            DifferentialExpressionAnalysis expressionAnalysis = processTtestResults( expressionExperiment, dmatrix,
                    factorValue, null, resultus );

            return expressionAnalysis;

        } catch ( Exception e ) {
            log.error( "Error during one-sample t-test analysis on " + expressionExperiment.getShortName() );
            log.error( dmatrix );
            throw ( new RuntimeException( e ) );
        } finally {
            disconnectR();
        }
    }

    /**
     * Two-sample tTest.
     * 
     * @param expressionExperiment
     * @param factorValueA
     * @param factorValueB
     * @return
     */
    private DifferentialExpressionAnalysis tTest( ExpressionExperiment expressionExperiment, FactorValue factorValueA,
            FactorValue factorValueB ) {

        connectToR();

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisHelperService
                .getBioMaterialsForBioAssays( dmatrix );

        Collection<FactorValue> factorValues = new ArrayList<FactorValue>();
        factorValues.add( factorValueA );
        factorValues.add( factorValueB );

        FactorValue controlGroup = determineControlGroup( factorValues );

        Map<FactorValue, Collection<Integer>> byFactorvalue = byFactorvalue( samplesUsed );
        Collection<Integer> controlColumns = byFactorvalue.get( controlGroup );
        // ...

        /*
         * TODO: if non-null, use the control group to define the effect size in a sensible way (up or downregulated wrt
         * control). First thing is to re-organize the data by factor values. Then we can isolate the baseline group and
         * compute effect sizes directly.
         */

        List<String> rFactors = DifferentialExpressionAnalysisHelperService.getRFactorsFromFactorValuesForOneWayAnova(
                factorValues, samplesUsed );

        assert !rFactors.isEmpty();

        DoubleMatrix<DesignElement, Integer> namedMatrix = dmatrix.getMatrix();

        String facts = rc.assignStringList( rFactors );
        String tfacts = "t(" + facts + ")";
        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( namedMatrix );

        StringBuffer pvalueCommand = new StringBuffer();
        pvalueCommand.append( "apply(" );
        pvalueCommand.append( matrixName );
        pvalueCommand.append( ", 1, function(x) try(t.test(x ~ " + factor + "), silent=T)" );
        pvalueCommand.append( ")" );

        log.info( "Starting two-sample t-test analysis ... " );

        try {
            List<HTest> resultus = ( List<HTest> ) rc.listEvalWithLogging( HTest.class, pvalueCommand.toString() );

            DifferentialExpressionAnalysis expressionAnalysis = processTtestResults( expressionExperiment, dmatrix,
                    factorValueA, factorValueB, resultus );

            return expressionAnalysis;

        } catch ( Exception e ) {
            log.error( "Error during t-test analysis on " + expressionExperiment.getShortName() );
            log.error( "Factor= " + StringUtils.join( rFactors, "," ) );
            log.error( dmatrix );
            throw ( new RuntimeException( e ) );
        } finally {
            disconnectR();
        }
    }
}
