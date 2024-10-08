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

package ubic.gemma.persistence.service.expression.experiment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

/**
 * @author paul
 */
public class FactorValueServiceIntegrationTest extends BaseSpringContextTest {

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BioMaterialService bioMaterialService;

    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        ee = getTestPersistentCompleteExpressionExperiment( false );
    }

    @After
    public void tearDown() {
        expressionExperimentService.remove( ee );
    }

    @Test
    public void testDelete() {
        ee = expressionExperimentService.thawLite( ee );

        FactorValue fv = ee.getExperimentalDesign().getExperimentalFactors().iterator().next().getFactorValues()
                .iterator().next();

        // ensure that the FV is being used
        assertThat( ee.getBioAssays() ).anySatisfy( ba -> {
            assertThat( ba.getSampleUsed().getFactorValues() ).contains( fv );
        } );

        // delete the FV
        Long id = fv.getId();
        factorValueService.remove( fv );
        assertNull( factorValueService.load( id ) );

        ee = expressionExperimentService.thawLite( ee );
        assertThat( ee.getBioAssays() ).allSatisfy( ba -> {
            BioMaterial bm = ba.getSampleUsed();
            assertThat( bm.getFactorValues() ).doesNotContain( fv );
        } );
    }

}
