package ubic.gemma.loader.expression.geo.service;

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

}
