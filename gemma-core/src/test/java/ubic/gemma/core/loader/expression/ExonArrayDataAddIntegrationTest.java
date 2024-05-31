/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.core.loader.expression;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.core.config.Settings;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.junit.Assume.assumeTrue;

/**
 * Uses the Affy Power Tools, and full-sized data sets.
 *
 * @author paul
 */
@Category(SlowTest.class)
@Ignore("These tests are broken and also way too slow to be included in the test suite by default.")
public class ExonArrayDataAddIntegrationTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService experimentService;

    @BeforeClass
    public static void checkAptIsAvailable() {
        String apt = Settings.getString( "affy.power.tools.exec" );
        assumeTrue( "Test skipped due to lack of Affy Power Tools executable", new File( apt ).canExecute() );
    }

    @Test
    public void testAddAffyExonArrayDataExpressionExperiment() throws Exception {
        //    mouse  GPL6096 gene level, no switch needed
        ExpressionExperiment ee;
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            Collection<?> results = geoService.fetchAndLoad( "GSE12135", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        /*
         * Add the raw data.
         */
        dataUpdater.reprocessAffyDataFromCel( ee );

        experimentService.load( ee.getId() );
    }

    @Test
    public void testAddAffyExonHuman() {
        ExpressionExperiment ee; // GSE22498,  GPL5188 - exon level, so will be switched to GPL5175 (human)
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            Collection<?> results = geoService.fetchAndLoad( "GSE22498", false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }
        dataUpdater.reprocessAffyDataFromCel( ee );
        experimentService.load( ee.getId() );
    }

    @Test
    public void testAddAffyExonRat() {
        ExpressionExperiment ee; // GSE33597 -  GPL6543, gene level so shouldn't switch (though GEO version of platform needs to be filtered)
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            Collection<?> results = geoService.fetchAndLoad( "GSE33597", false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }
        dataUpdater.reprocessAffyDataFromCel( ee );
        experimentService.load( ee.getId() );

    }

}
