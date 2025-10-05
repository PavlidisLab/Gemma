/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.persistence.service.blacklist;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.blacklist.BlacklistedExperiment;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import static org.junit.Assert.*;

/**
 *
 *
 * @author paul
 */
public class BlacklistTest extends BaseSpringContextTest {

    @Autowired
    BlacklistedEntityService blacklistedEntityService;

    @Autowired
    ExternalDatabaseService externalDatabaseService;

    @Autowired
    GeoService geoService;

    @Test
    public void testBlacklist() {

        BlacklistedExperiment blee = new BlacklistedExperiment();
        blee.setDescription( "an experiment" );
        blee.setReason( "no good" );
        blee.setName( "the experiment" );

        String acc = "GSE" + RandomStringUtils.randomNumeric( 10 );

        blee.setShortName( acc );

        ExternalDatabase geo = externalDatabaseService.findByName( ExternalDatabases.GEO );

        DatabaseEntry d = DatabaseEntry.Factory.newInstance( acc, geo );
        blee.setExternalAccession( d );

        blacklistedEntityService.create( blee );

        assertTrue( blacklistedEntityService.isBlacklisted( acc ) );
        assertFalse( blacklistedEntityService.isBlacklisted( "imok" ) );
        try {
            geoService.fetchAndLoad( acc, false, false, false );
            fail( "Should have gotten an exception when trying to load a blacklisted experiment" );
        } catch ( IllegalArgumentException e ) {
            // OK
        }

    }

}
