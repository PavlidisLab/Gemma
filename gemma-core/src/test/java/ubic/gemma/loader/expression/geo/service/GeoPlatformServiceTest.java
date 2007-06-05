/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.AbstractGeoServiceTest;

/**
 * Tests of GeoPlatformService
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoPlatformServiceTest extends AbstractGeoServiceTest {
    protected AbstractGeoService geoService;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        init();
    }

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.GeoPlatformService.fetchAndLoad(String)'
     */
    public void testFetchAndLoadGPL101Short() throws Exception {
        endTransaction();
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "platform" ) );
        geoService.fetchAndLoad( "GPL101", false, true, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.AbstractGeoServiceTest#init()
     */
    @Override
    protected void init() {
        geoService = new GeoPlatformService();
        geoService.setGeoConverter( ( GeoConverter ) getBean( "geoConverter" ) );
        geoService.setPersisterHelper( ( PersisterHelper ) getBean( "persisterHelper" ) );
    }
}
