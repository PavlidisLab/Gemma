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
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the Phase 3 typed test-fixture factory for {@link ArrayDesign}.
 *
 * @see ArrayDesignFactory
 */
public class ArrayDesignFactoryTest extends BaseIntegrationTest {

    @Autowired
    private ArrayDesignFactory arrayDesignFactory;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private TaxonService taxonService;

    private final List<ArrayDesign> createdAds = new ArrayList<>();

    @After
    public void cleanUp() {
        for ( ArrayDesign ad : createdAds ) {
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
    public void build_defaults_returnsPersistentAdWithId() {
        ArrayDesign ad = arrayDesignFactory.build();
        createdAds.add( ad );

        assertNotNull( ad );
        assertNotNull( "AD id must be assigned by persist", ad.getId() );
        assertNotNull( ad.getShortName() );
        assertNotNull( ad.getName() );
        assertNotNull( "default build() should attach a primary taxon", ad.getPrimaryTaxon() );
        assertNotNull( ad.getTechnologyType() );
        assertTrue( "default build() should attach zero composite sequences",
                ad.getCompositeSequences().isEmpty() );
    }

    @Test
    public void withCompositeSequences_producesExactlyN() {
        int n = 4;
        ArrayDesign ad = arrayDesignFactory.builder().withCompositeSequences( n ).build();
        createdAds.add( ad );

        assertNotNull( ad.getId() );
        assertEquals( n, ad.getCompositeSequences().size() );
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            assertNotNull( "cs must be persisted by cascade", cs.getId() );
            assertNotNull( cs.getName() );
            assertSame( "cs back-reference must point to its parent AD",
                    ad.getId(), cs.getArrayDesign().getId() );
        }
    }

    @Test
    public void withTaxon_usesSuppliedTaxon() {
        Taxon human = taxonService.findByCommonName( "human" );
        assertNotNull( "test data must contain human taxon", human );

        ArrayDesign ad = arrayDesignFactory.builder().withTaxon( human ).build();
        createdAds.add( ad );

        assertNotNull( ad.getId() );
        assertEquals( "AD primary taxon should be the one supplied",
                human.getId(), ad.getPrimaryTaxon().getId() );
    }

    @Test
    public void withShortName_usesSuppliedShortName() {
        String sn = "AD_FIXTURE_TEST_" + System.nanoTime();
        ArrayDesign ad = arrayDesignFactory.builder().withShortName( sn ).build();
        createdAds.add( ad );

        assertNotNull( ad.getId() );
        assertEquals( sn, ad.getShortName() );
    }
}
