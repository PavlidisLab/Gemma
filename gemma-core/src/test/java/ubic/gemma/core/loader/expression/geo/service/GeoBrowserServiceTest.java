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
package ubic.gemma.core.loader.expression.geo.service;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.GeoTest;

import java.net.UnknownHostException;
import java.util.List;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * @author paul
 */
@Category(GeoTest.class)
public class GeoBrowserServiceTest extends BaseSpringContextTest {
    @Autowired
    GeoBrowserService gbs;

    @Test
    public final void testGetDetails() throws Exception {
        assumeThatResourceIsAvailable( "https://www.ncbi.nlm.nih.gov/geo/browse/" );

        try {
            String details = gbs.getDetails( "GSE15904", "" );
            assertTrue( "Got: " + details, details.contains( "GSE15904" ) );

            Thread.sleep( 400 );

            details = gbs.getDetails( "GSE1295", "" );
            assertTrue( "Got: " + details, details.contains( "GSE1295" ) );
            Thread.sleep( 400 );

            // log.info( details );
            details = gbs.getDetails( "GSE2565", "" );
            assertTrue( "Got: " + details, details.contains( "GSE2565" ) );

            // occurs in a "accessioned in GEO as..."
            assertFalse( "Got: " + details, details.contains( "<strong>GPL8321" ) );
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "500" ) || e.getMessage().contains( "502" ) || e.getMessage()
                    .contains( "503" ) || e.getMessage().contains( "GEO returned an error" ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            if ( e.getCause() != null && ( e.getCause() instanceof UnknownHostException || e.getCause().getMessage()
                    .contains( "500" ) || e.getCause().getMessage().contains( "502" )
                    || e.getCause().getMessage()
                    .contains( "503" ) ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            throw e;
        }

        // log.info( details );

    }

    @Test
    public final void testGetRecentRecords() throws Exception {
        assumeThatResourceIsAvailable( "https://www.ncbi.nlm.nih.gov/geo/browse/" );

        try {
            // I changed the skip because the very newest records can cause a problem with fetching details.
            List<GeoRecord> recentGeoRecords = gbs.getRecentGeoRecords( 100, 10 );

            if ( recentGeoRecords.isEmpty() ) {
                log.warn( "Skipping test: no GEO records returned, check test settings" );
                return;
            }

            GeoRecord rec = recentGeoRecords.iterator().next();
            int oldCount = rec.getPreviousClicks();
            String firstAccession = rec.getGeoAccession();

            // this should cause the increment.
            gbs.getDetails( firstAccession, "" );

            recentGeoRecords = gbs.getRecentGeoRecords( 11, 10 );

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
            if ( e.getMessage().contains( "500" ) || e.getMessage().contains( "502" ) || e.getMessage()
                    .contains( "503" ) || e.getMessage().contains( "GEO returned an error" ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            if ( e.getCause() != null && ( e.getCause() instanceof UnknownHostException || e.getCause().getMessage()
                    .contains( "500" ) || e.getCause().getMessage().contains( "502" )
                    || e.getCause().getMessage()
                    .contains( "503" ) ) ) {
                log.warn( "NCBI returned error, skipping test" );
                return;
            }
            throw e;
        }
    }

    @Test
    public final void testToggleUsability() {

        boolean oneWay = gbs.toggleUsability( "GSE15904" );

        boolean sw = gbs.toggleUsability( "GSE15904" );

        assertFalse( oneWay && sw );
        assertTrue( oneWay || sw );
    }

}
