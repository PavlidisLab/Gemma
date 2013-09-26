/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.analysis.preprocess.batcheffects;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentBatchCorrectionServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private ExpressionExperimentBatchCorrectionService correctionService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Before
    public void setup() {
        cleanup();
    }

    @After
    public void teardown() {
        cleanup();
    }

    @Test
    public void testComBatOnEE() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                getTestFileBasePath( "gse18162Short" ) ) );
        ExpressionExperiment newee;
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE18162", false, true, false, false );
            newee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
        }

        assertNotNull( newee );
        newee = expressionExperimentService.thawLite( newee );
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( newee );
        try (InputStream deis = this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18162Short/design.txt" );) {
            experimentalDesignImporter.importDesign( newee, deis );
        }
        ExpressionDataDoubleMatrix comBat = correctionService.comBat( newee );
        assertNotNull( comBat );

    }

    private void cleanup() {
        ExpressionExperiment ee = expressionExperimentService.findByShortName( "GSE18162" );
        if ( ee != null ) {
            expressionExperimentService.delete( ee );
        }
    }

}
