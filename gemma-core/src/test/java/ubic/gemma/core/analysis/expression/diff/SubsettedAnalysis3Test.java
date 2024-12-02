/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Paul
 */
public class SubsettedAnalysis3Test extends AbstractGeoServiceTest {

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

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Before
    public void setUp() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                FileTools.resourceToPath( "/data/analysis/expression/gse26927short" ) ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE26927", false, true, false );
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
                .getResourceAsStream( "/data/analysis/expression/gse26927short/2684_GSE26927_expdesign.data.txt" ) );

    }

    @Test
    @Category(SlowTest.class)
    public void test() {

        ee = expressionExperimentService.thawLite( ee );
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 3, factors.size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( 3, ba.getSampleUsed().getFactorValues().size() );
        }

        ExperimentalFactor organismpart = null;
        ExperimentalFactor disease = null;
        ExperimentalFactor diseasegroup = null;
        for ( ExperimentalFactor ef : factors ) {
            assertNotNull( ef.getCategory() );
            switch ( ef.getCategory().getValue() ) {
                case "study design":
                    diseasegroup = ef;
                    break;
                case "disease":
                    disease = ef;
                    break;
                case "organism part":
                    organismpart = ef;
                    break;
            }
        }
        assertNotNull( diseasegroup );
        assertNotNull( disease );
        assertNotNull( organismpart );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.getFactorsToInclude().add( disease );
        config.getFactorsToInclude().add( organismpart );

        config.setSubsetFactor( diseasegroup );

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config );
        assertEquals( 6, analyses.size() ); // a subset for each disease: SZ, PD, HD, ALS, ALZ, MS

        /*
         * Now, within each we should have only one disease contrast,
         */
        for ( DifferentialExpressionAnalysis analysis : analyses ) {

            // there should be one for disease - tissue isn't used.
            assertEquals( 1, analysis.getResultSets().size() );

            for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
                ExperimentalFactor factor = rs.getExperimentalFactors().iterator().next();
                //noinspection LoopStatementThatDoesntLoop
                for ( DifferentialExpressionAnalysisResult res : rs.getResults() ) {
                    Collection<ContrastResult> contrasts = res.getContrasts();

                    for ( ContrastResult cr : contrasts ) {
                        log.info( analysis + "   " + factor + " " + cr + " " + res );
                    }
                    break;
                }
            }

        }

    }
}
