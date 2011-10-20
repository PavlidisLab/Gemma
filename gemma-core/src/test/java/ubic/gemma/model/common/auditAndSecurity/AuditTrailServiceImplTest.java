/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedFlagEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailServiceImplTest extends BaseSpringContextTest {

    ArrayDesign auditable;

    @Autowired
    AuditTrailService auditTrailService;
    private int size;

    @Before
    public void setup() throws Exception {

        auditable = ArrayDesign.Factory.newInstance();
        auditable.setName( "testing audit " + RandomStringUtils.randomAlphanumeric( 32 ) );
        auditable.setShortName( RandomStringUtils.randomAlphanumeric( 8 ) );
        auditable.setPrimaryTaxon( this.getTaxon( "human" ) );
        auditable = ( ArrayDesign ) this.persisterHelper.persist( auditable );

        assertTrue( auditable.getAuditTrail() != null );

        size = auditable.getAuditTrail().getEvents().size();
    }

    @Test
    public final void testAddUpdateEventAuditableAuditEventTypeString() {
        AuditEventType f = ArrayDesignGeneMappingEvent.Factory.newInstance();
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, f, "nothing special, just testing" );
        assertNotNull( ev.getId() );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getStatus() );
        assertNotNull( auditable.getStatus().getLastUpdateDate() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertEquals( ArrayDesignGeneMappingEventImpl.class, ( ( List<AuditEvent> ) auditTrail.getEvents() ).get( size )
                .getEventType().getClass() );
    }

    @Test
    public final void testAddUpdateEventAuditableString() {
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, "nothing special, just testing" );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertEquals( size + 1, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public final void testReflectionOnFactory() throws Exception {
        Class<? extends AuditEventType> type = ValidatedFlagEvent.class;
        AuditEventType auditEventType = null;

        Class<?> factory = Class.forName( type.getName() + "$Factory" );
        Method method = factory.getMethod( "newInstance" );
        auditEventType = ( AuditEventType ) method.invoke( type );
        assertNotNull( auditEventType );
        assertTrue( auditEventType instanceof ValidatedFlagEvent );
    }
}
