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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
public class AuditTrailServiceImplTest extends BaseSpringContextTest {

    private ArrayDesign auditable;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private Persister<ArrayDesign> persisterHelper;

    private int size;

    @Before
    public void setup() {

        auditable = ArrayDesign.Factory.newInstance();
        auditable.setName( "testing audit " + RandomStringUtils.randomAlphanumeric( 32 ) );
        auditable.setShortName( RandomStringUtils.randomAlphanumeric( 8 ) );
        auditable.setPrimaryTaxon( this.getTaxon( "human" ) );
        auditable = this.persisterHelper.persist( auditable );

        assertTrue( auditable.getAuditTrail() != null );

        size = auditable.getAuditTrail().getEvents().size();
    }

    @Test
    public final void testAddOKEvent() {
        AuditEventType eventType = NotTroubledStatusFlagEvent.Factory.newInstance();
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, eventType, "nothing special, just testing" );
        assertNotNull( ev.getId() );

        auditable = arrayDesignService.thawLite( arrayDesignService.load( auditable.getId() ) );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertEquals( NotTroubledStatusFlagEvent.class,
                ( ( List<AuditEvent> ) auditTrail.getEvents() ).get( size ).getEventType().getClass() );
    }

    @Test
    public final void testAddTroubleEvent() {
        AuditEventType eventType = TroubledStatusFlagEvent.Factory.newInstance();
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, eventType, "nothing special, just testing" );
        assertNotNull( ev.getId() );

        auditable = arrayDesignService.thawLite( arrayDesignService.load( auditable.getId() ) );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertEquals( size + 1, auditTrail.getEvents().size() );

        assertTrue( auditable.getCurationDetails().getTroubled() );

        assertEquals( TroubledStatusFlagEvent.class,
                ( ( List<AuditEvent> ) auditTrail.getEvents() ).get( size ).getEventType().getClass() );
    }

    @Test
    public final void testAddUpdateEventAuditableAuditEventTypeString() {
        AuditEventType f = AlignmentBasedGeneMappingEvent.Factory.newInstance();
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, f, "nothing special, just testing" );
        assertNotNull( ev.getId() );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertEquals( AlignmentBasedGeneMappingEvent.class,
                ( ( List<AuditEvent> ) auditTrail.getEvents() ).get( size ).getEventType().getClass() );
    }

    @Test
    public final void testAddUpdateEventAuditableString() {
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, "nothing special, just testing" );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertEquals( size + 1, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public final void testAddNeedsAttentionEvent() {
        AuditEventType eventType = NeedsAttentionEvent.Factory.newInstance();
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, eventType, "nothing special, just testing" );
        assertNotNull( ev.getId() );

        auditable = arrayDesignService.thawLite( arrayDesignService.load( auditable.getId() ) );

        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        assertTrue( auditable.getCurationDetails().getNeedsAttention() );

        assertEquals( NeedsAttentionEvent.class,
                ( ( List<AuditEvent> ) auditTrail.getEvents() ).get( size ).getEventType().getClass() );

        for ( AuditEvent e : auditTrail.getEvents() ) {
            assertNotNull( e.getId() );
        }
    }

    @Test
    public final void testAddDoesNotNeedsAttentionEvent() {
        AuditEventType eventType = DoesNotNeedAttentionEvent.Factory.newInstance();
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, eventType, "nothing special, just testing" );
        assertNotNull( ev.getId() );

        auditable = arrayDesignService.thawLite( arrayDesignService.load( auditable.getId() ) );

        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        assertFalse( auditable.getCurationDetails().getNeedsAttention() );

        assertEquals( DoesNotNeedAttentionEvent.class,
                ( ( List<AuditEvent> ) auditTrail.getEvents() ).get( size ).getEventType().getClass() );

        for ( AuditEvent e : auditTrail.getEvents() ) {
            assertNotNull( e.getId() );
        }
    }

    @Test
    public final void testGetEntitiesWithEvent() {
        AuditEventType eventType = SampleRemovalEvent.Factory.newInstance();
        AuditEvent ev = auditTrailService.addUpdateEvent( auditable, eventType, "nothing special, just testing" );
        assertNotNull( ev.getId() );

        AuditTrail auditTrail = auditable.getAuditTrail();

        Collection<AuditEvent> events = auditTrail.getEvents();
        assertTrue( events.contains( ev ) );

        events = auditTrailService.getEvents( auditable );
        assertTrue( events.contains( ev ) );
    }

    @Test
    public final void testReflectionOnFactory() throws Exception {
        Class<? extends AuditEventType> type = DoesNotNeedAttentionEvent.class;
        AuditEventType auditEventType;

        Class<?> factory = Class.forName( type.getName() + "$Factory" );
        Method method = factory.getMethod( "newInstance" );
        auditEventType = ( AuditEventType ) method.invoke( type );
        assertNotNull( auditEventType );
        assertTrue( auditEventType instanceof DoesNotNeedAttentionEvent );
    }
}
