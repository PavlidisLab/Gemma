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

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzerTest extends BaseAnalyzerConfigurationTest {

    /**
     * The following has been confirmed with the results from the R console:
     * <p>
     * data (for one design element): 0.654, 0.277, 0.999, 0.0989, 0.963, 0.747, 0.726, 0.426
     * <p>
     * factor: "no pcp", "no pcp", "no pcp", "pcp", "pcp", "pcp", "no pcp", "pcp"
     * <p>
     * resulting p-value: 0.677
     */

    @Autowired
    private TTestAnalyzer analyzer = null;

    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * Tests the t-test with an {@link ExpressionExperiment}.
     */
    @Test
    public void testTTestWithExpressionExperiment() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        factors.add( super.experimentalFactorA );

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment, factors );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        int numResults = resultSet.getResults().size();

        assertEquals( numResults, NUM_DESIGN_ELEMENTS );
        checkResults( resultSet );
    }

    /**
     * @param resultSet
     */
    @Override
    protected void checkResults( ExpressionAnalysisResultSet resultSet ) {

        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();
            log.debug( "probe: " + probe + "; p-value: " + pvalue );

            if ( probe.getName().equals( "0" ) ) {
                assertEquals( 9.16511e-8, pvalue, 0.0001 );
            } else if ( probe.getName().equals( "16" ) ) {
                assertTrue( pvalue == null );
            } else if ( probe.getName().equals( "17" ) ) {
                assertEquals( 2.7407e-7, pvalue, 0.00001 );
            } else if ( probe.getName().equals( "75" ) ) {
                assertEquals( 0.5700398, pvalue, 0.00001 );
            }
        }
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
