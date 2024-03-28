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
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the two way anova analyzer. See test/data/stat-tests/README.txt for R code.
 *
 * @author keshav
 */
public class TwoWayAnovaWithoutInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    DiffExAnalyzer analyzer = null;

    /**
     * Tests the TwoWayAnova method.
     */
    @Test
    public void testTwoWayAnova() {

        log.debug( "Testing getPValues method in " + DiffExAnalyzer.class.getName() );

        assumeTrue( "Could not establish R connection.  Skipping test ...", connected );

        this.configureMocks();

        List<ExperimentalFactor> factors = Arrays.asList( experimentalFactorA_Area, experimentalFactorB );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( factors );
        config.setModerateStatistics( false );
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, config );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 2, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            this.checkResults( resultSet );
        }
    }

    private void configureMocks() {

        this.configureMockAnalysisServiceHelper();

        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );

    }

    /**
     * @param resultSet the result set to check
     */
    private void checkResults( ExpressionAnalysisResultSet resultSet ) {

        Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactors();
        assertEquals( "Should not have an interaction term", 1, factors.size() );

        ExperimentalFactor f = factors.iterator().next();

        boolean found = false;
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            CompositeSequence probe = r.getProbe();
            Double pvalue = r.getPvalue();
            if ( f.equals( super.experimentalFactorB ) && probe.getName().equals( "probe_1" ) ) {
                assertEquals( 0.501040, pvalue, 0.001 );
                found = true;
            }
            Collection<ContrastResult> contrasts = r.getContrasts();
            Double stat;
            if ( contrasts.isEmpty() ) {
                continue;
            }

            stat = contrasts.iterator().next().getTstat();

            assertNotNull( probe );

            if ( f.equals( super.experimentalFactorA_Area ) ) {

                assertEquals( factorValueA2, resultSet.getBaselineGroup() );

                switch ( probe.getName() ) {
                    case "probe_1":  // id=1001
                        assertEquals( 0.001814, pvalue, 0.00001 );
                        assertNotNull( stat );
                        assertEquals( -287.061, stat, 0.001 );
                        found = true;
                        break;
                    case "probe_97":  // id 1097
                        assertEquals( 0.3546, pvalue, 0.001 );
                        break;
                    case "probe_0":
                        assertEquals( 1.36e-12, pvalue, 1e-10 );
                        assertNotNull( stat );
                        assertEquals( -425.3, stat, 0.1 );
                        break;
                }

            } else {

                assertEquals( factorValueB2, resultSet.getBaselineGroup() );

                if ( probe.getName().equals( "probe_97" ) ) {
                    assertEquals( 0.4449, pvalue, 0.001 );
                }

            }

        }
        assertTrue( "Did not find expected results for probe_1", found );
    }

}
