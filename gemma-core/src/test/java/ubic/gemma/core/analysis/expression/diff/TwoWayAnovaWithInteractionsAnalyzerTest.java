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
package ubic.gemma.core.analysis.expression.diff;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the two way anova analyzer with interactions. See test/data/stat-tests/README.txt for R code.
 *
 * @author keshav
 */
public class TwoWayAnovaWithInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    private DiffExAnalyzer analyzer = null;

    @Test
    public void testTwoWayAnova() {
        log.debug( "Testing TwoWayAnova method in " + DiffExAnalyzer.class.getName() );

        assumeTrue( "Could not establish R connection.  Skipping test ...", connected );

        Collection<ExperimentalFactor> factors = new HashSet<>();
        factors.add( experimentalFactorA_Area );
        factors.add( experimentalFactorB );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( factors );
        config.addInteractionToInclude( factors );
        config.setModerateStatistics( false );
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, dmatrix, config );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( BaseAnalyzerConfigurationTest.NUM_TWA_RESULT_SETS, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            this.checkResults( resultSet );
        }
    }

    /**
     * @param resultSet the result set to check
     */
    private void checkResults( ExpressionAnalysisResultSet resultSet ) {

        Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactors();

        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            CompositeSequence probe = r.getProbe();
            Double pvalue = r.getPvalue();
            pvalue = pvalue == null ? Double.NaN : pvalue;
            Double qvalue = r.getCorrectedPvalue();

            assertNotNull( probe );

            if ( factors.size() == 1 ) {

                ExperimentalFactor f = factors.iterator().next();

                if ( f.equals( super.experimentalFactorA_Area ) ) {
                    assertEquals( factorValueA2, resultSet.getBaselineGroup() );
                    switch ( probe.getName() ) {
                        case "probe_98":
                            assertEquals( 0.8769, pvalue, 0.001 );
                            assertNotNull( qvalue );
                            break;
                        case "probe_10":
                            assertEquals( 5.158e-10, pvalue, 1e-12 );
                            break;
                        case "probe_4":
                            assertEquals( 0.0048, pvalue, 0.0001 );
                            break;
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
                assertNull( resultSet.getBaselineGroup() );
                switch ( probe.getName() ) {
                    case "probe_98":
                        assertEquals( 0.7893, pvalue, 0.001 );
                        break;
                    case "probe_10":
                        assertEquals( 0.04514, pvalue, 0.00001 );
                        break;
                    case "probe_4":
                        assertEquals( Double.NaN, pvalue, 0.00001 );
                        break;
                }
            }

        }
    }
}
