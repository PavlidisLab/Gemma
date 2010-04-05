/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author paul
 * @version $Id$
 */
public class AncovaTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    private AncovaAnalyzer analyzer;

    /**
     * @throws Exception
     */
    @Test
    public void testAncovaA() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment,
                super.experimentalFactors );

        assertNotNull( expressionAnalysis );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 2, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

            Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactor();

            assertEquals( 1, factors.size() );

            for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

                ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
                CompositeSequence probe = probeAnalysisResult.getProbe();
                Double pvalue = probeAnalysisResult.getPvalue();
                Double stat = probeAnalysisResult.getScore();

                if ( pvalue != null ) assertNotNull( stat );
                assertNotNull( probe );

                log.debug( "probe: " + probe + "; p-value: " + pvalue + "; T=" + stat );

                ExperimentalFactor f = factors.iterator().next();

                if ( f.equals( super.experimentalFactorA ) ) {

                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.921, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 4.69e-11, pvalue, 1e-12 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        assertEquals( 0.00506, pvalue, 0.0001 );
                        assertEquals( 125.746, stat, 0.001 );
                    }

                } else {
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.642, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.196, pvalue, 0.001 );
                    }
                }

            }
        }
    }

    /**
     * With a continuous covariate + two categorical
     * 
     * @throws Exception
     */
    @Test
    public void testAncovaB() throws Exception {
        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        /*
         * Add a continuous factor
         */
        ExperimentalFactor experimentalFactorC = ExperimentalFactor.Factory.newInstance();
        experimentalFactorC.setName( "confabulatiliationity" );
        experimentalFactorC.setId( 5399424551L );
        for ( int i = 1; i <= 8; i++ ) {

            FactorValue factorValueC = FactorValue.Factory.newInstance();
            factorValueC.setId( 2000L + i );

            factorValueC.setMeasurement( Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, "" + i,
                    PrimitiveType.DOUBLE ) );

            factorValueC.setExperimentalFactor( experimentalFactorC );

            assert !biomaterials.get( i - 1 ).getFactorValues().contains( factorValueC );
            super.biomaterials.get( i - 1 ).getFactorValues().add( factorValueC );

            experimentalFactorC.getFactorValues().add( factorValueC );
        }

        expressionExperiment.getExperimentalDesign().getExperimentalFactors().add( experimentalFactorC );

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment, expressionExperiment
                .getExperimentalDesign().getExperimentalFactors() );

        assertNotNull( expressionAnalysis );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 3, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

            Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactor();

            assertEquals( 1, factors.size() );

            for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

                ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
                CompositeSequence probe = probeAnalysisResult.getProbe();
                Double pvalue = probeAnalysisResult.getPvalue();
                Double stat = probeAnalysisResult.getScore();

                if ( pvalue != null ) assertNotNull( stat );
                assertNotNull( probe );

                log.info( "probe: " + probe + "; Factor="
                        + resultSet.getExperimentalFactor().iterator().next().getName() + "; p-value: " + pvalue
                        + "; T=" + stat );

                ExperimentalFactor f = factors.iterator().next();

                if ( f.equals( super.experimentalFactorA ) ) {

                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.520018, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 1.11e-06, pvalue, 1e-8 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        // too few samples
                        assertEquals( null, pvalue );
                        assertEquals( null, stat );
                    }

                } else if ( f.equals( super.experimentalFactorB ) ) {
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.684442, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.94301, pvalue, 0.001 );
                        assertEquals( -0.076, stat, 0.001 );
                    }
                } else {
                    // our new one.
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.497665, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.57347, pvalue, 0.001 );
                    }
                }

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
        throw new UnsupportedOperationException( "don't use this here" );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 1 );

        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );

    }

}
