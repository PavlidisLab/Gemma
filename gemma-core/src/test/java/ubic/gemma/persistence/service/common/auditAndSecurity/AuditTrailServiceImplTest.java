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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
public class AuditTrailServiceImplTest extends BaseSpringContextTest {

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private ArrayDesign auditable;
    private int size;

    @Before
    public void setUp() throws Exception {

        auditable = ArrayDesign.Factory.newInstance();
        auditable.setName( "testing audit " + RandomStringUtils.randomAlphanumeric( 32 ) );
        auditable.setShortName( RandomStringUtils.randomAlphanumeric( 8 ) );
        auditable.setPrimaryTaxon( this.getTaxon( "human" ) );
        auditable = ( ArrayDesign ) this.persisterHelper.persist( auditable );

        assertNotNull( auditable.getAuditTrail() );
        assertNotNull( auditable.getCurationDetails() );

        size = auditable.getAuditTrail().getEvents().size();
    }

    @After
    public void tearDown() {
        arrayDesignService.remove( auditable );
    }

    @Test
    public final void testAddOKEvent() {
        auditable.getCurationDetails().setTroubled( true );
        auditTrailService.addUpdateEvent( auditable, NotTroubledStatusFlagEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertTrue( ev.getEventType() instanceof NotTroubledStatusFlagEvent );

        auditable = arrayDesignService.load( auditable.getId() );
        assertNotNull( auditable );
        auditable = arrayDesignService.thawLite( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertEquals( ev.getDate(), auditable.getCurationDetails().getLastUpdated() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        System.out.println( auditable.getAuditTrail().getEvents() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
    }

    @Test
    public final void testAddTroubleEvent() {
        auditTrailService.addUpdateEvent( auditable, TroubledStatusFlagEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertTrue( ev.getEventType() instanceof TroubledStatusFlagEvent );
        assertEquals( "nothing special, just testing", ev.getNote() );

        auditable = arrayDesignService.load( auditable.getId() );
        assertNotNull( auditable );
        auditable = arrayDesignService.thawLite( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertEquals( ev.getDate(), auditable.getCurationDetails().getLastUpdated() );
        assertEquals( size + 1, auditTrail.getEvents().size() );

        assertTrue( auditable.getCurationDetails().getTroubled() );
    }

    @Test
    public final void testAddUpdateEventAuditableAuditEventTypeString() {
        auditTrailService.addUpdateEvent( auditable, AlignmentBasedGeneMappingEvent.class, "nothing special, just testing" );
        auditable = arrayDesignService.load( auditable.getId() );
        assertNotNull( auditable );
        auditable = arrayDesignService.thawLite( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertNotNull( auditable.getCurationDetails() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        // FIXME: one of the two date makes a round-trip in the database and is of type Timestamp (which is a subclass of Date)
        assertEquals( ev.getDate().getTime(), auditable.getCurationDetails().getLastUpdated().getTime() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertEquals( AlignmentBasedGeneMappingEvent.class, ev.getEventType().getClass() );
    }

    @Test
    public final void testAddUpdateEventAuditableString() {
        auditTrailService.addUpdateEvent( auditable, "nothing special, just testing" );
        auditable = arrayDesignService.load( auditable.getId() );
        assertNotNull( auditable );
        auditable = arrayDesignService.thawLite( auditable );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertEquals( size + 1, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public final void testAddNeedsAttentionEvent() {
        auditTrailService.addUpdateEvent( auditable, NeedsAttentionEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertEquals( NeedsAttentionEvent.class, ev.getEventType().getClass() );

        auditable = arrayDesignService.load( auditable.getId() );
        assertNotNull( auditable );
        auditable = arrayDesignService.thawLite( auditable );

        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertEquals( ev.getDate(), auditable.getCurationDetails().getLastUpdated() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        assertTrue( auditable.getCurationDetails().getNeedsAttention() );

        for ( AuditEvent e : auditTrail.getEvents() ) {
            assertNotNull( e.getId() );
        }
    }

    @Test
    public final void testAddDoesNotNeedsAttentionEvent() {
        auditTrailService.addUpdateEvent( auditable, DoesNotNeedAttentionEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertEquals( DoesNotNeedAttentionEvent.class, ev.getEventType().getClass() );

        auditable = arrayDesignService.load( auditable.getId() );
        assertNotNull( auditable );
        auditable = arrayDesignService.thawLite( auditable );

        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        assertEquals( ev.getDate(), auditable.getCurationDetails().getLastUpdated() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        assertFalse( auditable.getCurationDetails().getNeedsAttention() );

        for ( AuditEvent e : auditTrail.getEvents() ) {
            assertNotNull( e.getId() );
        }
    }

    @Test
    public final void testGetEntitiesWithEvent() {
        auditTrailService.addUpdateEvent( auditable, SampleRemovalEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getLast();
        assertNotNull( ev );
        assertNotNull( ev.getId() );

        AuditTrail auditTrail = auditable.getAuditTrail();

        Collection<AuditEvent> events = auditTrail.getEvents();
        assertTrue( events.contains( ev ) );

        events = auditTrailService.getEvents( auditable );
        assertTrue( events.contains( ev ) );
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private PlatformTransactionManager pta;

    @Test
    public void testAddEventWhenTransactionIsRolledBack() {
        Session session = sessionFactory.openSession();
        try {
            TransactionStatus t = pta.getTransaction( new DefaultTransactionDefinition() );
            auditTrailService.addUpdateEvent( auditable, SampleRemovalEvent.class, "test" );
            pta.rollback( t );
        } finally {
            session.close();
        }
        auditable = arrayDesignService.thaw( auditable );
        // ensure that no even has been created
        assertEquals( size, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public void testAddEventWhenTransactionIsRolledBack2() {
        Session session = sessionFactory.openSession();
        try {
            TransactionStatus t = pta.getTransaction( new DefaultTransactionDefinition() );
            auditTrailService.addUpdateEvent( auditable, SampleRemovalEvent.class, "test", new RuntimeException() );
            pta.rollback( t );
        } finally {
            session.close();
        }
        auditable = arrayDesignService.thaw( auditable );
        AuditEvent e = auditable.getAuditTrail().getLast();
        Assert.assertEquals( AuditAction.UPDATE, e.getAction() );
        assertEquals( "test", e.getNote() );
        assertTrue( e.getDetail().contains( "RuntimeException" ) );
        // ensure that the exception is logged
        assertEquals( size + 1, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public void testAddUpdateEventOnTransientEntity() {
        ArrayDesign ad = new ArrayDesign();
        assertThrows( IllegalArgumentException.class, () -> auditTrailService.addUpdateEvent( ad, SampleRemovalEvent.class, "test" ) );
    }

    @Test
    public void testAddExceptionEventOnTransientEntity() {
        ArrayDesign ad = new ArrayDesign();
        assertThrows( IllegalArgumentException.class, () -> auditTrailService.addUpdateEvent( ad, SampleRemovalEvent.class, "test", new RuntimeException() ) );
    }
}
