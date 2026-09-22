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

import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 
 * @author paul
 */
public class GeoPlatformTest {

    @Test
    public void testExonArrayInfo() {
        assertTrue( GeoPlatform.isGEOAffyDataUsable( "GPL23159" ) );
        assertFalse( GeoPlatform.isGEOAffyDataUsable( "GPL20103" ) ); // alternative;an exon array, though not the usual one

        assertTrue( GeoPlatform.isGEOAffyDataUsable( "GPL23159" ) ); // this is an MPS array, but the right one

        assertFalse( GeoPlatform.isGEOAffyDataUsable( "GPL13712" ) ); // exon array, though this is gene-level data, we wouldn't load it.

        assertEquals( "GPL16686", GeoPlatform.alternativeToProperAffyPlatform( "GPL20103" ) );

    }

    /**
     * Two custom-CDF platforms affyFromCel refused as "not recognized as an Affymetrix platform" until they were
     * mapped (FRB, 2026-09-22: GSE286181 on GPL35276, GSE227261 on GPL33245). Both resolve to the array their own
     * GEO title names in brackets, which is the chip type their CEL files carry.
     */
    @Test
    public void testBrainarrayAlternativesResolveToTheirAffymetrixArray() {
        // [RAE230A] Affymetrix Rat Expression Set 230 [CDF: Brainarray Entrez Gene version 25.0]
        assertTrue( GeoPlatform.isAffyPlatform( "GPL35276" ) );
        assertEquals( "GPL341", GeoPlatform.alternativeToProperAffyPlatform( "GPL35276" ) );
        // [Clariom_S_Human] Affymetrix Clariom S Human array [ClariomSHuman_Hs_ENTREZG_23.0.0]
        assertTrue( GeoPlatform.isAffyPlatform( "GPL33245" ) );
        assertEquals( "GPL23159", GeoPlatform.alternativeToProperAffyPlatform( "GPL33245" ) );
    }

    /**
     * GPL24242 is Affymetrix's HT plate format of the Clariom S Mouse array, not an alternative CDF of it: it
     * resolves to ITSELF and carries its own library set. Its CEL files declare
     * {@code affymetrix-array-type=Clariom_S_Mouse_HT} (checked on GSE167387 and GSE254912, 2026-09-22), and
     * GPL23038's library declares {@code #%chip_type=Clariom_S_Mouse}, so resolving it there would hand apt a
     * library for a different chip.
     */
    @Test
    public void testClariomSMouseHTResolvesToItselfRatherThanTheNonHTArray() {
        assertTrue( GeoPlatform.isAffyPlatform( "GPL24242" ) );
        assertEquals( "GPL24242", GeoPlatform.alternativeToProperAffyPlatform( "GPL24242" ) );
        assertTrue( GeoPlatform.isGEOAffyDataUsable( "GPL24242" ) );
    }

}
