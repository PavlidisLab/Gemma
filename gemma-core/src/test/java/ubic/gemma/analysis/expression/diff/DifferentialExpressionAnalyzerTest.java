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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link DifferentialExpessionAnalysis} tool.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    AnalysisSelectionAndExecutionService analysis = null;
    DiffExAnalyzer analyzer;

    /**
     * * Tests determineAnalysis.
     * <p>
     * 2 experimental factors
     * <p>
     * 2 factor value / experimental factor
     * <p>
     * complete block design and biological replicates
     * <p>
     * Expected analyzer: {@link TwoWayAnovaWithInteractionsAnalyzerImpl}
     * 
     * @throws Exception
     */
    @Test
    public void testDetermineAnalysisA() throws Exception {
        configureMocks();
        analyzer = this.getBean( DiffExAnalyzer.class );
        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );
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
     * Expected analyzer: {@link TwoWayAnovaWithoutInteractionsAnalyzerImpl}
     * 
     * @throws Exception
     */
    @Test
    public void testDetermineAnalysisB() throws Exception {
        super.configureTestDataForTwoWayAnovaWithoutInteractions();
        configureMocks();
        analyzer = this.getBean( DiffExAnalyzer.class );
        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 2 );

    }

}
