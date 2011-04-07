/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader.expression.geo.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.List;

import org.junit.Test;

import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class GeoBrowserServiceTest extends BaseSpringContextTest {

    @Test
    public final void testGetDetails() throws Exception {
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );

        try {
            String details = gbs.getDetails( "GSE15904" );
            assertTrue( details.contains( "GSE15904" ) );

            details = gbs.getDetails( "GSE1295" );
            assertTrue( details.contains( "GSE1295" ) );

            // log.info( details );

            details = gbs.getDetails( "GSE2565" );
            assertTrue( details.contains( "GSE2565" ) );

            // occurs in a "accessioned in GEO as..."
            assertFalse( details.contains( "<strong>GPL8321" ) );
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "500" ) || e.getMessage().contains( "502" )
                    || e.getMessage().contains( "503" ) || e.getMessage().contains( "GEO returned an error" ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            if ( e.getCause() instanceof UnknownHostException || e.getCause().getMessage().contains( "500" )
                    || e.getCause().getMessage().contains( "502" ) || e.getCause().getMessage().contains( "503" ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            throw e;
        }

        // log.info( details );

    }

    @Test
    public final void testGetRecentRecords() throws Exception {
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );

        try {
            List<GeoRecord> recentGeoRecords = gbs.getRecentGeoRecords( 1010, 1 );

            if ( recentGeoRecords.size() == 0 ) {
                log.warn( "Skipping test: no GEO records returned, check test settings" );
                return;
            }

            GeoRecord rec = recentGeoRecords.iterator().next();
            int oldCount = rec.getPreviousClicks();
            String firstAccession = rec.getGeoAccession();

            // this should cause the increment.
            gbs.getDetails( firstAccession );

            recentGeoRecords = gbs.getRecentGeoRecords( 1000, 1 );

            /*
             * Do this check in case it gets filtered out.
             */
            if ( recentGeoRecords.size() == 0 ) {
                return;
            }

            rec = recentGeoRecords.iterator().next();

            if ( !rec.getGeoAccession().equals( firstAccession ) ) {
                return;
            }

            int newCount = rec.getPreviousClicks();

            assertEquals( oldCount + 1, newCount );
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "500" ) || e.getMessage().contains( "502" )
                    || e.getMessage().contains( "503" ) || e.getMessage().contains( "GEO returned an error" ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            if ( e.getCause() instanceof UnknownHostException || e.getCause().getMessage().contains( "500" )
                    || e.getCause().getMessage().contains( "502" ) || e.getCause().getMessage().contains( "503" ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            throw e;
        }
    }

    @Test
    public final void testToggleUsability() throws Exception {
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );

        boolean oneWay = gbs.toggleUsability( "GSE15904" );

        boolean sw = gbs.toggleUsability( "GSE15904" );

        assertFalse( oneWay && sw );
        assertTrue( oneWay || sw );
    }

}
