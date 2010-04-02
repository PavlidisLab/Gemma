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

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Tests the two way anova analyzer.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithoutInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    TwoWayAnovaWithoutInteractionsAnalyzer analyzer = null;

    /**
     * Tests the TwoWayAnova method.
     */
    @Test
    public void testTwoWayAnova() throws Exception {

        log.debug( "Testing getPValues method in " + TwoWayAnovaWithoutInteractionsAnalyzer.class.getName() );

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        DifferentialExpressionAnalysis differentialExpressionAnalysis = analyzer.run( expressionExperiment, Arrays
                .asList( new ExperimentalFactor[] { experimentalFactorA, experimentalFactorB } ) );

        Collection<ExpressionAnalysisResultSet> resultSets = differentialExpressionAnalysis.getResultSets();

        assertEquals( 2, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            checkResults( resultSet );
        }
    }

    /**
     * @param resultSet
     */
    @Override
    protected void checkResults( ExpressionAnalysisResultSet resultSet ) {

        Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactor();
        assertEquals( "Should not have an interaction term", 1, factors.size() );

        ExperimentalFactor f = factors.iterator().next();

        log.info( "************  " + resultSet.getExperimentalFactor() );

        boolean found = false;
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();
            Double stat = probeAnalysisResult.getScore();

            if ( pvalue != null ) assertNotNull( stat );
            assertNotNull( probe );

            log.debug( "probe: " + probe + "; p-value: " + pvalue + "; F=" + stat );

            if ( f.equals( super.experimentalFactorA ) ) {
                log.info( probe.getName() );
                if ( probe.getName().equals( "probe_1" ) ) { // id=1001
                    assertEquals( 0.0009837, pvalue, 0.001 );
                    assertEquals( 418990.60, stat, 0.1 );
                    found = true;
                } else if ( probe.getName().equals( "probe_97" ) ) { // id 1097
                    assertEquals( 0.1567, pvalue, 0.00001 );
                }

            } else {
                if ( probe.getName().equals( "probe_1" ) ) {
                    assertEquals( 0.0649223, pvalue, 0.001 );
                    assertEquals( 95.49, stat, 0.1 );
                    found = true;
                } else if ( probe.getName().equals( "probe_97" ) ) {
                    assertEquals( 0.8323, pvalue, 0.001 );
                }

            }

        }
        assertTrue( "Did not find expected results for probe_1", found );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 1 );

        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );

    }

}
