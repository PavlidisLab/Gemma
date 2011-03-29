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

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Tests the two way anova analyzer with interactions. See test/data/stat-tests/README.txt for R code.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    TwoWayAnovaWithInteractionsAnalyzer analyzer = null;

    @Test
    public void testTwoWayAnova() throws Exception {

        log.debug( "Testing TwoWayAnova method in " + TwoWayAnovaWithInteractionsAnalyzer.class.getName() );

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment,
                experimentalFactorA_Area, experimentalFactorB );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( NUM_TWA_RESULT_SETS, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            checkResults( resultSet );
        }
    }

    /**
     * @param resultSet
     */
    private void checkResults( ExpressionAnalysisResultSet resultSet ) {

        Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactors();

        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();
            pvalue = pvalue == null ? Double.NaN : pvalue;
            Double qvalue = probeAnalysisResult.getCorrectedPvalue();
            // Double stat = probeAnalysisResult.getEffectSize();
            // Collection<ContrastResult> contrasts = probeAnalysisResult.getContrasts();
            // Double stat = null;
            // if ( !contrasts.isEmpty() ) {
            // stat = contrasts.iterator().next().getTstat();
            // }

            // if ( pvalue != null ) assertNotNull( stat );
            assertNotNull( probe );

            // log.debug( "probe: " + probe + "; p-value: " + pvalue + "; F=" + stat );

            if ( factors.size() == 1 ) {

                ExperimentalFactor f = factors.iterator().next();

                if ( f.equals( super.experimentalFactorA_Area ) ) {
                    assertEquals( factorValueA2, resultSet.getBaselineGroup() );
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.8769, pvalue, 0.001 );
                        assertNotNull( qvalue );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 5.158e-10, pvalue, 1e-12 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        assertEquals( 0.0048, pvalue, 0.0001 );
                    }

                } else {
                    assertEquals( factorValueB2, resultSet.getBaselineGroup() );
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.6888, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.07970, pvalue, 0.00001 );
                    }

                }

            } else {
                assertEquals( null, resultSet.getBaselineGroup() );
                if ( probe.getName().equals( "probe_98" ) ) {
                    assertEquals( 0.7893, pvalue, 0.001 );
                } else if ( probe.getName().equals( "probe_10" ) ) {
                    assertEquals( 0.04514, pvalue, 0.00001 );
                } else if ( probe.getName().equals( "probe_4" ) ) {
                    assertEquals( Double.NaN, pvalue.doubleValue(), 0.00001 );
                }
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
