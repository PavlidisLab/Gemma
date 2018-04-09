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

package ubic.gemma.core.loader.expression.geo;

import static org.junit.Assert.*;

import org.junit.Test;

import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;

/**
 * 
 * @author paul
 */
public class GeoPlatformTest {

    @Test
    public void testExonArrayInfo() {
        assertTrue( GeoPlatform.isAffymetrixExonArray( "GPL23159" ) );
        assertTrue( GeoPlatform.isAffymetrixExonArray( "GPL20103" ) );

        assertTrue( !GeoPlatform.isAffymetrixExonArray( "GPL2315" ) );

        assertEquals( "GPL16686", GeoPlatform.exonArrayToGeneLevelArray( "GPL20103" ) );

    }

}
