/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixColumnSort;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Paul
 */
public class ContinuousVariableDiffExTest extends AbstractGeoServiceTest {

    private ExpressionExperiment ee;

    @Autowired
    private AnalysisSelectionAndExecutionService analysisService = null;

    @Autowired
    private DiffExAnalyzer analyzer;

    @Autowired
    private ExperimentalDesignImporter designImporter;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private GeoService geoService;

    @Test
    @Category(SlowTest.class)
    public void test() {
        AnalysisType aa = analysisService
                .determineAnalysis( ee, ee.getExperimentalDesign().getExperimentalFactors(), null, true );

        assertEquals( AnalysisType.GENERICLM, aa );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 1, factors.size() );
        config.setAnalysisType( aa );
        config.setFactorsToInclude( factors );

        Collection<DifferentialExpressionAnalysis> result = analyzer.run( ee, config );
        assertEquals( 1, result.size() );

        DifferentialExpressionAnalysis analysis = result.iterator().next();

        assertNotNull( analysis );

        Map<ExperimentalFactor, FactorValue> baselineLevels = ExpressionDataMatrixColumnSort
                .getBaselineLevels( ee.getExperimentalDesign().getExperimentalFactors() );

        assertEquals( 1, baselineLevels.size() );
        FactorValue fv = baselineLevels.values().iterator().next();

        assertEquals( 24.0, Double.parseDouble( fv.getMeasurement().getValue() ), 0.0001 );

        // checkResults( analysis );
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Before
    public void setUp() throws Exception {

        /*
         * this is an exon array data set that has the data present. It's an annoying choice for a test in that it
         * exposes issues with our attempts to ignore the data from exon arrays until we get it from the raw CEL files.
         */

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                FileTools.resourceToPath( "/data/analysis/expression/gse13949short" ) ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE13949", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();
        }

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> toremove = new HashSet<>( ee.getExperimentalDesign().getExperimentalFactors() );
        for ( ExperimentalFactor ef : toremove ) {
            experimentalFactorService.remove( ef );
        }

        expressionExperimentService.update( ee );

        processedExpressionDataVectorService.createProcessedDataVectors( ee, true );

        ee = expressionExperimentService.thaw( ee );

        designImporter.importDesign( ee, this.getClass()
                .getResourceAsStream( "/data/analysis/expression/gse13949short/1151_GSE13949_expdesign.data.txt" ) );

    }

}
