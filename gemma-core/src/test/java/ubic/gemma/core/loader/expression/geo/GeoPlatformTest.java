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

import org.junit.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;

import static org.junit.Assert.*;

/**
 * 
 * @author paul
 */
public class GeoPlatformTest {

    @Test
    public void testExonArrayInfo() {
        assertTrue( GeoPlatform.isGEOAffyDataUsable( "GPL23159" ) );
        assertTrue( GeoPlatform.isGEOAffyDataUsable( "GPL20103" ) ); // an exon array, though not the usual one

        assertTrue( GeoPlatform.isGEOAffyDataUsable( "GPL23159" ) ); // this is an MPS array, but the right one

        assertFalse( GeoPlatform.isGEOAffyDataUsable( "GPL13712" ) ); // exon array, though this is gene-level data, we wouldn't load it.

        assertEquals( "GPL16686", GeoPlatform.alternativeToProperAffyPlatform( "GPL20103" ) );

    }

}
