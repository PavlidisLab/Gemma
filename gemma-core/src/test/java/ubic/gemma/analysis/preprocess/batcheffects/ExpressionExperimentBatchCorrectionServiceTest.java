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
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.expression.diff.BaseAnalyzerConfigurationTest;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.persistence.PersisterHelper;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentBatchCorrectionServiceTest extends AbstractGeoServiceTest {

    @Autowired
    ExpressionExperimentBatchCorrectionService correctionService;

    @Autowired
    private BatchInfoPopulationService batchInfoPopulationService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PersisterHelper persisterHelper;

    @Autowired
    protected GeoDatasetService geoService;

    @Autowired
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    ExperimentalDesignImporter experimentalDesignImporter;

    // @Test
    // final public void testComBat() throws Exception {
    //
    // ExpressionExperiment ee = super.getTestPersistentCompleteExpressionExperiment( false );
    //
    // Map<BioMaterial, Date> dates = new HashMap<BioMaterial, Date>();
    //
    // Calendar cal = Calendar.getInstance();
    // cal.set( 2004, 3, 10, 10, 1, 1 );
    // Date batch1Date = cal.getTime();
    // Date batch2Date = DateUtils.addHours( batch1Date, 270 );
    //
    // int i = 0;
    // for ( BioAssay ba : ee.getBioAssays() ) {
    // for ( BioMaterial bm : ba.getSamplesUsed() ) {
    // if ( i % 2 == 0 ) {
    // dates.put( bm, batch1Date );
    // } else {
    // dates.put( bm, batch2Date );
    // }
    // }
    // i++;
    // }
    //
    // batchInfoPopulationService.convertToFactor( ee, dates );
    //
    // ee = this.expressionExperimentService.load( ee.getId() );
    // ee = this.expressionExperimentService.thawLite( ee );
    //
    // ExpressionDataDoubleMatrix comBat = correctionService.comBat( ee );
    // assertNotNull( comBat );
    // }

    @Test
    public void testComBatOnEE() throws Exception {

        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gse18162Short" ) );
        ExpressionExperiment newee;
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE18162", false, true, false, false );
            newee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) e.getData();
        }

        assertNotNull( newee );
        newee = expressionExperimentService.thawLite( newee );
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( newee );
        InputStream deis = this.getClass().getResourceAsStream( "/data/loader/expression/geo/gse18162Short/design.txt" );
        experimentalDesignImporter.importDesign( newee, deis );

        ExpressionDataDoubleMatrix comBat = correctionService.comBat( newee );
        assertNotNull( comBat );
    }

}
