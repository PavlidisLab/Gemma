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
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the Phase 3 typed {@link CompositeSequenceFactory}.
 *
 * @see CompositeSequenceFactory
 */
public class CompositeSequenceFactoryTest extends BaseIntegrationTest5 {

    @Autowired
    private CompositeSequenceFactory compositeSequenceFactory;

    @Autowired
    private ArrayDesignFactory arrayDesignFactory;

    @Autowired
    private TaxonFactory taxonFactory;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private final List<CompositeSequence> createdCSes = new ArrayList<>();
    private final List<ArrayDesign> createdADs = new ArrayList<>();

    @AfterEach
    public void cleanUp() {
        for ( CompositeSequence cs : createdCSes ) {
            try {
                CompositeSequence fresh = compositeSequenceService.load( cs.getId() );
                if ( fresh != null ) {
                    compositeSequenceService.remove( fresh );
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
    public void defaults_returnPersistentCS_withAutoCreatedADAndBioSequence() {
        CompositeSequence cs = compositeSequenceFactory.builder().build();
        createdCSes.add( cs );
        createdADs.add( cs.getArrayDesign() );

        assertNotNull( cs.getId(), "CS must be persisted (id assigned)" );
        assertNotNull( cs.getName(), "default CS should have a name" );
        assertNotNull( cs.getArrayDesign(), "default CS should have an ArrayDesign" );
        assertNotNull( cs.getArrayDesign().getId(), "auto-created AD should be persisted" );
        assertNotNull( cs.getBiologicalCharacteristic(), "default CS should have a BioSequence" );
        assertNotNull( cs.getBiologicalCharacteristic().getId(),
                "BioSequence should be persisted (no cascade from CS)" );
        assertNotNull( cs.getBiologicalCharacteristic().getSequenceDatabaseEntry(),
                "default BioSequence should carry a sequence-database link" );
    }

    @Test
    public void withCustomArrayDesign_appliesOverride() {
        ArrayDesign ad = arrayDesignFactory.oneColor().build();
        createdADs.add( ad );

        CompositeSequence cs = compositeSequenceFactory.builder()
                .withArrayDesign( ad )
                .build();
        createdCSes.add( cs );

        assertEquals( ad.getId(), cs.getArrayDesign().getId(),
                "CS should reference the supplied AD" );
    }

    @Test
    public void withName_appliesOverride() {
        String name = "named_cs_test_fixture";
        CompositeSequence cs = compositeSequenceFactory.builder()
                .withName( name )
                .build();
        createdCSes.add( cs );
        createdADs.add( cs.getArrayDesign() );

        assertEquals( name, cs.getName() );
    }

    @Test
    public void withBioSequenceFalse_skipsBioSequence() {
        CompositeSequence cs = compositeSequenceFactory.builder()
                .withBioSequence( false )
                .build();
        createdCSes.add( cs );
        createdADs.add( cs.getArrayDesign() );

        assertNotNull( cs.getId() );
        assertNull( cs.getBiologicalCharacteristic(),
                "biological characteristic should be omitted when withBioSequence(false)" );
    }

    @Test
    public void withTaxon_setsBioSequenceTaxon() {
        Taxon human = taxonFactory.human();
        // Build an AD on human so the CS's default doesn't override us.
        ArrayDesign ad = arrayDesignFactory.oneColor().withTaxon( human ).build();
        createdADs.add( ad );

        CompositeSequence cs = compositeSequenceFactory.builder()
                .withArrayDesign( ad )
                .withTaxon( human )
                .build();
        createdCSes.add( cs );

        assertNotNull( cs.getBiologicalCharacteristic() );
        assertEquals( human.getId(), cs.getBiologicalCharacteristic().getTaxon().getId(),
                "BioSequence taxon should be the supplied human" );
    }

    @Test
    public void withDescription_appliesOverride() {
        CompositeSequence cs = compositeSequenceFactory.builder()
                .withDescription( "test cs description" )
                .build();
        createdCSes.add( cs );
        createdADs.add( cs.getArrayDesign() );

        assertEquals( "test cs description", cs.getDescription() );
    }
}
