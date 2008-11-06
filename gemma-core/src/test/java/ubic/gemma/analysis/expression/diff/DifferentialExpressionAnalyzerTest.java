/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

/**
 * Tests the {@link DifferentialExpessionAnalysis} tool.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerTest extends BaseAnalyzerConfigurationTest {

    DifferentialExpressionAnalyzer analysis = null;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        analysis = ( DifferentialExpressionAnalyzer ) this.getBean( "differentialExpressionAnalyzer" );

    }

    /**
     * * Tests determineAnalysis.
     * <p>
     * 2 experimental factors
     * <p>
     * 2 factor value / experimental factor
     * <p>
     * complete block design and biological replicates
     * <p>
     * Expected analyzer: {@link TwoWayAnovaWithInteractionsAnalyzer}
     * 
     * @throws Exception
     */
    public void testDetermineAnalysisA() throws Exception {
        configureMocks();
        AbstractAnalyzer analyzer = analysis.determineAnalysis( expressionExperiment );
        assertTrue( analyzer instanceof TwoWayAnovaWithInteractionsAnalyzer );
    }

    /**
     * Tests determineAnalysis.
     * <p>
     * 2 experimental factors
     * <p>
     * 2 factor value / experimental factor
     * <p>
     * no replicates
     * <p>
     * Expected analyzer: {@link TwoWayAnovaWithoutInteractionsAnalyzer}
     * 
     * @throws Exception
     */
    public void testDetermineAnalysisB() throws Exception {
        super.configureTestDataForTwoWayAnovaWithoutInteractions();
        configureMocks();
        AbstractAnalyzer analyzer = analysis.determineAnalysis( expressionExperiment );
        assertTrue( analyzer instanceof TwoWayAnovaWithoutInteractionsAnalyzer );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 2 );

        DifferentialExpressionAnalysisHelperService analyzerHelper = ( DifferentialExpressionAnalysisHelperService ) this
                .getBean( "differentialExpressionAnalysisHelperService" );

        analysis.setDifferentialExpressionAnalysisHelperService( analyzerHelper );

    }

}
