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

package ubic.gemma.loader.expression;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.Settings;

/**
 * Uses the Affy Power Tools, and full-sized data sets.
 * 
 * @author paul
 * @version $Id$
 */
public class ExonArrayDataAddIntegrationTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService experimentService;

    private boolean hasApt = false;

    @org.junit.Before
    public void startup() {
        String apt = Settings.getString( "affy.power.tools.exec" );
        if ( new File( apt ).canExecute() ) {
            hasApt = true;
        }
    }

    /**
     * Test method for .
     */
    @Test
    public void testAddAffyExonArrayDataExpressionExperiment() throws Exception {

        if ( !hasApt ) {
            log.warn( "Test skipped due to lack of Affy Power Tools executable" );
            return;
        }

        ExpressionExperiment ee;
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath() ) );
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

    /**
     * 
     */
    @Test
    public void testAddAffyExonHuman() {
        if ( !hasApt ) {
            log.warn( "Test skipped due to lack of Affy Power Tools executable" );
            return;
        }
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

    /**
     * 
     */
    @Test
    public void testAddAffyExonRat() {
        if ( !hasApt ) {
            log.warn( "Test skipped due to lack of Affy Power Tools executable" );
            return;
        }
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

}
