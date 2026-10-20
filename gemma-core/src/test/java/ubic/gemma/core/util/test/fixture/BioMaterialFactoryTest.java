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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies the Phase 3 typed test-fixture factory for {@link BioMaterial}.
 *
 * @see BioMaterialFactory
 */
public class BioMaterialFactoryTest extends BaseIntegrationTest5 {

    @Autowired
    private BioMaterialFactory bioMaterialFactory;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private TaxonService taxonService;

    private final List<BioMaterial> createdBms = new ArrayList<>();

    @AfterEach
    public void cleanUp() {
        // best-effort cleanup; iterate children before parents
        for ( int i = createdBms.size() - 1; i >= 0; i-- ) {
            BioMaterial bm = createdBms.get( i );
            try {
                BioMaterial fresh = bioMaterialService.load( bm.getId() );
                if ( fresh != null ) {
                    bioMaterialService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // ignore — fixture cleanup
            }
        }
    }

    @Test
    public void build_defaults_returnsPersistentBmWithId() {
        BioMaterial bm = bioMaterialFactory.build();
        createdBms.add( bm );

        assertNotNull( bm );
        assertNotNull( bm.getId(), "BM id must be assigned by persist" );
        assertNotNull( bm.getName() );
        assertNotNull( bm.getSourceTaxon(), "default build() should attach a source taxon" );
        assertNull( bm.getSourceBioMaterial(), "default build() should not attach a source BM" );
        assertNull( bm.getExternalAccession(), "default build() should not attach an external accession" );
    }

    @Test
    public void withTaxon_usesSuppliedTaxon() {
        Taxon human = taxonService.findByCommonName( "human" );
        assertNotNull( human, "test data must contain human taxon" );

        BioMaterial bm = bioMaterialFactory.withTaxon( human ).build();
        createdBms.add( bm );

        assertNotNull( bm.getId() );
        assertEquals( human.getId(), bm.getSourceTaxon().getId(),
                "BM should reference the supplied taxon" );
    }

    @Test
    public void withSourceBioMaterial_chainsParentCorrectly() {
        BioMaterial parent = bioMaterialFactory.build();
        createdBms.add( parent );

        BioMaterial child = bioMaterialFactory.withSourceBioMaterial( parent ).build();
        createdBms.add( child );

        assertNotNull( child.getId() );
        assertNotNull( child.getSourceBioMaterial() );
        assertEquals( parent.getId(), child.getSourceBioMaterial().getId(),
                "child should reference the supplied parent BM" );
    }

    @Test
    public void withSourceBioMaterial_rejectsTransientParent() {
        BioMaterial transientParent = BioMaterial.Factory.newInstance();
        transientParent.setName( "not persistent" );
        try {
            bioMaterialFactory.withSourceBioMaterial( transientParent );
            fail( "expected IllegalArgumentException for a transient parent" );
        } catch ( IllegalArgumentException expected ) {
            assertTrue( expected.getMessage().contains( "persistent" ) );
        }
    }

    @Test
    public void withExternalAccession_attachesGeoAccession() {
        BioMaterial bm = bioMaterialFactory.withExternalAccession().build();
        createdBms.add( bm );

        assertNotNull( bm.getId() );
        assertNotNull( bm.getExternalAccession(), "withExternalAccession should attach an accession" );
        assertNotNull( bm.getExternalAccession().getAccession() );
        assertNotNull( bm.getExternalAccession().getExternalDatabase() );
    }
}
