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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the Phase 3 typed {@link BioAssayFactory}.
 *
 * @see BioAssayFactory
 */
public class BioAssayFactoryTest extends BaseIntegrationTest5 {

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

    @AfterEach
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

        assertNotNull( ba.getId(), "BA must be persisted (id assigned)" );
        assertNotNull( ba.getName() );
        assertNotNull( ba.getSampleUsed(), "default BA should have a BioMaterial" );
        assertNotNull( ba.getSampleUsed().getId(), "auto-created BM should be persisted" );
        assertNotNull( ba.getArrayDesignUsed(), "default BA should have an ArrayDesign" );
        assertNotNull( ba.getArrayDesignUsed().getId(), "auto-created AD should be persisted" );
        assertNotNull( ba.getAccession(), "default BA should attach a GEO accession" );

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

        assertSame( persistedBm.getId(), ba.getSampleUsed().getId(),
                "BA should reference the supplied BM" );
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

        assertEquals( ad.getId(), ba.getArrayDesignUsed().getId(),
                "BA should reference the supplied AD" );
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
        assertEquals( name, ba.getAccession().getAccession(),
                "default accession should mirror the BA name" );
    }

    @Test
    public void withAccessionFalse_skipsAccession() {
        BioAssay ba = bioAssayFactory.builder()
                .withAccession( false )
                .build();
        createdBAs.add( ba );
        createdBMs.add( ba.getSampleUsed() );
        createdADs.add( ba.getArrayDesignUsed() );

        assertNull( ba.getAccession(),
                "accession should be omitted when withAccession(false)" );
    }

    @Test
    public void buildTransient_returnsUnpersistedBA_withPersistentBMAndAD() {
        BioAssay ba = bioAssayFactory.builder().buildTransient();

        assertNull( ba.getId(), "buildTransient should not persist the BA" );
        assertNotNull( ba.getSampleUsed().getId(),
                "BM should still be persisted (no cascade)" );
        assertNotNull( ba.getArrayDesignUsed().getId(),
                "AD should still be persisted (no cascade)" );

        createdBMs.add( ba.getSampleUsed() );
        createdADs.add( ba.getArrayDesignUsed() );
    }
}
