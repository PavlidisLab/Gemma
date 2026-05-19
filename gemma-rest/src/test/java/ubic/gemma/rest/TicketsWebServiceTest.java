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
package ubic.gemma.rest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;

import jakarta.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link TicketsWebService} covering the
 * read-only endpoints introduced in Phase B-2 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}. Does NOT stand up Jersey; the focus is
 * the WebService → service wiring and the VO projection shape.
 *
 * @author paul
 */
@RunWith(MockitoJUnitRunner.class)
public class TicketsWebServiceTest {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketsWebService webService;

    private User reporter;
    private Ticket ticket;

    @Before
    public void setUp() {
        reporter = new User();
        reporter.setId( 42L );
        reporter.setName( "alice" );

        ticket = Ticket.Factory.newInstance( TicketType.GENERIC, "Test ticket", reporter );
        ticket.setId( 1L );
        ticket.setState( TicketState.OPEN );
        ticket.setPriority( TicketPriority.HIGH );
        ticket.setCreatedAt( new Date() );
        ticket.setUpdatedAt( new Date() );

        TicketTarget tt = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 99L );
        tt.setTicket( ticket );
        ticket.setTargets( new HashSet<>( Collections.singletonList( tt ) ) );

        TicketEvent ev = TicketEvent.Factory.newInstance( TicketEventType.OPENED, reporter, null );
        ev.setTicket( ticket );
        ev.setOccurredAt( new Date() );
        ticket.setEvents( new HashSet<>( Collections.singletonList( ev ) ) );
    }

    @Test
    public void testGetTickets_passesFiltersThrough() {
        when( ticketService.findTickets( eq( true ), eq( 42L ), eq( TicketPriority.HIGH ), anyInt(), anyInt() ) )
                .thenReturn( Collections.singletonList( ticket ) );
        when( ticketService.countTickets( eq( true ), eq( 42L ), eq( TicketPriority.HIGH ) ) ).thenReturn( 1L );

        PaginatedResponseDataObject<TicketValueObject> resp = webService.getTickets(
                true, 42L, TicketPriority.HIGH,
                ubic.gemma.rest.util.args.OffsetArg.valueOf( "0" ),
                ubic.gemma.rest.util.args.LimitArg.valueOf( "20" ) );

        assertThat( resp.getData() ).hasSize( 1 );
        assertThat( resp.getData().get( 0 ).getTitle() ).isEqualTo( "Test ticket" );
        // list view omits events
        assertThat( resp.getData().get( 0 ).getEvents() ).isEmpty();
        // targets are present
        assertThat( resp.getData().get( 0 ).getTargets() ).hasSize( 1 );

        verify( ticketService ).findTickets( true, 42L, TicketPriority.HIGH, 0, 20 );
        verify( ticketService ).countTickets( true, 42L, TicketPriority.HIGH );
    }

    @Test
    public void testGetTickets_defaultsAreNullFilters() {
        when( ticketService.findTickets( anyBoolean(), any(), any(), anyInt(), anyInt() ) )
                .thenReturn( Collections.emptyList() );
        when( ticketService.countTickets( anyBoolean(), any(), any() ) ).thenReturn( 0L );

        webService.getTickets(
                false, null, null,
                ubic.gemma.rest.util.args.OffsetArg.valueOf( "0" ),
                ubic.gemma.rest.util.args.LimitArg.valueOf( "20" ) );

        verify( ticketService ).findTickets( false, null, null, 0, 20 );
    }

    @Test
    public void testGetTicket_includesEvents() {
        when( ticketService.load( 1L ) ).thenReturn( ticket );

        ResponseDataObject<TicketValueObject> resp = webService.getTicket( 1L );

        assertThat( resp.getData() ).isNotNull();
        assertThat( resp.getData().getId() ).isEqualTo( 1L );
        assertThat( resp.getData().getEvents() ).hasSize( 1 );
        assertThat( resp.getData().getEvents().get( 0 ).getType() ).isEqualTo( TicketEventType.OPENED );
        assertThat( resp.getData().getEvents().get( 0 ).getActorName() ).isEqualTo( "alice" );
    }

    @Test
    public void testGetTicket_notFound() {
        when( ticketService.load( 999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.getTicket( 999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void testGetTicketEvents_returnsEventList() {
        when( ticketService.load( 1L ) ).thenReturn( ticket );

        ResponseDataObject<List<TicketEventValueObject>> resp = webService.getTicketEvents( 1L );

        assertThat( resp.getData() ).hasSize( 1 );
        assertThat( resp.getData().get( 0 ).getType() ).isEqualTo( TicketEventType.OPENED );
    }

    @Test
    public void testGetTicketEvents_notFound() {
        when( ticketService.load( 999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.getTicketEvents( 999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void testOpenTicketsForExpressionExperiment_delegatesToService() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, 99L ) )
                .thenReturn( Arrays.asList( ticket ) );

        List<TicketValueObject> vos = webService.openTicketsForExpressionExperiment( 99L );

        assertThat( vos ).hasSize( 1 );
        assertThat( vos.get( 0 ).getId() ).isEqualTo( 1L );
    }

    @Test
    public void testOpenTicketsForArrayDesign_delegatesToService() {
        when( ticketService.findOpenForTarget( TicketTargetType.ARRAY_DESIGN, 7L ) )
                .thenReturn( Collections.singletonList( ticket ) );

        List<TicketValueObject> vos = webService.openTicketsForArrayDesign( 7L );

        assertThat( vos ).hasSize( 1 );
    }
}
