/*
 * The gemma-web project
 *
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubledStatusFlagEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author tesarst
 */
public class CuratableValueObjectTest extends BaseSpringContextTest {

    private ArrayDesign arrayDesign;
    private ExpressionExperiment expressionExperiment;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Before
    public void setUp() throws Exception {

        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( "testing audit " + RandomStringUtils.randomAlphanumeric( 32 ) );
        arrayDesign.setShortName( RandomStringUtils.randomAlphanumeric( 8 ) );
        arrayDesign.setPrimaryTaxon( this.getTaxon( "human" ) );
        arrayDesign = ( ArrayDesign ) this.persisterHelper.persist( arrayDesign );

        assertTrue( arrayDesign.getAuditTrail() != null );

        Taxon taxon = Taxon.Factory
                .newInstance( "text taxon scientific name " + RandomStringUtils.randomAlphanumeric( 8 ), "ttxn", 0,
                        true );
        this.persisterHelper.persist( taxon );

        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( RandomStringUtils.randomAlphanumeric( 8 ) );
        bm.setSourceTaxon( taxon );
        this.persisterHelper.persist( bm );

        BioAssay bioAssay = BioAssay.Factory.newInstance();
        bioAssay.setArrayDesignUsed( arrayDesign );
        bioAssay.setSampleUsed( bm );
        this.persisterHelper.persist( bioAssay );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( RandomStringUtils.randomAlphanumeric( 8 ) );

        expressionExperiment = super.getTestPersistentBasicExpressionExperiment();

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCuratableValueObjectCreation() {
        ArrayDesignValueObject adVO = this.arrayDesignService.loadValueObject( arrayDesign );
        assertNotNull( adVO );

        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }

        ExpressionExperimentValueObject eeVO = this.expressionExperimentService.loadValueObject( expressionExperiment );
        assertNotNull( eeVO );

        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }

        ExpressionExperimentDetailsValueObject eeDVO = new ExpressionExperimentDetailsValueObject( eeVO );
        eeDVO.setArrayDesigns( Collections.singleton( adVO ) );

        assertNotNull( eeDVO );
        assertNotNull( eeDVO.getArrayDesigns() );
    }

    @Test
    public void testCuratableValueObjectInteraction() {
        ArrayDesignValueObject adVO = this.arrayDesignService.loadValueObject( arrayDesign );
        assertNotNull( adVO );
        assertFalse( adVO.getTroubled() );

        ExpressionExperimentDetailsValueObject eeDVO = new ExpressionExperimentDetailsValueObject(
                this.expressionExperimentService.loadValueObject( expressionExperiment ) );
        eeDVO.setArrayDesigns( Collections.singleton( adVO ) );

        assertFalse( eeDVO.getTroubled() );
        assertFalse( eeDVO.getActuallyTroubled() );
        assertFalse( eeDVO.getPlatformTroubled() );

        // Make array design troubled
        this.auditTrailService.addUpdateEvent( this.arrayDesign, TroubledStatusFlagEvent.class,
                "testing trouble update on platform",
                "trouble update details" );

        adVO = this.arrayDesignService.loadValueObject( arrayDesign );
        assertNotNull( adVO );
        assertTrue( adVO.getTroubled() );

        eeDVO = new ExpressionExperimentDetailsValueObject(
                this.expressionExperimentService.loadValueObject( expressionExperiment ) );
        eeDVO.setArrayDesigns( Collections.singleton( adVO ) );

        assertTrue( eeDVO.getTroubled() );
        assertFalse( eeDVO.getActuallyTroubled() );
        assertTrue( eeDVO.getPlatformTroubled() );

        // Make expression experiment troubled
        this.auditTrailService.addUpdateEvent( this.expressionExperiment, TroubledStatusFlagEvent.class,
                "testing trouble update on expression experiment",
                "trouble update details" );

        eeDVO = new ExpressionExperimentDetailsValueObject(
                this.expressionExperimentService.loadValueObject( expressionExperiment ) );
        eeDVO.setArrayDesigns( Collections.singleton( adVO ) );

        assertTrue( eeDVO.getTroubled() );
        assertTrue( eeDVO.getActuallyTroubled() );
        assertTrue( eeDVO.getPlatformTroubled() );
    }

}
