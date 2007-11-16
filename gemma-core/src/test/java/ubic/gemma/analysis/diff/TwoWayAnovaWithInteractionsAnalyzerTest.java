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
package ubic.gemma.analysis.diff;

import java.util.Iterator;

import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Tests the two way anova analyzer with interactions.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    /**
     * The following has been confirmed with the results from the R console:
     * <p>
     * data (for one design element): 0.923, 0.823, 0.0894, 0.0632, 0.7038, 0.603, 0.839, 0.395
     * <p>
     * factor A: "pcp", "no pcp", "pcp", "no pcp", "no pcp", "pcp", "no pcp", "pcp"
     * <p>
     * factor B: "cerebellum", "amygdala", "amygdala", "amygdala", "cerebellum", "cerebellum", "cerebellum", "amygdala"
     * <p>
     * resulting p-value: 0.662
     * <p>
     * resulting p-value: 0.129
     * <p>
     * resulting p-value: 0.687
     * <p>
     * (Note: Because there are only two factor values ("pcp", "no pcp") this is really just a t-test but this was
     * tested out on the R console the same one way anova call used in the {@link OneWayAnovaAnalyzer}).
     */

    TwoWayAnovaWithInteractionsAnalyzer analyzer = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        analyzer = ( TwoWayAnovaWithInteractionsAnalyzer ) this.getBean( "twoWayAnovaWithInteractionsAnalyzer" );

        configureMocks();
    }

    /**
     * 
     *
     */
    public void testTwoWayAnova() {

        log.debug( "Testing TwoWayAnova method in " + TwoWayAnovaWithInteractionsAnalyzer.class.getName() );

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        Iterator<ExperimentalFactor> iter = expressionExperiment.getExperimentalDesign().getExperimentalFactors()
                .iterator();

        ExperimentalFactor experimentalFactorA = iter.next();

        ExperimentalFactor experimentalFactorB = iter.next();

        ExpressionAnalysis expressionAnalysis = analyzer.twoWayAnova( expressionExperiment, experimentalFactorA,
                experimentalFactorB );

        assertEquals( expressionAnalysis.getAnalysisResults().size(), NUM_DESIGN_ELEMENTS * 3 );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper();

        analyzer.setAnalysisHelperService( analysisHelperService );

    }
}
