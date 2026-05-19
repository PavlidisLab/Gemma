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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the Phase 3 typed test-fixture factory.
 *
 * @see ExperimentFactory
 */
public class ExperimentFactoryTest extends BaseIntegrationTest {

    @Autowired
    private ExperimentFactory experimentFactory;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private TaxonService taxonService;

    private final List<ExpressionExperiment> createdEEs = new ArrayList<>();
    private final List<ArrayDesign> createdADs = new ArrayList<>();

    @After
    public void cleanUp() {
        for ( ExpressionExperiment ee : createdEEs ) {
            try {
                ExpressionExperiment fresh = expressionExperimentService.load( ee.getId() );
                if ( fresh != null ) {
                    expressionExperimentService.remove( fresh );
                }
            } catch ( Exception ignored ) {
                // best-effort cleanup
            }
        }
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
    public void bulkRna_defaults_returnsPersistentEEWithId() {
        ExpressionExperiment ee = experimentFactory.bulkRna().build();
        createdEEs.add( ee );

        assertNotNull( ee );
        assertNotNull( "EE id must be assigned by persist", ee.getId() );
        assertNotNull( ee.getShortName() );
        assertNotNull( ee.getName() );
        assertNotNull( "default bulkRna() should attach a taxon", ee.getTaxon() );
        assertNotNull( "default bulkRna() should attach an experimental design", ee.getExperimentalDesign() );
        assertFalse( "default bulkRna() should attach bioassays", ee.getBioAssays().isEmpty() );
        assertFalse( "default bulkRna() should attach a preferred raw QT", ee.getQuantitationTypes().isEmpty() );
        assertTrue( "default bulkRna() should mark a QT as preferred",
                ee.getQuantitationTypes().stream().anyMatch( qt -> Boolean.TRUE.equals( qt.getIsPreferred() ) ) );
    }

    @Test
    public void withSamples_producesExactlyNBioAssays() {
        int n = 5;
        ExpressionExperiment ee = experimentFactory.bulkRna().withSamples( n ).build();
        createdEEs.add( ee );

        assertEquals( n, ee.getBioAssays().size() );
        assertEquals( Integer.valueOf( n ), ee.getNumberOfSamples() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertNotNull( ba.getArrayDesignUsed() );
            assertNotNull( ba.getSampleUsed() );
        }
    }

    @Test
    public void withArrayDesign_usesSuppliedAD() {
        Taxon mouse = taxonService.findByCommonName( "mouse" );
        assertNotNull( "test data must contain mouse taxon", mouse );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "supplied AD test" );
        ad.setShortName( "SUP_AD" );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        ad.setPrimaryTaxon( mouse );
        ad = arrayDesignService.create( ad );
        createdADs.add( ad );

        ExpressionExperiment ee = experimentFactory.bulkRna()
                .withArrayDesign( ad )
                .withSamples( 3 )
                .build();
        createdEEs.add( ee );

        assertFalse( ee.getBioAssays().isEmpty() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            assertSame( "every bioassay should reference the supplied AD",
                    ad.getId(), ba.getArrayDesignUsed().getId() );
        }
    }

    @Test
    public void singleCell_defaults_returnsPersistentEEWithoutBulkQts() {
        ExpressionExperiment ee = experimentFactory.singleCell().build();
        createdEEs.add( ee );

        assertNotNull( ee );
        assertNotNull( ee.getId() );
        assertTrue( "single-cell EE should have no raw bulk QTs by default",
                ee.getQuantitationTypes().isEmpty() );
        assertFalse( ee.getBioAssays().isEmpty() );
    }
}
