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
package ubic.gemma.core.analysis.expression.diff;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the one way anova analyzer. See test/data/stat-tests/README.txt for R code.
 *
 * @author keshav
 */
public class OneWayAnovaAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    private LinearModelAnalyzerImpl analyzer = null;

    /**
     * Tests the OneWayAnova method.
     */
    @Test
    public void testOneWayAnova() throws Exception {

        assumeTrue( "Could not establish R connection.  Skipping test ...", connected );

        super.configureTestDataForOneWayAnova();

        this.configureMocks();
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        Collection<ExperimentalFactor> factors = expressionExperiment.getExperimentalDesign().getExperimentalFactors();
        config.setFactorsToInclude( factors );
        config.setModerateStatistics( false );
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, config );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();
        int numResults = resultSet.getResults().size();

        assertEquals( 100, numResults );

        /*
         * Check we got the histograms - only happens during persisting.
         */
        // File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( expressionExperiment.getShortName()
        // );
        //
        // File histFile = new File( dir, expressionExperiment.getShortName() + ".pvalues"
        // + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX );
        // assertTrue( histFile.exists() );
    }

    @Test
    public void testOnewayAnovaB() throws Exception {

        assumeTrue( "Could not establish R connection.  Skipping test ...", connected );

        super.configureTestDataForOneWayAnova();

        this.configureMocks();
        /*
         * Add a factor with three levels
         */
        ExperimentalFactor experimentalFactorC = ExperimentalFactor.Factory.newInstance();
        experimentalFactorC.setName( "groupash" );
        experimentalFactorC.setId( 5399424551L );
        experimentalFactorC.setType( FactorType.CATEGORICAL );
        expressionExperiment.getExperimentalDesign().getExperimentalFactors().add( experimentalFactorC );

        FactorValue controlGroup = null;
        for ( int i = 1; i <= 3; i++ ) {
            FactorValue f = FactorValue.Factory.newInstance();
            f.setId( 2000L + i );
            if ( i != 2 ) {
                f.setValue( i + "_group" );
            } else {
                f.setValue( "control_group" );
                controlGroup = f;
            }
            f.setExperimentalFactor( experimentalFactorC );
            experimentalFactorC.getFactorValues().add( f );
        }

        List<FactorValue> facV = new ArrayList<>( experimentalFactorC.getFactorValues() );
        for ( int i = 0; i < 8; i++ ) {
            super.biomaterials.get( i ).getFactorValues().add( facV.get( i % 3 ) );
        }

        Collection<ExperimentalFactor> factors = new HashSet<>();
        factors.add( experimentalFactorC );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( factors );
        config.setModerateStatistics( false );
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, config );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();
        int numResults = resultSet.getResults().size();

        assertEquals( 100, numResults );

        assertEquals( controlGroup, resultSet.getBaselineGroup() );

        factors = resultSet.getExperimentalFactors();

        assertEquals( 1, factors.size() );

        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            CompositeSequence probe = r.getProbe();
            Double pvalue = r.getPvalue();

            // if ( pvalue != null ) assertNotNull( stat );
            assertNotNull( probe );

            // log.debug( "probe: " + probe + "; Factor=" +
            // resultSet.getExperimentalFactors().iterator().next().getName()
            // + "; p-value: " + pvalue + "; T=" + stat );

            switch ( probe.getName() ) {
                case "probe_98":
                    assertEquals( 0.1604, pvalue, 0.001 );
                    break;
                case "probe_10":
                    assertEquals( 0.8014, pvalue, 0.0001 );
                    break;
                case "probe_4":
                    assertEquals( 0.6531, pvalue, 0.0001 );
                    break;
            }

        }

    }

    private void configureMocks() {

        this.configureMockAnalysisServiceHelper();
        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );

    }

}
