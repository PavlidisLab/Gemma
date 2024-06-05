/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author keshav
 */
public class AuditTrailDaoTest extends BaseSpringContextTest {

    @Autowired
    AuditTrailService auditTrailService;

    @Autowired
    ArrayDesignService arrayDesignService;
    private Auditable auditable;
    private AuditTrail auditTrail;

    @Before
    public void setUp() throws Exception {

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "test_" + RandomStringUtils.randomAlphabetic( 10 ) );
        ad.setPrimaryTaxon( this.getTaxon( "mouse" ) );

        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        auditable = ad;

        auditTrail = AuditTrail.Factory.newInstance();

        AuditEvent auditEvent0 = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.CREATE, "ccccc", null, null, null );

        AuditEvent auditEvent1 = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.CREATE, "ddddd", null, null, null );

        AuditEvent auditEvent2 = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.CREATE, "aaaaa", null, null, null );

        AuditEvent auditEvent3 = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.CREATE, "bbbbb", null, null, null );

        auditTrail.getEvents().add( auditEvent0 );
        auditTrail.getEvents().add( auditEvent1 );
        auditTrail.getEvents().add( auditEvent2 );
        auditTrail.getEvents().add( auditEvent3 );

    }

    @Test
    public void testCreate() {
        log.info( "Creating audit trail" );
        assert auditTrail != null;
        AuditTrail t = auditTrailService.create( auditTrail );
        assertNotNull( t );
        assertNotNull( t.getId() );
    }

    @Test
    public void testHandleAddEventAuditableAuditEvent() {
        auditTrailService.addUpdateEvent( auditable, "this is a test" );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertTrue( auditable.getAuditTrail().getEvents().size() > 1 );

    }
}
