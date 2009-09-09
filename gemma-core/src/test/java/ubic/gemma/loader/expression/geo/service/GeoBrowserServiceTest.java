package ubic.gemma.loader.expression.geo.service;

import ubic.gemma.testing.BaseSpringContextTest;

public class GeoBrowserServiceTest extends BaseSpringContextTest {

    public final void testGetDetails() throws Exception {
        GeoBrowserService gbs = ( GeoBrowserService ) this.getBean( "geoBrowserService" );

        String details = gbs.getDetails( "GSE15904" );
        assertTrue( details.contains( "GSE15904" ) );

        // log.info( details );

    }

}
