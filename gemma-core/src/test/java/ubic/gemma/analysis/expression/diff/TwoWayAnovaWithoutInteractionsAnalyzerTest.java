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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;

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

    }

    /**
     * Tests the TwoWayAnova method.
     */
    public void testTwoWayAnova() throws Exception {

        log.debug( "Testing getPValues method in " + TwoWayAnovaWithoutInteractionsAnalyzer.class.getName() );

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        super.configureTestDataForTwoWayAnovaWithoutInteractions();

        configureMocks();

        DifferentialExpressionAnalysis differentialExpressionAnalysis = analyzer
                .getDifferentialExpressionAnalysis( expressionExperiment );

        Collection<ExpressionAnalysisResultSet> resultSets = differentialExpressionAnalysis.getResultSets();

        assertEquals( NUM_TWA_RESULT_SETS - 1, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            log.info( "*** Result set for factor: " + resultSet.getExperimentalFactor()
                    + ".  If factor is null, the result set contains all results per probe. ***" );
            logResults( resultSet );
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
