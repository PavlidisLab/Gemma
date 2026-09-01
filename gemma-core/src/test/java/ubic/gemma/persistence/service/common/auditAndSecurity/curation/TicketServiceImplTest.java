/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetStatus;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.eventType.TicketOpenedEvent;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lightweight Mockito unit tests for {@link TicketServiceImpl} covering the
 * Phase B-1 happy paths (open / assign / addComment / transition) and verifying
 * that each mutating call appends the correct {@link TicketEventType} to the
 * ticket's append-only event log. The DAO is mocked — these aren't integration
 * tests; persistence is exercised in a follow-on DAO test once the schema
 * lands.
 */
@ExtendWith(MockitoExtension.class)
public class TicketServiceImplTest {

    @Mock
    private TicketDao ticketDao;

    @Mock
    private AuditTrailService auditTrailService;

    @InjectMocks
    private TicketServiceImpl service;

    private Contact reporter;
    private Contact assignee;
    private TicketTarget eeTarget;

    @BeforeEach
    public void setUp() {
        reporter = new Contact();
        reporter.setId( 11L );
        reporter.setName( "Reporter" );
        assignee = new Contact();
        assignee.setId( 22L );
        assignee.setName( "Assignee" );
        eeTarget = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 100L );
    }

    /** Make ticketDao.create echo the argument back so the test holds a reference identical to what the service returned. */
    private void stubDaoCreateEchoes() {
        when( ticketDao.create( any( Ticket.class ) ) ).thenAnswer( inv -> inv.getArgument( 0 ) );
    }

    /** Make ticketDao.save echo the argument back so the test holds a reference identical to what the service returned. */
    private void stubDaoSaveEchoes() {
        when( ticketDao.save( any( Ticket.class ) ) ).thenAnswer( inv -> inv.getArgument( 0 ) );
    }

    @Test
    public void openTicket_seedsOpenedEvent_andDelegatesToCreate() {
        stubDaoCreateEchoes();
        Ticket t = service.openTicket( reporter,
                TicketType.BATCH_INFO_NEEDED,
                "missing batch info",
                Collections.singleton( eeTarget ) );

        assertNotNull( t );
        assertEquals( TicketState.OPEN, t.getState() );
        assertSame( reporter, t.getReporter() );
        assertEquals( "missing batch info", t.getTitle() );
        assertEquals( 1, t.getTargets().size() );
        assertSame( t, eeTarget.getTicket() );

        // exactly one event of type OPENED was appended
        assertEquals( 1, t.getEvents().size() );
        TicketEvent e = t.getEvents().iterator().next();
        assertEquals( TicketEventType.OPENED, e.getType() );
        assertSame( reporter, e.getActor() );
        assertSame( t, e.getTicket() );

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass( Ticket.class );
        verify( ticketDao ).create( captor.capture() );
        assertSame( t, captor.getValue() );

        // openTicket also writes a companion AuditTrail row (inline, because
        // @Audited can't target a method with no Auditable arg).
        verify( auditTrailService ).addUpdateEvent( eq( t ), eq( TicketOpenedEvent.class ),
                org.mockito.ArgumentMatchers.contains( "missing batch info" ) );
    }

    @Test
    public void openTicket_rejectsEmptyTargets() {
        assertThrows( IllegalArgumentException.class, () -> service.openTicket(
                reporter, TicketType.GENERIC, "x", Collections.emptyList() ) );
    }

    @Test
    public void openTicket_rejectsBlankTitle() {
        assertThrows( IllegalArgumentException.class, () -> service.openTicket(
                reporter, TicketType.GENERIC, "   ", Collections.singleton( eeTarget ) ) );
    }

    @Test
    public void assign_setsAssignee_andAppendsAssignedEvent() {
        stubDaoCreateEchoes();
        stubDaoSaveEchoes();
        Ticket t = openHelper();
        int eventsBefore = t.getEvents().size();

        Ticket result = service.assign( t, reporter, assignee );

        assertSame( assignee, result.getAssignee() );
        assertEquals( eventsBefore + 1, result.getEvents().size() );
        assertTrue( containsEventOfType( result, TicketEventType.ASSIGNED ) );
        verify( ticketDao ).save( t );
    }

    @Test
    public void assign_withNullAssignee_clearsAssignment_andStillLogsEvent() {
        stubDaoCreateEchoes();
        stubDaoSaveEchoes();
        Ticket t = openHelper();
        t.setAssignee( assignee );
        int eventsBefore = t.getEvents().size();

        Ticket result = service.assign( t, reporter, null );

        assertEquals( null, result.getAssignee() );
        assertEquals( eventsBefore + 1, result.getEvents().size() );
        assertTrue( containsEventOfType( result, TicketEventType.ASSIGNED ) );
    }

    @Test
    public void addComment_appendsCommentedEvent_withPayload() {
        stubDaoCreateEchoes();
        stubDaoSaveEchoes();
        Ticket t = openHelper();
        int eventsBefore = t.getEvents().size();

        Ticket result = service.addComment( t, reporter, "{\"text\":\"hi\"}" );

        assertEquals( eventsBefore + 1, result.getEvents().size() );
        TicketEvent latest = mostRecentEventOfType( result, TicketEventType.COMMENTED );
        assertNotNull( latest );
        // appendEvent JSON-encodes the payload so MySQL's JSON column accepts it; a
        // pre-formed JSON string therefore gets re-encoded as a JSON string literal.
        // No current caller passes pre-formed JSON; the contract is "free-form string in,
        // JSON-string out". See TicketServiceImpl#appendEvent.
        assertEquals( "\"{\\\"text\\\":\\\"hi\\\"}\"", latest.getPayload() );
        verify( ticketDao ).save( t );
    }

    @Test
    public void transition_OPEN_to_IN_PROGRESS_emitsStateChanged() {
        verifyTransitionEmitsEvent( TicketState.OPEN, TicketState.IN_PROGRESS, TicketEventType.STATE_CHANGED );
    }

    @Test
    public void transition_IN_PROGRESS_to_RESOLVED_emitsResolved() {
        verifyTransitionEmitsEvent( TicketState.IN_PROGRESS, TicketState.RESOLVED, TicketEventType.RESOLVED );
    }

    @Test
    public void transition_OPEN_to_CANCELLED_emitsCancelled() {
        verifyTransitionEmitsEvent( TicketState.OPEN, TicketState.CANCELLED, TicketEventType.CANCELLED );
    }

    @Test
    public void transition_RESOLVED_to_OPEN_emitsReopened() {
        verifyTransitionEmitsEvent( TicketState.RESOLVED, TicketState.OPEN, TicketEventType.REOPENED );
    }

    @Test
    public void transition_noop_is_silent() {
        stubDaoCreateEchoes();
        Ticket t = openHelper();
        int eventsBefore = t.getEvents().size();

        Ticket result = service.transition( t, TicketState.OPEN, reporter, null );

        assertSame( t, result );
        assertEquals( eventsBefore, result.getEvents().size(), "no-op transitions must not append events" );
    }

    @Test
    public void terminalStatesMapToTypedEvents_table() {
        // Compact sanity table — every state must land on a definite event type
        Map<TicketState, TicketEventType> expected = new EnumMap<>( TicketState.class );
        expected.put( TicketState.IN_PROGRESS, TicketEventType.STATE_CHANGED );
        expected.put( TicketState.RESOLVED, TicketEventType.RESOLVED );
        expected.put( TicketState.CANCELLED, TicketEventType.CANCELLED );
        for ( Map.Entry<TicketState, TicketEventType> e : expected.entrySet() ) {
            verifyTransitionEmitsEvent( TicketState.OPEN, e.getKey(), e.getValue() );
        }
    }

    // ---- extended filter dispatch -----------------------------------------

    /**
     * Extended {@code findTickets} dispatches each of the four new filter
     * arguments (type / state / targetType / updatedSince) through to the DAO
     * unchanged, preserving the {@code state} value (it doesn't get coerced
     * into the legacy openOnly path).
     */
    @Test
    public void findTicketsExtended_passesAllFiltersThroughToDao() {
        Date since = new Date( 1000L );
        when( ticketDao.findTickets( eq( false ), eq( 7L ), eq( TicketPriority.HIGH ),
                eq( TicketType.QUALITY_REVIEW ), eq( TicketState.RESOLVED ),
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ), eq( since ), eq( 5 ), eq( 25 ) ) )
                .thenReturn( Collections.emptyList() );

        service.findTickets( false, 7L, TicketPriority.HIGH,
                TicketType.QUALITY_REVIEW, TicketState.RESOLVED,
                TicketTargetType.EXPRESSION_EXPERIMENT, since, 5, 25 );

        verify( ticketDao ).findTickets( false, 7L, TicketPriority.HIGH,
                TicketType.QUALITY_REVIEW, TicketState.RESOLVED,
                TicketTargetType.EXPRESSION_EXPERIMENT, since, 5, 25 );
    }

    /**
     * Extended {@code countTickets} mirrors {@code findTickets} in dispatch —
     * each filter argument lands on the DAO without massaging.
     */
    @Test
    public void countTicketsExtended_passesAllFiltersThroughToDao() {
        Date since = new Date( 2000L );
        when( ticketDao.countTickets( eq( true ), eq( ( Long ) null ), eq( ( TicketPriority ) null ),
                eq( ( TicketType ) null ), eq( TicketState.OPEN ),
                eq( ( TicketTargetType ) null ), eq( since ) ) )
                .thenReturn( 3L );

        long n = service.countTickets( true, null, null, null, TicketState.OPEN, null, since );

        assertEquals( 3L, n );
        verify( ticketDao ).countTickets( true, null, null, null, TicketState.OPEN, null, since );
    }

    /**
     * Cursor-mode dispatch parity: extended cursor signature forwards each of
     * the four new filter arguments verbatim to the DAO.
     */
    @Test
    public void findTicketsByCursorExtended_passesAllFiltersThroughToDao() {
        Date since = new Date( 3000L );
        // The service walks the returned page to initialize each ticket for projection, so an
        // unstubbed DAO hands it null and it NPEs before reaching the assertion. Stub an empty
        // page: this test is about the filters reaching the DAO, and an empty result isolates
        // that from anything the initialization pass does.
        when( ticketDao.findTicketsByCursor( anyBoolean(), any(), any(), any(), any(), any(), any(), any(), anyInt() ) )
                .thenReturn( new CursorPage<>( Collections.emptyList(), null, 10, null, null, 0L ) );

        service.findTicketsByCursor( false, null, TicketPriority.LOW,
                TicketType.REALIGNMENT_NEEDED, TicketState.IN_PROGRESS,
                TicketTargetType.ARRAY_DESIGN, since, null, 10 );

        verify( ticketDao ).findTicketsByCursor( false, null, TicketPriority.LOW,
                TicketType.REALIGNMENT_NEEDED, TicketState.IN_PROGRESS,
                TicketTargetType.ARRAY_DESIGN, since, null, 10 );
    }

    // ---- helpers -----------------------------------------------------------

    /** Opens a fresh ticket via the real service path so the test holds a
     *  reference identical to what the service has. */
    private Ticket openHelper() {
        return service.openTicket( reporter,
                TicketType.GENERIC,
                "t",
                Collections.singleton(
                        TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 100L ) ) );
    }

    private void verifyTransitionEmitsEvent( TicketState from, TicketState to, TicketEventType expected ) {
        stubDaoCreateEchoes();
        stubDaoSaveEchoes();
        Ticket t = openHelper();
        t.setState( from );
        int eventsBefore = t.getEvents().size();

        Ticket result = service.transition( t, to, reporter, "because" );

        assertEquals( to, result.getState() );
        assertEquals( eventsBefore + 1, result.getEvents().size() );
        TicketEvent latest = mostRecentEventOfType( result, expected );
        assertNotNull( latest, "Expected an event of type " + expected + " after " + from + "->" + to );
        // appendEvent JSON-encodes the payload string ("because" → "\"because\"") so MySQL's
        // JSON column accepts it. See TicketServiceImpl#appendEvent.
        assertEquals( "\"because\"", latest.getPayload() );
    }

    private static boolean containsEventOfType( Ticket t, TicketEventType type ) {
        return mostRecentEventOfType( t, type ) != null;
    }

    @Test
    public void updateMetadata_bumpsUpdatedAt_andDoesNotAppendTicketEvent() {
        // Transient ticket (no id) so reattach short-circuits and the mutated
        // arg is the one saved — keeps the test free of DAO load stubs.
        Ticket t = Ticket.Factory.newInstance( TicketType.CURATION, "metadata-edit", reporter );
        t.setPriority( TicketPriority.NORMAL );
        Date before = t.getUpdatedAt();
        int eventsBefore = t.getEvents().size();
        stubDaoSaveEchoes();

        // Mutate metadata then call updateMetadata; the changedFields string
        // shows up in the audit-trail NOTE (verified in the IT).
        t.setPriority( TicketPriority.URGENT );
        Ticket saved = service.updateMetadata( t, "priority" );

        assertSame( t, saved );
        assertEquals( TicketPriority.URGENT, saved.getPriority() );
        assertEquals( eventsBefore, saved.getEvents().size(),
                "metadata edits MUST NOT append a TicketEvent (Decision 4 of AUDIT_AS_WORKFLOW_RECCE.md)" );
        assertTrue( saved.getUpdatedAt() == null || before == null
                        || saved.getUpdatedAt().getTime() >= before.getTime(),
                "updatedAt should advance" );
        verify( ticketDao ).save( t );
    }

    @Test
    public void addTarget_appendsTheTarget_andLogsTargetAdded() {
        Ticket t = Ticket.Factory.newInstance( TicketType.CURATION, "scratchpad", reporter );
        t.setAcceptsTargets( true );
        int eventsBefore = t.getEvents().size();
        stubDaoSaveEchoes();

        Ticket saved = service.addTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 4242L, reporter );

        assertEquals( 1, saved.getTargets().size() );
        TicketTarget added = saved.getTargets().iterator().next();
        assertEquals( 4242L, added.getTargetId() );
        assertEquals( TicketTargetType.EXPRESSION_EXPERIMENT, added.getTargetType() );
        assertEquals( TicketTargetStatus.NOT_DONE, added.getStatus(), "a freshly added target is not done" );
        assertSame( saved, added.getTicket(), "the back-reference must be set or the row orphans" );
        assertEquals( eventsBefore + 1, saved.getEvents().size() );
        assertTrue( containsEventOfType( saved, TicketEventType.TARGET_ADDED ) );
    }

    /**
     * The flag is the whole gate. False is the default and the state of every ticket that predates it,
     * so without this check an agent-created ticket's fixed batch could silently grow.
     */
    @Test
    public void addTarget_refusesWhenTheTicketDoesNotAcceptAdditions() {
        Ticket t = Ticket.Factory.newInstance( TicketType.CURATION, "fixed-batch", reporter );
        assertFalse( t.isAcceptsTargets(), "the flag must default to false" );

        assertThrows( IllegalStateException.class,
                () -> service.addTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 4242L, reporter ) );
        assertTrue( t.getTargets().isEmpty(), "nothing may be added when the gate refuses" );
        verify( ticketDao, never() ).save( any( Ticket.class ) );
    }

    /** State wins over the flag: a finished ticket must not quietly grow new work. */
    @Test
    public void addTarget_refusesOnAResolvedTicketEvenWithTheFlagOn() {
        Ticket t = Ticket.Factory.newInstance( TicketType.CURATION, "done", reporter );
        t.setAcceptsTargets( true );
        t.setState( TicketState.RESOLVED );

        assertThrows( IllegalStateException.class,
                () -> service.addTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 4242L, reporter ) );
        assertTrue( t.getTargets().isEmpty() );
        assertTrue( t.isAcceptsTargets(), "the flag must be left alone so reopening restores it" );
        verify( ticketDao, never() ).save( any( Ticket.class ) );
    }

    /**
     * Idempotent, NOT a conflict. uib's argument, which is the deciding one: the client cannot know
     * membership at click time — a menu may have been open for a minute — and a curator clicking twice
     * must not get an error for reaching the state they asked for, nor a duplicate row on a
     * 500-target ticket.
     */
    @Test
    public void addTarget_isIdempotentOnADuplicate() {
        Ticket t = Ticket.Factory.newInstance( TicketType.SCRATCHPAD, "scratchpad", reporter );
        t.setAcceptsTargets( true );
        TicketTarget existing = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 4242L );
        existing.setTicket( t );
        t.getTargets().add( existing );

        Ticket saved = service.addTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 4242L, reporter );

        assertEquals( 1, saved.getTargets().size(), "the duplicate must not be appended" );
        assertFalse( containsEventOfType( saved, TicketEventType.TARGET_ADDED ),
                "a no-op must not pollute the event log" );
        verify( ticketDao, never() ).save( any( Ticket.class ) );
    }

    /** A cancelled ticket is as finished as a resolved one; both refuse target changes. */
    @Test
    public void addTarget_refusesOnACancelledTicket() {
        Ticket t = Ticket.Factory.newInstance( TicketType.CURATION, "cancelled", reporter );
        t.setAcceptsTargets( true );
        t.setState( TicketState.CANCELLED );

        assertThrows( IllegalStateException.class,
                () -> service.addTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 4242L, reporter ) );
        assertTrue( t.getTargets().isEmpty() );
    }

    /**
     * On a scratchpad, removing IS finishing (Paul, 2026-08-31) — the ticket stays open and the dataset
     * leaves it — so this is the counterpart of addTarget, not an afterthought.
     */
    @Test
    public void removeTarget_removesMembership_andLogsTargetRemoved() {
        Ticket t = Ticket.Factory.newInstance( TicketType.SCRATCHPAD, "scratchpad", reporter );
        t.setAcceptsTargets( true );
        TicketTarget tgt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 4242L );
        tgt.setStatus( TicketTargetStatus.NOT_DONE );
        tgt.setTicket( t );
        t.getTargets().add( tgt );
        stubDaoSaveEchoes();

        TicketTargetStatus removed = service.removeTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 4242L, reporter );

        assertEquals( TicketTargetStatus.NOT_DONE, removed, "the caller is told what it discarded" );
        assertTrue( t.getTargets().isEmpty() );
        assertTrue( containsEventOfType( t, TicketEventType.TARGET_REMOVED ),
                "membership goes, the history stays" );
    }

    /** Removing something that is not there has already reached the asked-for state. */
    @Test
    public void removeTarget_isIdempotentWhenAbsent() {
        Ticket t = Ticket.Factory.newInstance( TicketType.SCRATCHPAD, "scratchpad", reporter );
        t.setAcceptsTargets( true );

        assertNull( service.removeTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 4242L, reporter ) );
        verify( ticketDao, never() ).save( any( Ticket.class ) );
    }

    /**
     * A completed target may be removed — a scratchpad's rows are all NOT_DONE and refusing would make
     * the common case pay for the rare one. The status comes back so the caller can say what went.
     */
    @Test
    public void removeTarget_allowsRemovingCompletedWork_butReportsIt() {
        Ticket t = Ticket.Factory.newInstance( TicketType.CURATION, "worklist", reporter );
        t.setAcceptsTargets( true );
        TicketTarget tgt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 99L );
        tgt.setStatus( TicketTargetStatus.DONE );
        tgt.setTicket( t );
        t.getTargets().add( tgt );
        stubDaoSaveEchoes();

        assertEquals( TicketTargetStatus.DONE,
                service.removeTarget( t, TicketTargetType.EXPRESSION_EXPERIMENT, 99L, reporter ) );
        assertTrue( t.getTargets().isEmpty() );
    }

    @Test
    public void updateTargetStatus_appendsTargetStatusChangedEvent_andBumpsUpdatedAt() {
        // Multi-target ticket; agent marks one target DONE while the other stays NOT_DONE.
        Ticket t = Ticket.Factory.newInstance( TicketType.LITERATURE_SEARCH, "target-status", reporter );
        TicketTarget tgt1 = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 101L );
        tgt1.setStatus( TicketTargetStatus.NOT_DONE );
        // Simulate a persisted row id so updateTargetStatus can find it.
        tgt1.setId( 501L );
        TicketTarget tgt2 = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 102L );
        tgt2.setStatus( TicketTargetStatus.NOT_DONE );
        tgt2.setId( 502L );
        t.getTargets().add( tgt1 );
        t.getTargets().add( tgt2 );
        int eventsBefore = t.getEvents().size();
        stubDaoSaveEchoes();

        Ticket result = service.updateTargetStatus( t, 501L, TicketTargetStatus.DONE, reporter );

        assertSame( t, result );
        assertEquals( TicketTargetStatus.DONE, tgt1.getStatus() );
        assertEquals( TicketTargetStatus.NOT_DONE, tgt2.getStatus(), "untouched target retains status" );

        // One TARGET_STATUS_CHANGED event appended.
        assertEquals( eventsBefore + 1, result.getEvents().size() );
        TicketEvent ev = mostRecentEventOfType( result, TicketEventType.TARGET_STATUS_CHANGED );
        assertNotNull( ev );
        assertSame( reporter, ev.getActor() );

        // Companion governance audit row is emitted inline via the mock.
        verify( auditTrailService ).addUpdateEvent(
                eq( t ),
                eq( ubic.gemma.model.common.auditAndSecurity.eventType.TicketTargetStatusChangedEvent.class ),
                org.mockito.ArgumentMatchers.contains( "NOT_DONE -> DONE" ) );
    }

    @Test
    public void updateTargetStatus_noOp_whenAlreadyAtRequestedStatus() {
        Ticket t = Ticket.Factory.newInstance( TicketType.LITERATURE_SEARCH, "noop-target", reporter );
        TicketTarget tgt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 101L );
        tgt.setStatus( TicketTargetStatus.DONE );
        tgt.setId( 501L );
        t.getTargets().add( tgt );
        int eventsBefore = t.getEvents().size();

        Ticket result = service.updateTargetStatus( t, 501L, TicketTargetStatus.DONE, reporter );

        assertSame( t, result );
        assertEquals( eventsBefore, result.getEvents().size(), "no-op MUST NOT append an event" );
        org.mockito.Mockito.verifyNoInteractions( auditTrailService );
    }

    @Test
    public void updateTargetStatus_unknownTargetId_throwsIAE() {
        // Leave the ticket transient (id=null) so reattach short-circuits and
        // returns the arg — the test exercises the "no matching target row" branch.
        Ticket t = Ticket.Factory.newInstance( TicketType.LITERATURE_SEARCH, "missing-target", reporter );
        TicketTarget tgt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 101L );
        tgt.setId( 501L );
        t.getTargets().add( tgt );

        assertThrows( IllegalArgumentException.class, () ->
                service.updateTargetStatus( t, 9999L, TicketTargetStatus.DONE, reporter ) );
    }

    private static TicketEvent mostRecentEventOfType( Ticket t, TicketEventType type ) {
        java.util.List<TicketEvent> events = t.getEvents();
        TicketEvent best = null;
        for ( TicketEvent e : events ) {
            if ( e.getType() == type ) {
                if ( best == null || e.getOccurredAt().compareTo( best.getOccurredAt() ) >= 0 ) {
                    best = e;
                }
            }
        }
        return best;
    }
}
