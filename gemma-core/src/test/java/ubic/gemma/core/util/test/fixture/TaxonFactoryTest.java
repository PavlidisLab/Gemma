/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.core.util.test.fixture;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies the Phase 3 typed {@link TaxonFactory}.
 *
 * @see TaxonFactory
 */
public class TaxonFactoryTest extends BaseIntegrationTest {

    @Autowired
    private TaxonFactory taxonFactory;

    @Autowired
    private TaxonService taxonService;

    private final List<Taxon> adHocTaxa = new ArrayList<>();

    @After
    public void cleanUp() {
        for ( Taxon t : adHocTaxa ) {
            try {
                Taxon fresh = taxonService.load( t.getId() );
                if ( fresh != null ) {
                    taxonService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // best-effort cleanup
            }
        }
    }

    @Test
    public void mouse_returnsSeededTaxonWithNcbi10090() {
        Taxon mouse = taxonFactory.mouse();
        assertNotNull( mouse );
        assertNotNull( "seeded taxon must have a persistent id", mouse.getId() );
        assertEquals( "mouse", mouse.getCommonName() );
        assertEquals( Integer.valueOf( 10090 ), mouse.getNcbiId() );
    }

    @Test
    public void human_returnsSeededTaxonWithNcbi9606() {
        Taxon human = taxonFactory.human();
        assertNotNull( human.getId() );
        assertEquals( "human", human.getCommonName() );
        assertEquals( Integer.valueOf( 9606 ), human.getNcbiId() );
    }

    @Test
    public void seededShortcutsReturnSameRowAcrossCalls() {
        Taxon a = taxonFactory.mouse();
        Taxon b = taxonFactory.mouse();
        assertSame( "seeded resolution should return identity-equal entities", a.getId(), b.getId() );
    }

    @Test
    public void byCommonName_resolvesSeededTaxon() {
        Taxon rat = taxonFactory.byCommonName( "rat" ).build();
        assertNotNull( rat.getId() );
        assertEquals( "rat", rat.getCommonName() );
    }

    @Test
    public void byNcbiId_resolvesSeededTaxon() {
        Taxon zebrafish = taxonFactory.byNcbiId( 7955 ).build();
        assertNotNull( zebrafish.getId() );
        assertEquals( Integer.valueOf( 7955 ), zebrafish.getNcbiId() );
    }

    @Test
    public void byScientificName_resolvesSeededTaxon() {
        Taxon human = taxonFactory.byScientificName( "Homo sapiens" ).build();
        assertNotNull( human.getId() );
        assertEquals( "Homo sapiens", human.getScientificName() );
    }

    @Test
    public void byCommonName_unknownThrows() {
        try {
            taxonFactory.byCommonName( "definitely-not-a-real-taxon-zzz" ).build();
            fail( "expected IllegalStateException for missing taxon" );
        } catch ( IllegalStateException expected ) {
            assertTrue( expected.getMessage().contains( "not found" ) );
        }
    }

    @Test
    public void adHoc_defaults_returnsPersistentTaxon() {
        Taxon t = taxonFactory.adHoc().build();
        adHocTaxa.add( t );

        assertNotNull( "ad-hoc taxon should be persisted", t.getId() );
        assertNotNull( t.getScientificName() );
        assertNotNull( t.getCommonName() );
        assertNotNull( t.getNcbiId() );
        assertTrue( "default ad-hoc taxon should have genes usable", t.getIsGenesUsable() );
        assertTrue( "ad-hoc NCBI id should be in the synthetic range",
                t.getNcbiId() >= 500_000 );
    }

    @Test
    public void adHoc_withNcbiId_appliesOverride() {
        int ncbi = 999_991;
        Taxon t = taxonFactory.adHoc()
                .withNcbiId( ncbi )
                .withCommonName( "test_override_taxon" )
                .withScientificName( "Testus override_us" )
                .build();
        adHocTaxa.add( t );

        assertEquals( Integer.valueOf( ncbi ), t.getNcbiId() );
        assertEquals( "test_override_taxon", t.getCommonName() );
        assertEquals( "Testus override_us", t.getScientificName() );
    }

    @Test
    public void adHoc_withGenesUsableFalse_appliesOverride() {
        Taxon t = taxonFactory.adHoc().withGenesUsable( false ).build();
        adHocTaxa.add( t );
        assertEquals( false, t.getIsGenesUsable() );
    }
}
