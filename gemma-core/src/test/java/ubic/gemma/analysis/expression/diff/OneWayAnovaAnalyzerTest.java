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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;

/**
 * Tests the one way anova analyzer.
 * 
 * @author keshav
 * @version $Id$
 */
public class OneWayAnovaAnalyzerTest extends BaseAnalyzerConfigurationTest {

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    private OneWayAnovaAnalyzer analyzer = null;

    /**
     * Tests the OneWayAnova method.
     */
    @Test
    public void testOneWayAnova() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }
        log.debug( "Testing OneWayAnova method in " + OneWayAnovaAnalyzer.class.getName() );

        super.configureTestDataForOneWayAnova();

        configureMocks();

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();
        int numResults = resultSet.getResults().size();

        assertEquals( numResults, 99 ); // we lose a row due to filtering
        checkResults( resultSet );
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
