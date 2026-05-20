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
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        assertEquals( "{\"text\":\"hi\"}", latest.getPayload() );
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
        assertEquals( "because", latest.getPayload() );
    }

    private static boolean containsEventOfType( Ticket t, TicketEventType type ) {
        return mostRecentEventOfType( t, type ) != null;
    }

    private static TicketEvent mostRecentEventOfType( Ticket t, TicketEventType type ) {
        Set<TicketEvent> events = t.getEvents();
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
