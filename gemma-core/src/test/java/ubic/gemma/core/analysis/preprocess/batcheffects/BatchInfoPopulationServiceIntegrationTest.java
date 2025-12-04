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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExperimentFactorUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test fetching and loading the batch information from raw files. Test takes around 10-15 minutes if the files are not
 * downloaded first.
 *
 * @author paul
 */
public class BatchInfoPopulationServiceIntegrationTest extends AbstractGeoServiceTest {

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    @Autowired
    private GeoService geoService;

    @Autowired
    private BatchInfoPopulationService batchInfoPopulationService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = EntrezUtils.ESEARCH)
    public void testLoad() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
        ExpressionExperiment newee;
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE26903", false, true, false );
            newee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
        }

        assertNotNull( newee );

        batchInfoPopulationService.fillBatchInformation( newee, true );

    }

    /*
     * Another Affymetrix format - GCOS
     */
    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = EntrezUtils.ESEARCH)
    public void testLoadCommandConsoleFormat() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
        ExpressionExperiment newee;
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE20219", false, true, false );
            newee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
        }

        assertNotNull( newee );
        batchInfoPopulationService.fillBatchInformation( newee, true );

        newee = eeService.thawLite( newee );

        for ( ExperimentalFactor ef : newee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getName().equals( ExperimentFactorUtils.BATCH_FACTOR_NAME ) ) {
                for ( FactorValue fv : ef.getFactorValues() ) {
                    assertNotNull( fv.getValue() );
                    assertTrue( fv.getValue().startsWith( "Batch_0" ) ); // Batch_01, Batch_02 etc.
                }
            }
        }

    }
}
