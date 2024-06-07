/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.core.analysis.expression.diff;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * Test based on GSE19480, see bug 3177
 *
 * @author paul
 */
public class LowVarianceDataTest extends AbstractGeoServiceTest {

    @Autowired
    private AnalysisSelectionAndExecutionService analysisService = null;

    @Autowired
    private DiffExAnalyzer analyzer;

    @Autowired
    private ExperimentalDesignImporter designImporter;

    private ExpressionExperiment ee;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private GeoService geoService;

    public void checkResults( DifferentialExpressionAnalysis analysis ) {
        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        assertEquals( 2, resultSets.size() );

        boolean found1 = false;

        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

            // lose rows that have zero variance or lots of 'thresholded' values, but number of values is either 88 or
            // 86 (factors are not the same)

            for ( DifferentialExpressionAnalysisResult r : results ) {
                CompositeSequence probe = r.getProbe();

                log.info( probe.getName() + " " + r.getPvalue() );

                switch ( probe.getName() ) {
                    case "1552338_at":
                        fail( "Should not have found a result for constant probe: 1552338_at" );
                        break;
                    case "1552337_s_at":
                        fail( "Should not have found a result for constant probe: 1552337_s_at" );
                        break;
//                    case "1552391_at":
//                        fail( "Should not have found a result for constant probe: 1552391_at" );
//                        break;
                    default:
                        found1 = true;
                        break;
                }
            }

        }

        assertTrue( found1 );
    }

    @Before
    public void setUp() throws Exception {

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/expression" ) ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE19420", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();

        }

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> toremove = new HashSet<>( ee.getExperimentalDesign().getExperimentalFactors() );
        experimentalFactorService.remove( toremove );
        ee.getExperimentalDesign().getExperimentalFactors().clear();

        expressionExperimentService.update( ee );

        processedExpressionDataVectorService.computeProcessedExpressionData( ee );

        ee = expressionExperimentService.thaw( ee );

        designImporter.importDesign( ee,
                this.getClass().getResourceAsStream( "/data/analysis/expression/2976_GSE19420_expdesign.data.txt" ) );

    }

    @Test
    @Category(SlowTest.class)
    public void test() {
        ee = expressionExperimentService.thawLite( ee );

        AnalysisType aa = analysisService
                .determineAnalysis( ee, ee.getExperimentalDesign().getExperimentalFactors(), null, true );

        assertEquals( AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION, aa );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 2, factors.size() );
        config.setAnalysisType( aa );
        config.setFactorsToInclude( factors );

        Collection<DifferentialExpressionAnalysis> result = analyzer.run( ee, config );
        assertEquals( 1, result.size() );

        DifferentialExpressionAnalysis analysis = result.iterator().next();

        this.checkResults( analysis );

    }
}
