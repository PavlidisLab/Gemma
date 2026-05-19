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
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the Phase 3 typed {@link ArrayDesignFactory}.
 *
 * @see ArrayDesignFactory
 */
public class ArrayDesignFactoryTest extends BaseIntegrationTest {

    @Autowired
    private ArrayDesignFactory arrayDesignFactory;

    @Autowired
    private TaxonFactory taxonFactory;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private final List<ArrayDesign> createdADs = new ArrayList<>();

    @After
    public void cleanUp() {
        for ( ArrayDesign ad : createdADs ) {
            try {
                ArrayDesign fresh = arrayDesignService.load( ad.getId() );
                if ( fresh != null ) {
                    arrayDesignService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // best-effort cleanup
            }
        }
    }

    @Test
    public void oneColor_defaults_returnsPersistentEmptyAD() {
        ArrayDesign ad = arrayDesignFactory.oneColor().build();
        createdADs.add( ad );

        assertNotNull( ad );
        assertNotNull( "AD id must be assigned by persist", ad.getId() );
        assertNotNull( ad.getShortName() );
        assertNotNull( ad.getName() );
        assertNotNull( "default oneColor() should attach a primary taxon", ad.getPrimaryTaxon() );
        assertEquals( TechnologyType.ONECOLOR, ad.getTechnologyType() );
        assertTrue( "default oneColor() should have no probes", ad.getCompositeSequences().isEmpty() );
    }

    @Test
    public void twoColor_setsTechnologyType() {
        ArrayDesign ad = arrayDesignFactory.twoColor().build();
        createdADs.add( ad );
        assertEquals( TechnologyType.TWOCOLOR, ad.getTechnologyType() );
    }

    @Test
    public void geneChip_setsTechnologyType() {
        ArrayDesign ad = arrayDesignFactory.geneChip().build();
        createdADs.add( ad );
        assertEquals( TechnologyType.GENELIST, ad.getTechnologyType() );
    }

    @Test
    public void withProbes_producesExactlyNCompositeSequences() {
        int n = 7;
        ArrayDesign ad = arrayDesignFactory.oneColor().withProbes( n ).build();
        createdADs.add( ad );

        assertEquals( n, ad.getCompositeSequences().size() );
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            assertNotNull( "every CS should be persisted via cascade", cs.getId() );
            assertSame( "every CS should back-reference this AD", ad.getId(), cs.getArrayDesign().getId() );
            assertNotNull( cs.getName() );
        }
    }

    @Test
    public void withRandomProbeNamesFalse_namesAreDeterministic() {
        ArrayDesign ad = arrayDesignFactory.oneColor()
                .withProbes( 3 )
                .withRandomProbeNames( false )
                .build();
        createdADs.add( ad );

        // exactly probeset_0, probeset_1, probeset_2
        boolean[] seen = new boolean[3];
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            for ( int i = 0; i < 3; i++ ) {
                if ( ( "probeset_" + i ).equals( cs.getName() ) ) {
                    seen[i] = true;
                }
            }
        }
        for ( int i = 0; i < 3; i++ ) {
            assertTrue( "expected probeset_" + i + " in deterministic names", seen[i] );
        }
    }

    @Test
    public void withSequencesTrue_attachesBioSequencesToEveryProbe() {
        int n = 4;
        ArrayDesign ad = arrayDesignFactory.oneColor()
                .withProbes( n )
                .withSequences( true )
                .build();
        createdADs.add( ad );

        assertEquals( n, ad.getCompositeSequences().size() );
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            assertNotNull( "every CS should have a BioSequence attached", cs.getBiologicalCharacteristic() );
            assertNotNull( "BioSequence should be persisted (id assigned)",
                    cs.getBiologicalCharacteristic().getId() );
        }
    }

    @Test
    public void withTaxon_appliesOverride() {
        Taxon human = taxonFactory.human();
        ArrayDesign ad = arrayDesignFactory.oneColor().withTaxon( human ).build();
        createdADs.add( ad );

        assertEquals( "primaryTaxon should be the supplied human",
                human.getId(), ad.getPrimaryTaxon().getId() );
    }

    @Test
    public void withShortName_appliesOverride() {
        String shortNm = "SUP_AD_TEST";
        ArrayDesign ad = arrayDesignFactory.oneColor().withShortName( shortNm ).build();
        createdADs.add( ad );
        assertEquals( shortNm, ad.getShortName() );
    }

    @Test
    public void emptyAD_hasNoProbes_withSequencesIsNoOp() {
        ArrayDesign ad = arrayDesignFactory.oneColor()
                .withSequences( true )      // requested but no probes
                .build();
        createdADs.add( ad );

        assertFalse( "AD persisted", ad.getId() == null );
        assertTrue( "no probes requested means no CSes", ad.getCompositeSequences().isEmpty() );
    }
}
