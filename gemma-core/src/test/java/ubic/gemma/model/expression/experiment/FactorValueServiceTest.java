/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.model.expression.experiment;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class FactorValueServiceTest extends BaseSpringContextTest {

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Test
    public void testDelete() {

        ExpressionExperiment ee = super.getTestPersistentCompleteExpressionExperiment( false );

        ee = expressionExperimentService.thawLite( ee );

        FactorValue fv = ee.getExperimentalDesign().getExperimentalFactors().iterator().next().getFactorValues()
                .iterator().next();

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                bm.getFactorValues().add( fv );
                bioMaterialService.update( bm );
            }
        }

        ee = expressionExperimentService.thawLite( ee );

        // done with setup

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertTrue( bm.getFactorValues().size() > 0 );
                fv = bm.getFactorValues().iterator().next();
            }
        }

        assertNotNull( fv );

        Long id = fv.getId();

        factorValueService.delete( fv );

        assertNull( factorValueService.load( id ) );

    }

}
