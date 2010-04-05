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
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzerTest extends BaseAnalyzerConfigurationTest {

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

        // check
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();
            log.debug( "probe: " + probe + "; p-value: " + pvalue );

            if ( probe.getName().equals( "probe_0" ) ) {
                assertEquals( 4.312e-10, pvalue, 1e-12 );
            } else if ( probe.getName().equals( "probe_4" ) ) {
                assertTrue( pvalue == null );
            } else if ( probe.getName().equals( "probe_17" ) ) {
                assertEquals( 9.604e-11, pvalue, 1e-13 );
            } else if ( probe.getName().equals( "probe_75" ) ) {
                assertEquals( 0.3523, pvalue, 0.001 );
            }
        }
    }

    /*
     * 
     */
    @Test
    public void testOneSampleTtest() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureVectors( super.biomaterials, "/data/stat-tests/onesample-ttest-data.txt" );

        configureMocks();

        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        factors.add( super.experimentalFactorA );

        /*
         * Remove factorValue from all the samples.
         */
        Iterator<FactorValue> iterator = experimentalFactorA.getFactorValues().iterator();
        FactorValue toUse = iterator.next();
        FactorValue toRemove = iterator.next();

        experimentalFactorA.getFactorValues().remove( toRemove );
        for ( BioMaterial bm : super.biomaterials ) {
            bm.getFactorValues().remove( toRemove );
            bm.getFactorValues().add( toUse );
        }

        quantitationType.setIsRatio( true ); // must be for one-sample to make sense.

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment, factors );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        int numResults = resultSet.getResults().size();

        assertEquals( numResults, NUM_DESIGN_ELEMENTS );

        // check
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();
            Double stat = probeAnalysisResult.getScore();
            log.debug( "probe: " + probe + "; p-value: " + pvalue );

            if ( probe.getName().equals( "probe_0" ) ) {
                assertEquals( 0.03505, pvalue, 0.00001 );
            } else if ( probe.getName().equals( "probe_16" ) ) {
                assertEquals( 0.03476, pvalue, 0.0001 );
            } else if ( probe.getName().equals( "probe_17" ) ) {
                assertEquals( 0.03578, pvalue, 0.0001 );
            } else if ( probe.getName().equals( "probe_75" ) ) {
                assertEquals( 0.8897, pvalue, 0.0001 );
                assertEquals( -0.1507, stat, 0.0001 );
            } else if ( probe.getName().equals( "probe_94" ) ) {
                assertEquals( 0.002717, pvalue, 0.0001 );
                assertEquals( 6.6087, stat, 0.001 );
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.BaseAnalyzerConfigurationTest#checkResults(ubic.gemma.model.analysis.expression
     * .ExpressionAnalysisResultSet)
     */
    @Override
    protected void checkResults( ExpressionAnalysisResultSet resultSet ) {
        throw new UnsupportedOperationException( "Don't use this" );
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
