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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Tests the one way anova analyzer. See test/data/stat-tests/README.txt for R code.
 * 
 * @author keshav
 * @version $Id$
 */
public class OneWayAnovaAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    private OneWayAnovaAnalyzer analyzer = null;

    /**
     * Tests the OneWayAnova method.
     */
    @Test
    public void testOneWayAnova() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        super.configureTestDataForOneWayAnova();

        configureMocks();

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();
        int numResults = resultSet.getResults().size();

        assertEquals( 100, numResults );

        /*
         * Check we got the histograms - only happens during persisting.
         */
        // File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( expressionExperiment.getShortName()
        // );
        //
        // File histFile = new File( dir, expressionExperiment.getShortName() + ".pvalues"
        // + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX );
        // assertTrue( histFile.exists() );
    }

    @Test
    public void testOnewayAnovaB() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        super.configureTestDataForOneWayAnova();

        configureMocks();
        /*
         * Add a factor with three levels
         */
        ExperimentalFactor experimentalFactorC = ExperimentalFactor.Factory.newInstance();
        experimentalFactorC.setName( "groupash" );
        experimentalFactorC.setId( 5399424551L );
        experimentalFactorC.setType( FactorType.CATEGORICAL );
        expressionExperiment.getExperimentalDesign().getExperimentalFactors().add( experimentalFactorC );

        FactorValue controlGroup = null;
        for ( int i = 1; i <= 3; i++ ) {
            FactorValue f = FactorValue.Factory.newInstance();
            f.setId( 2000L + i );
            if ( i != 2 ) {
                f.setValue( i + "_group" );
            } else {
                f.setValue( "control_group" );
                controlGroup = f;
            }
            f.setExperimentalFactor( experimentalFactorC );
            experimentalFactorC.getFactorValues().add( f );
        }

        List<FactorValue> facV = new ArrayList<FactorValue>( experimentalFactorC.getFactorValues() );
        for ( int i = 0; i < 8; i++ ) {
            super.biomaterials.get( i ).getFactorValues().add( facV.get( i % 3 ) );
        }

        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        factors.add( experimentalFactorC );
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, factors );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();
        int numResults = resultSet.getResults().size();

        assertEquals( 100, numResults );

        assertEquals( controlGroup, resultSet.getBaselineGroup() );

        factors = resultSet.getExperimentalFactors();

        assertEquals( 1, factors.size() );

        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();

            // if ( pvalue != null ) assertNotNull( stat );
            assertNotNull( probe );

            // log.debug( "probe: " + probe + "; Factor=" +
            // resultSet.getExperimentalFactors().iterator().next().getName()
            // + "; p-value: " + pvalue + "; T=" + stat );

            if ( probe.getName().equals( "probe_98" ) ) {
                assertEquals( 0.1604, pvalue, 0.001 );
            } else if ( probe.getName().equals( "probe_10" ) ) {
                assertEquals( 0.8014, pvalue, 0.0001 );
            } else if ( probe.getName().equals( "probe_4" ) ) {
                assertEquals( 0.6531, pvalue, 0.0001 );
            }

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 1 );
        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );

    }

}
