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
package ubic.gemma.model.common.auditAndSecurity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class AuditTrailDaoTest extends BaseSpringContextTest {

    @Autowired
    AuditTrailDao auditTrailDao;

    @Autowired
    ArrayDesignService arrayDesignService;

    Auditable auditable;
    AuditTrail auditTrail;
    AuditEvent auditEvent0;
    AuditEvent auditEvent1;
    AuditEvent auditEvent2;
    AuditEvent auditEvent3;
    AuditEvent auditEvent4;

    /**
     * @exception Exception
     */
    @Before
    public void setup() throws Exception {

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "test_" + RandomStringUtils.randomAlphabetic( 10 ) );
        ad.setPrimaryTaxon( this.getTaxon( "mouse" ) );

        ad = arrayDesignService.create( ad );
        auditable = ad;

        auditTrail = AuditTrail.Factory.newInstance();

        auditEvent0 = AuditEvent.Factory.newInstance();
        auditEvent0.setDate( new Date() );
        auditEvent0.setNote( "ccccc" );
        auditEvent0.setAction( AuditAction.CREATE );

        auditEvent1 = AuditEvent.Factory.newInstance();
        auditEvent1.setDate( new Date() );
        auditEvent1.setNote( "ddddd" );
        auditEvent1.setAction( AuditAction.CREATE );

        auditEvent2 = AuditEvent.Factory.newInstance();
        auditEvent2.setDate( new Date() );
        auditEvent2.setNote( "aaaaa" );
        auditEvent2.setAction( AuditAction.CREATE );

        auditEvent3 = AuditEvent.Factory.newInstance();
        auditEvent3.setDate( new Date() );
        auditEvent3.setNote( "bbbbb" );
        auditEvent3.setAction( AuditAction.CREATE );

        auditTrail.addEvent( auditEvent0 );
        auditTrail.addEvent( auditEvent1 );
        auditTrail.addEvent( auditEvent2 );
        auditTrail.addEvent( auditEvent3 );

    }

    @Test
    public void testCreate() {
        log.info( "Creating audit trail" );
        assert auditTrail != null;
        AuditTrail t = auditTrailDao.create( auditTrail );
        assertNotNull( t );
        assertNotNull( t.getId() );
    }

    @Test
    public void testHandleAddEventAuditableAuditEvent() throws Exception {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setNote( "this is a test" );
        auditEvent = auditTrailDao.addEvent( auditable, auditEvent );
        assertNotNull( auditEvent.getId() );
        assertTrue( auditable.getAuditTrail().getEvents().size() > 1 );

    }

}
