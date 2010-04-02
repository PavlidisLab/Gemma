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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Tests the two way anova analyzer with interactions.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    TwoWayAnovaWithInteractionsAnalyzer analyzer = null;

    /**
     * 
     *
     */
    @Test
    public void testTwoWayAnova() throws Exception {

        log.debug( "Testing TwoWayAnova method in " + TwoWayAnovaWithInteractionsAnalyzer.class.getName() );

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.twoWayAnova( expressionExperiment,
                experimentalFactorA, experimentalFactorB );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( NUM_TWA_RESULT_SETS, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            log
                    .debug( "*** Result set for factor: "
                            + resultSet.getExperimentalFactor()
                            + ".  If factor is null, the result set contains all results per probe or represents the results for the 'interaction' effect. ***" );
            checkResults( resultSet );
        }

    }

    /**
     * @param resultSet
     */
    @Override
    protected void checkResults( ExpressionAnalysisResultSet resultSet ) {

        Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactor();

        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();
            Double stat = probeAnalysisResult.getScore();

            if ( pvalue != null ) assertNotNull( stat );
            assertNotNull( probe );

            log.debug( "probe: " + probe + "; p-value: " + pvalue + "; F=" + stat );

            if ( factors.size() == 1 ) {

                ExperimentalFactor f = factors.iterator().next();

                if ( f.equals( super.experimentalFactorA ) ) {

                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.7485, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 8.97e-08, pvalue, 0.00001 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        assertEquals( 0.00656, pvalue, 0.0001 );
                    }

                } else {
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.7792, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.08816, pvalue, 0.00001 );
                    }

                }

            } else {
                if ( probe.getName().equals( "probe_98" ) ) {
                    assertEquals( 0.9585, pvalue, 0.001 );
                } else if ( probe.getName().equals( "probe_10" ) ) {
                    assertEquals( 0.11823, pvalue, 0.00001 );
                }
            }

        }
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
