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
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies the Phase 3 typed test-fixture factory for {@link BioMaterial}.
 *
 * @see BioMaterialFactory
 */
public class BioMaterialFactoryTest extends BaseIntegrationTest {

    @Autowired
    private BioMaterialFactory bioMaterialFactory;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private TaxonService taxonService;

    private final List<BioMaterial> createdBms = new ArrayList<>();

    @After
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
        assertNotNull( "BM id must be assigned by persist", bm.getId() );
        assertNotNull( bm.getName() );
        assertNotNull( "default build() should attach a source taxon", bm.getSourceTaxon() );
        assertNull( "default build() should not attach a source BM", bm.getSourceBioMaterial() );
        assertNull( "default build() should not attach an external accession", bm.getExternalAccession() );
    }

    @Test
    public void withTaxon_usesSuppliedTaxon() {
        Taxon human = taxonService.findByCommonName( "human" );
        assertNotNull( "test data must contain human taxon", human );

        BioMaterial bm = bioMaterialFactory.withTaxon( human ).build();
        createdBms.add( bm );

        assertNotNull( bm.getId() );
        assertEquals( "BM should reference the supplied taxon",
                human.getId(), bm.getSourceTaxon().getId() );
    }

    @Test
    public void withSourceBioMaterial_chainsParentCorrectly() {
        BioMaterial parent = bioMaterialFactory.build();
        createdBms.add( parent );

        BioMaterial child = bioMaterialFactory.withSourceBioMaterial( parent ).build();
        createdBms.add( child );

        assertNotNull( child.getId() );
        assertNotNull( child.getSourceBioMaterial() );
        assertEquals( "child should reference the supplied parent BM",
                parent.getId(), child.getSourceBioMaterial().getId() );
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
        assertNotNull( "withExternalAccession should attach an accession", bm.getExternalAccession() );
        assertNotNull( bm.getExternalAccession().getAccession() );
        assertNotNull( bm.getExternalAccession().getExternalDatabase() );
    }
}
