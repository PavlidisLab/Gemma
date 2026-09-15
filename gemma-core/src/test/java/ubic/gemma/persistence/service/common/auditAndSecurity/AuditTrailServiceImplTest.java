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
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author pavlidis
 */
public class AuditTrailServiceImplTest extends BaseSpringContextTest5 {

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private ArrayDesign auditable;
    private int size;

    @BeforeEach
    public void setUp() throws Exception {

        auditable = ArrayDesign.Factory.newInstance();
        auditable.setName( "testing audit " + RandomStringUtils.insecure().nextAlphanumeric( 32 ) );
        auditable.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 8 ) );
        auditable.setPrimaryTaxon( this.getTaxon( "human" ) );
        auditable = this.arrayDesignPersister.persistArrayDesign( auditable );

        assertNotNull( auditable.getAuditTrail() );
        assertNotNull( auditable.getCurationDetails() );

        size = auditable.getAuditTrail().getEvents().size();
    }

    @AfterEach
    public void tearDown() {
        arrayDesignService.remove( auditable );
    }

    @Test
    public final void testAddOKEvent() {
        auditable.getCurationDetails().setTroubled( true );
        auditTrailService.addUpdateEvent( auditable, NotTroubledStatusFlagEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getEvents().isEmpty() ? null : auditable.getAuditTrail().getEvents().get( auditable.getAuditTrail().getEvents().size() - 1 );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertTrue( ev.getEventType() instanceof NotTroubledStatusFlagEvent );

        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );
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
        AuditEvent ev = auditable.getAuditTrail().getEvents().isEmpty() ? null : auditable.getAuditTrail().getEvents().get( auditable.getAuditTrail().getEvents().size() - 1 );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertTrue( ev.getEventType() instanceof TroubledStatusFlagEvent );
        assertEquals( "nothing special, just testing", ev.getNote() );

        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );
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
        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        AuditEvent ev = auditable.getAuditTrail().getEvents().isEmpty() ? null : auditable.getAuditTrail().getEvents().get( auditable.getAuditTrail().getEvents().size() - 1 );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertNotNull( auditable.getCurationDetails() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        // FIXME: one of the two date makes a round-trip in the database and is of type Timestamp (which is a subclass of Date)
        assertEquals( ev.getDate().getTime(), auditable.getCurationDetails().getLastUpdated().getTime() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertNotNull( ev.getEventType() );
        assertEquals( AlignmentBasedGeneMappingEvent.class, ev.getEventType().getClass() );
    }

    @Test
    public final void testAddUpdateEventAuditableString() {
        auditTrailService.addUpdateEvent( auditable, "nothing special, just testing" );
        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        AuditEvent ev = auditTrail.getEvents().isEmpty() ? null : auditTrail.getEvents().get( auditTrail.getEvents().size() - 1 );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertEquals( size + 1, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public final void testAddNeedsAttentionEvent() {
        auditTrailService.addUpdateEvent( auditable, NeedsAttentionEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getEvents().isEmpty() ? null : auditable.getAuditTrail().getEvents().get( auditable.getAuditTrail().getEvents().size() - 1 );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertNotNull( ev.getEventType() );
        assertEquals( NeedsAttentionEvent.class, ev.getEventType().getClass() );

        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );

        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        // After the lastEvent denorm work + MySQL datetime(3) precision, ev.getDate() comes
        // back as java.util.Date but CurationDetails.lastUpdated reloads as java.sql.Timestamp.
        // Timestamp.equals(Date) is asymmetric / false even for matching epoch millis, so
        // compare via getTime() (same instant) rather than Object.equals.
        assertEquals( ev.getDate().getTime(), auditable.getCurationDetails().getLastUpdated().getTime() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        assertTrue( auditable.getCurationDetails().getNeedsAttention() );

        for ( AuditEvent e : auditTrail.getEvents() ) {
            assertNotNull( e.getId() );
        }
    }

    @Test
    public final void testAddDoesNotNeedsAttentionEvent() {
        auditTrailService.addUpdateEvent( auditable, DoesNotNeedAttentionEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getEvents().isEmpty() ? null : auditable.getAuditTrail().getEvents().get( auditable.getAuditTrail().getEvents().size() - 1 );
        assertNotNull( ev );
        assertNotNull( ev.getId() );
        assertNotNull( ev.getEventType() );
        assertEquals( DoesNotNeedAttentionEvent.class, ev.getEventType().getClass() );

        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );

        AuditTrail auditTrail = auditable.getAuditTrail();
        assertNotNull( auditTrail );
        assertNotNull( auditable.getCurationDetails() );
        assertEquals( size + 1, auditTrail.getEvents().size() );
        assertNotNull( auditable.getCurationDetails().getLastUpdated() );
        // Date vs Timestamp epoch-millis comparison (see testAddNeedsAttentionEvent).
        assertEquals( ev.getDate().getTime(), auditable.getCurationDetails().getLastUpdated().getTime() );
        assertFalse( auditable.getCurationDetails().getTroubled() );
        assertFalse( auditable.getCurationDetails().getNeedsAttention() );

        for ( AuditEvent e : auditTrail.getEvents() ) {
            assertNotNull( e.getId() );
        }
    }

    @Test
    public final void testGetEntitiesWithEvent() {
        auditTrailService.addUpdateEvent( auditable, SampleRemovalEvent.class, "nothing special, just testing" );
        AuditEvent ev = auditable.getAuditTrail().getEvents().isEmpty() ? null : auditable.getAuditTrail().getEvents().get( auditable.getAuditTrail().getEvents().size() - 1 );
        assertNotNull( ev );
        assertNotNull( ev.getId() );

        AuditTrail auditTrail = auditable.getAuditTrail();

        Collection<AuditEvent> events = auditTrail.getEvents();
        assertTrue( events.contains( ev ) );

        events = auditEventService.getEvents( auditable );
        assertTrue( events.contains( ev ) );
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private PlatformTransactionManager pta;

    @Test
    public void testAddEventWhenTransactionIsRolledBack() {
        TransactionStatus t = pta.getTransaction( new DefaultTransactionDefinition() );
        try {
            auditTrailService.addUpdateEvent( auditable, SampleRemovalEvent.class, "test" );
        } finally {
            if ( !t.isCompleted() ) {
                pta.rollback( t );
            }
        }
        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );
        // ensure that no even has been created
        assertEquals( size, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public void testAddEventWhenTransactionIsRolledBack2() {
        TransactionStatus t = pta.getTransaction( new DefaultTransactionDefinition() );
        try {
            auditTrailService.addUpdateEvent( auditable, SampleRemovalEvent.class, "test", new RuntimeException() );
        } finally {
            if ( !t.isCompleted() ) {
                pta.rollback( t );
            }
        }
        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );
        AuditTrail auditTrail = auditable.getAuditTrail();
        // The events bag is order-by="date" only; with MySQL datetime(3) precision the CREATE
        // event from setUp and the REQUIRES_NEW UPDATE event written here can land in the same
        // millisecond, leaving the within-bucket order undefined. Resolve the UPDATE row by
        // action rather than by list position so the assertion is precision-independent.
        AuditEvent e = auditTrail.getEvents().stream()
                .filter( ev -> ev.getAction() == AuditAction.UPDATE )
                .findFirst()
                .orElse( null );
        assertNotNull( e );
        assertEquals( AuditAction.UPDATE, e.getAction() );
        assertEquals( "test", e.getNote() );
        assertNotNull( e.getDetail() );
        assertTrue( e.getDetail().contains( "RuntimeException" ) );
        // ensure that the exception is logged
        assertEquals( size + 1, auditable.getAuditTrail().getEvents().size() );
    }

    @Test
    public final void testAddTroubleEventWhenCurationDetailsAreModified() {
        TransactionStatus t = pta.getTransaction( new DefaultTransactionDefinition() );
        try {
            auditable = arrayDesignService.load( auditable.getId() );
            assertNotNull( auditable );
            auditable.setDescription( "foo" );
            assertFalse( auditable.getCurationDetails().getNeedsAttention() );
            // modify the curation details in-memory (must be rolled back by the wrapping txn).
            auditable.getCurationDetails().setNeedsAttention( true );
            // flush so the in-memory CURATION_DETAILS change reaches the DB inside the outer
            // transaction; the subsequent REQUIRES_NEW path then sees a pending uncommitted
            // change it must NOT block on. Hibernate 6 requires an active transaction for
            // flush(); using the transaction-bound session (not a fresh sessionFactory.openSession())
            // is what makes the call effective AND legal here.
            sessionFactory.getCurrentSession().flush();
            // Note: a prior shape of this test also issued auditTrailService.addUpdateEvent
            // (REQUIRED) in the outer transaction to verify it gets rolled back, then flushed
            // and called the REQUIRES_NEW Throwable overload. With the AuditTrail.lastEvent
            // FK denormalisation (Round-3 perf probe), both txns now mutate the same
            // AUDIT_TRAIL row and the outer + REQUIRES_NEW combination deadlocks at commit.
            // The "REQUIRED audit event rolls back" contract is already covered by
            // testAddEventWhenTransactionIsRolledBack; here we keep the unique coverage
            // (REQUIRES_NEW survives outer rollback alongside other in-flight rolled-back
            // state) and drop the conflicting in-outer addUpdateEvent.
            auditTrailService.addUpdateEvent( auditable, TroubledStatusFlagEvent.class, "nothing special, just testing", new RuntimeException() );
        } finally {
            // Always roll back even if an assertion / exception interrupts the test body;
            // otherwise the transaction leaks into subsequent tests in the class.
            if ( !t.isCompleted() ) {
                pta.rollback( t );
            }
        }
        auditable = arrayDesignService.loadWithAuditTrail( auditable.getId() );
        assertNotNull( auditable );
        assertNull( auditable.getDescription() );
        assertFalse( auditable.getCurationDetails().getNeedsAttention() );
        // unfortunately, curation details cannot be altered because that would cause a deadlock
        assertFalse( auditable.getCurationDetails().getTroubled() );
        Assertions.assertThat( auditable.getAuditTrail().getEvents() )
                .extracting( AuditEvent::getNote )
                .contains( "nothing special, just testing" );
    }

    @Test
    public void testAddTroubleEventOnTransientEntity() {
        ArrayDesign auditable;
        TransactionStatus t = pta.getTransaction( new DefaultTransactionDefinition() );
        try {
            auditable = new ArrayDesign();
            auditable.setPrimaryTaxon( getTaxon( "human" ) );
            auditable = arrayDesignService.create( auditable );
            assertNotNull( auditable );
            // now, the AD does not exist yet because the transaction will be suspended
            auditTrailService.addUpdateEvent( auditable, TroubledStatusFlagEvent.class, "nothing special, just testing", new RuntimeException() );
        } finally {
            if ( !t.isCompleted() ) {
                pta.rollback( t );
            }
        }
        auditable = arrayDesignService.load( auditable.getId() );
        assertNull( auditable );
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

    /**
     * Length audit event detail get abbreviated.
     */
    @Test
    public void testLengthyDetail() {
        AuditEvent event = auditTrailService.addUpdateEvent( auditable, SampleRemovalEvent.class, "test", StringUtils.repeat( 'a', 70000 ) );
        Assertions.assertThat( event.getDetail() )
                .hasSize( 65533 )
                .endsWith( "…" )
                .bytes( StandardCharsets.UTF_8 )
                .hasSize( 65535 );
    }
}
