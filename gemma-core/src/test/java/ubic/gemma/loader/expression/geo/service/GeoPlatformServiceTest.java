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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;

/**
 * Tests of GeoPlatformService
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoPlatformServiceTest extends AbstractGeoServiceTest {

    @Autowired
    GeoService geoService;

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.GeoPlatformService.fetchAndLoad(String)'
     */
    @Test
    public void testFetchAndLoadGPL101Short() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "platform" ) ) );
        geoService.fetchAndLoad( "GPL101", true, true, false, false );
    }
}
