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

package ubic.gemma.loader.expression.geo;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class DataUpdaterTest extends AbstractGeoServiceTest {
    @Autowired
    private GeoService geoService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService experimentService;

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.geo.DataUpdater#addAffyExonArrayData(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     * .
     */
    @Test
    public void testAddAffyExonArrayDataExpressionExperiment() throws Exception {
        ExpressionExperiment ee;
        try {
            String path = getTestFileBasePath();
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE12135", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        /*
         * Add the raw data.
         */
        dataUpdater.addAffyExonArrayData( ee );

        ee = experimentService.load( ee.getId() );
    }

    @Test
    public void testAddAffyExonHuman() throws Exception {
        ExpressionExperiment ee; // GSE22498
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            Collection<?> results = geoService.fetchAndLoad( "GSE22498", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }
        dataUpdater.addAffyExonArrayData( ee );
        ee = experimentService.load( ee.getId() );
    }

    @Test
    public void testAddAffyExonRat() throws Exception {
        ExpressionExperiment ee; // GSE33597
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            Collection<?> results = geoService.fetchAndLoad( "GSE33597", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }
        dataUpdater.addAffyExonArrayData( ee );
        ee = experimentService.load( ee.getId() );

    }

    @Test
    public void testReplaceData() {
        // not implemented yet
        assertTrue( true );
    }

}
