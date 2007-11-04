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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.analysis.ExpressionAnalysis;

/**
 * Tests the two way anova analyzer.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithoutInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    private Log log = LogFactory.getLog( this.getClass() );

    TwoWayAnovaWithoutInteractionsAnalyzer analyzer = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        analyzer = ( TwoWayAnovaWithoutInteractionsAnalyzer ) this.getBean( "twoWayAnovaWithoutInteractionsAnalyzer" );

        analyzer.setAnalysisHelperService( analysisHelperService );
    }

    /**
     * Tests the TwoWayAnova method.
     */
    public void testTwoWayAnova() {

        log.debug( "Testing getPValues method in " + TwoWayAnovaWithoutInteractionsAnalyzer.class.getName() );

        super.configureTestDataForTwoWayAnovaWithoutInteractions();

        ExpressionAnalysis expressionAnalysis = analyzer.getExpressionAnalysis( expressionExperiment );

        assertEquals( expressionAnalysis.getAnalysisResults().size(), NUM_DESIGN_ELEMENTS * 2 );
    }

}
