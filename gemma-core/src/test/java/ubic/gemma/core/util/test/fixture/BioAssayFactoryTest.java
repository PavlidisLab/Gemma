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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Verifies the Phase 3 typed {@link BioAssayFactory}.
 *
 * @see BioAssayFactory
 */
public class BioAssayFactoryTest extends BaseIntegrationTest {

    @Autowired
    private BioAssayFactory bioAssayFactory;

    @Autowired
    private ArrayDesignFactory arrayDesignFactory;

    @Autowired
    private TaxonFactory taxonFactory;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private final List<BioAssay> createdBAs = new ArrayList<>();
    private final List<BioMaterial> createdBMs = new ArrayList<>();
    private final List<ArrayDesign> createdADs = new ArrayList<>();

    @After
    public void cleanUp() {
        for ( BioAssay ba : createdBAs ) {
            try {
                BioAssay fresh = bioAssayService.load( ba.getId() );
                if ( fresh != null ) {
                    bioAssayService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // best-effort
            }
        }
        for ( BioMaterial bm : createdBMs ) {
            try {
                BioMaterial fresh = bioMaterialService.load( bm.getId() );
                if ( fresh != null ) {
                    bioMaterialService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // best-effort
            }
        }
        for ( ArrayDesign ad : createdADs ) {
            try {
                ArrayDesign fresh = arrayDesignService.load( ad.getId() );
                if ( fresh != null ) {
                    arrayDesignService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // best-effort
            }
        }
    }

    @Test
    public void defaults_returnPersistentBA_withAutoCreatedBMAndAD() {
        BioAssay ba = bioAssayFactory.builder().build();
        createdBAs.add( ba );

        assertNotNull( "BA must be persisted (id assigned)", ba.getId() );
        assertNotNull( ba.getName() );
        assertNotNull( "default BA should have a BioMaterial", ba.getSampleUsed() );
        assertNotNull( "auto-created BM should be persisted", ba.getSampleUsed().getId() );
        assertNotNull( "default BA should have an ArrayDesign", ba.getArrayDesignUsed() );
        assertNotNull( "auto-created AD should be persisted", ba.getArrayDesignUsed().getId() );
        assertNotNull( "default BA should attach a GEO accession", ba.getAccession() );

        createdBMs.add( ba.getSampleUsed() );
        createdADs.add( ba.getArrayDesignUsed() );
    }

    @Test
    public void withCustomBioMaterial_appliesOverride() {
        Taxon mouse = taxonFactory.mouse();
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( "custom_bm_for_ba_test" );
        bm.setSourceTaxon( mouse );
        BioMaterial persistedBm = bioMaterialService.create( bm );

        BioAssay ba = bioAssayFactory.builder()
                .withBioMaterial( persistedBm )
                .build();
        createdBAs.add( ba );
        createdBMs.add( persistedBm );
        createdADs.add( ba.getArrayDesignUsed() );

        assertSame( "BA should reference the supplied BM",
                persistedBm.getId(), ba.getSampleUsed().getId() );
    }

    @Test
    public void withCustomArrayDesign_appliesOverride() {
        ArrayDesign ad = arrayDesignFactory.oneColor().build();
        createdADs.add( ad );

        BioAssay ba = bioAssayFactory.builder()
                .withArrayDesign( ad )
                .build();
        createdBAs.add( ba );
        createdBMs.add( ba.getSampleUsed() );

        assertEquals( "BA should reference the supplied AD",
                ad.getId(), ba.getArrayDesignUsed().getId() );
    }

    @Test
    public void withName_appliesOverride() {
        String name = "named_ba_test_fixture";
        BioAssay ba = bioAssayFactory.builder()
                .withName( name )
                .build();
        createdBAs.add( ba );
        createdBMs.add( ba.getSampleUsed() );
        createdADs.add( ba.getArrayDesignUsed() );

        assertEquals( name, ba.getName() );
        assertNotNull( ba.getAccession() );
        assertEquals( "default accession should mirror the BA name",
                name, ba.getAccession().getAccession() );
    }

    @Test
    public void withAccessionFalse_skipsAccession() {
        BioAssay ba = bioAssayFactory.builder()
                .withAccession( false )
                .build();
        createdBAs.add( ba );
        createdBMs.add( ba.getSampleUsed() );
        createdADs.add( ba.getArrayDesignUsed() );

        assertNull( "accession should be omitted when withAccession(false)",
                ba.getAccession() );
    }

    @Test
    public void buildTransient_returnsUnpersistedBA_withPersistentBMAndAD() {
        BioAssay ba = bioAssayFactory.builder().buildTransient();

        assertNull( "buildTransient should not persist the BA", ba.getId() );
        assertNotNull( "BM should still be persisted (no cascade)",
                ba.getSampleUsed().getId() );
        assertNotNull( "AD should still be persisted (no cascade)",
                ba.getArrayDesignUsed().getId() );

        createdBMs.add( ba.getSampleUsed() );
        createdADs.add( ba.getArrayDesignUsed() );
    }
}
