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
 *
 */
package ubic.gemma.persistence.persister;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.EeWriteService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A new experiment must not attach its BioAssays to BioMaterials another experiment already uses.
 * <p>
 * {@code BioMaterialDao.findOrCreate} matches a sample with no accession and no description by name and taxon alone,
 * so without the skip the second experiment below would share {@code <prefix>_shared} with the first. That is how
 * HBCC_Cohort's 2026-09-29 re-add came to use 178 of Aging_Cohort's BioMaterials, after the skip was lost in the
 * {@code ExpressionPersister} → {@link EeWriteServiceImpl} move.
 */
public class EeWriteServiceImplExistingBioMaterialTest extends BaseSpringContextTest5 {

    @Autowired
    private EeWriteService eeWriteService;

    @Test
    @Transactional
    public void testSamplesWhoseBioMaterialsAlreadyExistAreSkipped() {
        ArrayDesign ad = getTestPersistentArrayDesign( 1, true );
        String prefix = "donor_" + randomName();

        ExpressionExperiment first = eeWriteService.create( newExperiment( ad, prefix + "_shared" ) );
        assertThat( first.getBioAssays() ).hasSize( 1 );

        ExpressionExperiment second = eeWriteService.create( newExperiment( ad, prefix + "_shared", prefix + "_new" ) );

        assertThat( second.getBioAssays() )
                .extracting( ba -> ba.getSampleUsed().getName() )
                .containsExactly( prefix + "_new" );
        assertThat( second.getNumberOfSamples() ).isEqualTo( 1 );
        assertThat( second.getBioAssays() )
                .extracting( BioAssay::getSampleUsed )
                .doesNotContainAnyElementsOf( first.getBioAssays().stream().map( BioAssay::getSampleUsed ).toList() );
    }

    private ExpressionExperiment newExperiment( ArrayDesign ad, String... sampleNames ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( randomName() );
        ee.setName( ee.getShortName() );
        ee.setTaxon( ad.getPrimaryTaxon() );
        for ( String sampleName : sampleNames ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( sampleName, ad.getPrimaryTaxon() );
            BioAssay ba = BioAssay.Factory.newInstance( sampleName, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        ee.setNumberOfSamples( ee.getBioAssays().size() );
        return ee;
    }
}
