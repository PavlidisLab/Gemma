package ubic.gemma.loader.expression.geo.service;

import java.util.List;

import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.testing.BaseSpringContextTest;

public class GeoBrowserServiceTest extends BaseSpringContextTest {

    public final void testGetDetails() throws Exception {
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );

        String details = gbs.getDetails( "GSE15904" );
        assertTrue( details.contains( "GSE15904" ) );

        details = gbs.getDetails( "GSE1295" );
        assertTrue( details.contains( "GSE1295" ) );

        // log.info( details );

        details = gbs.getDetails( "GSE2565" );
        assertTrue( details.contains( "GSE2565" ) );

        // occurs in a "accessioned in GEO as..."
        assertFalse( details.contains( "<strong>GPL8321" ) );

        // log.info( details );

    }

    public final void testToggleUsability() throws Exception {
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );

        boolean oneWay = gbs.toggleUsability( "GSE15904" );

        boolean sw = gbs.toggleUsability( "GSE15904" );

        assertFalse( oneWay && sw );
        assertTrue( oneWay || sw );
    }

    public final void testGetRecentRecords() throws Exception {
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );
        List<GeoRecord> recentGeoRecords = gbs.getRecentGeoRecords( 1000, 1 );

        if ( recentGeoRecords.size() == 0 ) {
            log.warn( "Skipping test: no GEO records returned, check test settings" );
            return;
        }

        GeoRecord rec = recentGeoRecords.iterator().next();
        int oldCount = rec.getPreviousClicks();
        String firstAccession = rec.getGeoAccession();
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

        assertTrue( oldCount + 1 == newCount );

    }

}
