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
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

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

    TTestAnalyzer analyzer = null;

    private Log log = LogFactory.getLog( this.getClass() );

    private FactorValue factorValueA = null;

    private FactorValue factorValueB = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#onSetUpInTransaction()
     */
    @Override
    public void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();

        analyzer = ( TTestAnalyzer ) this.getBean( "tTestAnalyzer" );

        /*
         * Doing this here because the test experiment has 2 experimental factors, each with 2 factor values. To test
         * the t-test, we only want to use one experimental factor and one factor value for each biomaterial.
         */
        Collection<FactorValue> factorValues = experimentalFactors.iterator().next().getFactorValues();
        assertEquals( factorValues.size(), 2 );

        Iterator<FactorValue> iter = factorValues.iterator();

        factorValueA = iter.next();

        factorValueB = iter.next();

        int i = 0;
        for ( BioMaterial m : biomaterials ) {
            Collection<FactorValue> fvs = new HashSet<FactorValue>();
            if ( i % 2 == 0 )
                fvs.add( factorValueA );
            else
                fvs.add( factorValueB );

            m.setFactorValues( fvs );
            i++;
        }
    }

    /**
     * Tests the t-test with an {@link ExpressionExperiment}.
     */
    @SuppressWarnings("unchecked")
    public void testTTestWithExpressionExperiment() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        ExpressionAnalysis expressionAnalysis = analyzer.tTest( expressionExperiment, factorValueA, factorValueB );

        log.info( expressionAnalysis );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        int numResults = resultSet.getResults().size();

        assertEquals( numResults, NUM_DESIGN_ELEMENTS );

        logResults( resultSet );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 1 );

        analyzer.setAnalysisHelperService( analysisHelperService );

    }

}
