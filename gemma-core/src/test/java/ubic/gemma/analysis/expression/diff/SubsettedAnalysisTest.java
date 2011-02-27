/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;

/**
 * @author paul
 * @version $Id$
 */
public class SubsettedAnalysisTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    private GenericAncovaAnalyzer analyzer = null;

    @Test
    public final void testWithSubset() throws Exception {
        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.getFactorsToInclude().add( this.experimentalFactorA_Area );
        config.setSubsetFactor( this.experimentalFactorB );

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, config );

        assertEquals( 2, expressionAnalyses.size() );

        for ( DifferentialExpressionAnalysis expressionAnalysis : expressionAnalyses ) {

            assertNotNull( expressionAnalysis.getExperimentAnalyzed() );

            assertEquals( 4, expressionAnalysis.getExperimentAnalyzed().getBioAssays().size() );

            Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

            ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();
            int numResults = resultSet.getResults().size();

            assertEquals( 100, numResults );
        }
    }

    @Test
    public final void testInvalidSubsetFactor() throws Exception {

        configureMocks();

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.getFactorsToInclude().add( this.experimentalFactorA_Area );
        config.setSubsetFactor( this.experimentalFactorA_Area );
        try {
            analyzer.run( expressionExperiment, config );
            fail( "Should have gotten an exception" );
        } catch ( Exception e ) {
        }
    }

    @Override
    protected void configureMocks() throws Exception {
        configureMockAnalysisServiceHelper( 1 );
        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );
    }

}
