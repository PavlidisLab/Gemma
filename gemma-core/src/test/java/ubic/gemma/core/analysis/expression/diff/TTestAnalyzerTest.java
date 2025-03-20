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
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * See test/data/stat-tests/README.txt for R code.
 *
 * @author keshav
 */
public class TTestAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    private DiffExAnalyzer analyzer;

    @Test
    public void testOneSampleTtest() throws Exception {

        assumeTrue( "Could not establish R connection.  Skipping test ...", connected );

        this.configureVectors( super.biomaterials, "/data/stat-tests/onesample-ttest-data.txt" );

        Collection<ExperimentalFactor> factors = new HashSet<>();
        factors.add( super.experimentalFactorA_Area );

        /*
         * Remove factorValue from all the samples.
         */
        Iterator<FactorValue> iterator = experimentalFactorA_Area.getFactorValues().iterator();
        FactorValue toUse = iterator.next();
        FactorValue toRemove = iterator.next();

        experimentalFactorA_Area.getFactorValues().remove( toRemove );
        for ( BioMaterial bm : super.biomaterials ) {
            bm.getFactorValues().remove( toRemove );
            bm.getFactorValues().add( toUse );
        }

        quantitationType.setIsRatio( true ); // must be for one-sample to make sense.
        quantitationType.setScale( ScaleType.LOG2 );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( factors );
        config.setModerateStatistics( false );
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, dmatrix, config );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        assertNull( resultSet.getBaselineGroup() );

        int numResults = resultSet.getResults().size();

        assertEquals( BaseAnalyzerConfigurationTest.NUM_DESIGN_ELEMENTS, numResults );

        // check
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            CompositeSequence probe = r.getProbe();
            Double pvalue = r.getPvalue();
            // Double stat = probeAnalysisResult.getEffectSize();
            log.debug( "probe: " + probe + "; p-value: " + pvalue );

            assertNotNull( pvalue );

            switch ( probe.getName() ) {
                case "probe_0":
                    assertEquals( 0.03505, pvalue, 0.00001 );
                    break;
                case "probe_16":
                    assertEquals( 0.03476, pvalue, 0.0001 );
                    break;
                case "probe_17":
                    assertEquals( 0.03578, pvalue, 0.0001 );
                    break;
                case "probe_75":
                    assertEquals( 0.8897, pvalue, 0.0001 );
                    // assertEquals( -0.1507, stat, 0.0001 );
                    break;
                case "probe_94":
                    assertEquals( 0.002717, pvalue, 0.0001 );
                    // assertEquals( 6.6087, stat, 0.001 );
                    break;
            }
        }
    }

    /**
     * Tests the t-test with an {@link ExpressionExperiment}.
     */
    @Test
    public void testTTestWithExpressionExperiment() {
        assumeTrue( "Could not establish R connection.  Skipping test ...", connected );

        Collection<ExperimentalFactor> factors = new HashSet<>();
        factors.add( super.experimentalFactorA_Area );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( factors );
        config.setModerateStatistics( false );
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, dmatrix, config );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();
        ExpressionAnalysisResultSet resultSet = resultSets.iterator().next();

        int numResults = resultSet.getResults().size();

        assertEquals( BaseAnalyzerConfigurationTest.NUM_DESIGN_ELEMENTS, numResults );

        assertEquals( factorValueA2, resultSet.getBaselineGroup() );

        Collection<HitListSize> hitListSizes = resultSet.getHitListSizes();
        assertEquals( 3 * 5, hitListSizes.size() );
        // for ( HitListSize hitListSize : hitListSizes ) {
        // // TODO explicitly check these counts.
        // log.info( hitListSize.getDirection() + " " + hitListSize.getThresholdQvalue() + " "
        // + hitListSize.getNumberOfProbes() )
        // + hitListSize.getNumberOfGenes());
        // }

        // check
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            CompositeSequence probe = r.getProbe();

            Double pvalue = r.getPvalue();

            assertNotNull( pvalue );

            Collection<ContrastResult> contrasts = r.getContrasts();
            Double stat;
            if ( contrasts.isEmpty() ) {
                continue;
            }

            stat = contrasts.iterator().next().getTstat();

            log.debug( "probe: " + probe + "; p-value: " + pvalue );

            switch ( probe.getName() ) {
                case "probe_0":
                    assertEquals( 1.48e-13, pvalue, 1e-15 );
                    assertNotNull( stat );
                    assertEquals( -277.4, stat, 0.1 );
                    break;
                case "probe_4":
                    assertEquals( 0.0001523, pvalue, 0.000001 );
                    break;
                case "probe_17":
                    assertEquals( 8.832e-12, pvalue, 1e-15 );
                    break;
                case "probe_75":
                    assertEquals( 0.2483, pvalue, 0.001 );
                    break;
            }
        }
    }
}
