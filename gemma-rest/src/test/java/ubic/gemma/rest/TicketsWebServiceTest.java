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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
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
import ubic.gemma.rest.util.args.LimitArg;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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
@ExtendWith(MockitoExtension.class)
public class TicketsWebServiceTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private UserManager userManager;

    @Mock
    private UserReadService userReadService;

    @InjectMocks
    private TicketsWebService webService;

    private User reporter;
    private Ticket ticket;

    @BeforeEach
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
        when( ticketService.findTickets( eq( true ), eq( 42L ), eq( TicketPriority.HIGH ),
                eq( ( ubic.gemma.model.common.auditAndSecurity.curation.TicketType ) null ),
                eq( ( ubic.gemma.model.common.auditAndSecurity.curation.TicketState ) null ),
                eq( ( ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType ) null ),
                eq( ( java.util.Date ) null ), anyInt(), anyInt() ) )
                .thenReturn( Collections.singletonList( ticket ) );
        when( ticketService.countTickets( eq( true ), eq( 42L ), eq( TicketPriority.HIGH ),
                eq( ( ubic.gemma.model.common.auditAndSecurity.curation.TicketType ) null ),
                eq( ( ubic.gemma.model.common.auditAndSecurity.curation.TicketState ) null ),
                eq( ( ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType ) null ),
                eq( ( java.util.Date ) null ) ) ).thenReturn( 1L );

        // Cursor parameter added in step 1o (CURSOR_PAGINATION_STEP1_PLAN.md); legacy
        // offset-mode tests pass null and cast the Object result back to the legacy
        // PaginatedResponseDataObject (cursor-mode tests live in TicketsWebServiceCursorTest).
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<TicketValueObject> resp = ( PaginatedResponseDataObject<TicketValueObject> ) webService.getTickets(
                true, 42L, TicketPriority.HIGH,
                null, null, null, null,
                ubic.gemma.rest.util.args.OffsetArg.valueOf( "0" ),
                ubic.gemma.rest.util.args.LimitArg.valueOf( "20" ),
                null );

        assertThat( resp.getData() ).hasSize( 1 );
        assertThat( resp.getData().get( 0 ).getTitle() ).isEqualTo( "Test ticket" );
        // list view omits events
        assertThat( resp.getData().get( 0 ).getEvents() ).isEmpty();
        // targets are present
        assertThat( resp.getData().get( 0 ).getTargets() ).hasSize( 1 );

        verify( ticketService ).findTickets( true, 42L, TicketPriority.HIGH, null, null, null, null, 0, 20 );
        verify( ticketService ).countTickets( true, 42L, TicketPriority.HIGH, null, null, null, null );
    }

    @Test
    public void testGetTickets_defaultsAreNullFilters() {
        when( ticketService.findTickets( anyBoolean(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt() ) )
                .thenReturn( Collections.emptyList() );
        when( ticketService.countTickets( anyBoolean(), any(), any(), any(), any(), any(), any() ) ).thenReturn( 0L );

        webService.getTickets(
                false, null, null,
                null, null, null, null,
                ubic.gemma.rest.util.args.OffsetArg.valueOf( "0" ),
                ubic.gemma.rest.util.args.LimitArg.valueOf( "20" ),
                null );

        verify( ticketService ).findTickets( false, null, null, null, null, null, null, 0, 20 );
    }

    @Test
    public void getTickets_passesNewFilterArgs_through() {
        Date since = new Date( 1700000000000L );
        when( ticketService.findTickets( eq( true ), isNull(), isNull(),
                eq( TicketType.GENERIC ), eq( TicketState.OPEN ),
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ),
                any( Date.class ), anyInt(), anyInt() ) )
                .thenReturn( Collections.emptyList() );
        when( ticketService.countTickets( eq( true ), isNull(), isNull(),
                eq( TicketType.GENERIC ), eq( TicketState.OPEN ),
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ),
                any( Date.class ) ) ).thenReturn( 0L );

        webService.getTickets(
                true, null, null,
                TicketType.GENERIC, TicketState.OPEN, TicketTargetType.EXPRESSION_EXPERIMENT, since,
                ubic.gemma.rest.util.args.OffsetArg.valueOf( "0" ),
                ubic.gemma.rest.util.args.LimitArg.valueOf( "20" ),
                null );

        verify( ticketService ).findTickets(
                eq( true ), isNull(), isNull(),
                eq( TicketType.GENERIC ), eq( TicketState.OPEN ),
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ),
                any( Date.class ), anyInt(), anyInt() );
        verify( ticketService ).countTickets(
                eq( true ), isNull(), isNull(),
                eq( TicketType.GENERIC ), eq( TicketState.OPEN ),
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ),
                any( Date.class ) );
    }

    /* -------------- dashboard endpoints (/tickets/mine, /tickets/summary/me) -------------- */

    @Test
    public void getMyQueue_returnsOpenAndRecentlyResolved_forCallingUser() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );

        // First call: openOnly=true slice → 3 OPEN/IN_PROGRESS tickets
        List<Ticket> openSlice = new ArrayList<>();
        for ( int i = 0; i < 3; i++ ) {
            openSlice.add( newTicket( 100L + i, TicketState.OPEN, new Date() ) );
        }

        // Second call: openOnly=false superset → 5 mixed tickets:
        //   2 recently RESOLVED + 1 recently CANCELLED + 2 OPEN
        Date recent = new Date(); // within window
        List<Ticket> superset = new ArrayList<>();
        superset.add( newTicket( 200L, TicketState.RESOLVED, recent ) );
        superset.add( newTicket( 201L, TicketState.RESOLVED, recent ) );
        superset.add( newTicket( 202L, TicketState.CANCELLED, recent ) );
        superset.add( newTicket( 203L, TicketState.OPEN, recent ) );
        superset.add( newTicket( 204L, TicketState.OPEN, recent ) );

        // Service is called twice with different limits; openSlice for the small one,
        // superset for the large one. Use any-int matchers for limit; assert on call shape.
        when( ticketService.findTickets( eq( true ), eq( 42L ), isNull(), eq( 0 ), anyInt() ) )
                .thenReturn( openSlice );
        when( ticketService.findTickets( eq( false ), eq( 42L ), isNull(), eq( 0 ), anyInt() ) )
                .thenReturn( superset );

        ResponseDataObject<TicketsWebService.MyQueueResponse> resp = webService.getMyQueue( 50, 7 );

        assertThat( resp.getData() ).isNotNull();
        assertThat( resp.getData().assigneeContactId ).isEqualTo( 42L );
        assertThat( resp.getData().openCount ).isEqualTo( 3 );
        // 2 RESOLVED + 1 CANCELLED, all within window
        assertThat( resp.getData().recentlyResolvedCount ).isEqualTo( 3 );
        assertThat( resp.getData().open ).hasSize( 3 );
        assertThat( resp.getData().recentlyResolved ).hasSize( 3 );
    }

    @Test
    public void getMyQueue_returns401_whenAnonymous() {
        when( userManager.getCurrentUser() ).thenReturn( null );
        assertThatThrownBy( () -> webService.getMyQueue( 50, 7 ) )
                .isInstanceOf( NotAuthorizedException.class );
    }

    @Test
    public void getMyQueueSummary_returnsCountsAndOldestOpen() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );
        when( ticketService.countTickets( eq( true ), eq( 42L ), isNull() ) ).thenReturn( 5L );
        when( ticketService.countTickets( eq( false ), eq( 42L ), isNull() ) ).thenReturn( 12L );

        // oldestOpen slice: youngest first, but oldest age is what the endpoint computes.
        Ticket young = newTicket( 300L, TicketState.OPEN, new Date() );
        young.setCreatedAt( new Date() );
        Ticket old = newTicket( 301L, TicketState.OPEN, new Date() );
        // 10 days ago
        old.setCreatedAt( new Date( System.currentTimeMillis() - 10L * 24L * 3600L * 1000L ) );
        when( ticketService.findTickets( eq( true ), eq( 42L ), isNull(), eq( 0 ), anyInt() ) )
                .thenReturn( Arrays.asList( young, old ) );

        ResponseDataObject<TicketsWebService.MyQueueSummaryResponse> resp = webService.getMyQueueSummary();

        assertThat( resp.getData().assigneeContactId ).isEqualTo( 42L );
        assertThat( resp.getData().openCount ).isEqualTo( 5L );
        assertThat( resp.getData().totalCount ).isEqualTo( 12L );
        // oldestOpenAgeDays must be ~10 (allow ±1 for boundary)
        assertThat( resp.getData().oldestOpenAgeDays ).isNotNull();
        assertThat( resp.getData().oldestOpenAgeDays ).isBetween( 9L, 11L );
    }

    /** Helper: minimal Ticket carrying id, state, updatedAt. */
    private Ticket newTicket( long id, TicketState state, Date updatedAt ) {
        Ticket t = Ticket.Factory.newInstance( TicketType.GENERIC, "t-" + id, reporter );
        t.setId( id );
        t.setState( state );
        t.setCreatedAt( updatedAt );
        t.setUpdatedAt( updatedAt );
        t.setTargets( new HashSet<>() );
        t.setEvents( new HashSet<>() );
        return t;
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

        // Legacy mode (cursor=null) returns a ResponseDataObject<List<TicketEventValueObject>>.
        // Step 1r widened the static return type to Object; the legacy JSON shape is unchanged.
        @SuppressWarnings("unchecked")
        ResponseDataObject<List<TicketEventValueObject>> resp =
                ( ResponseDataObject<List<TicketEventValueObject>> ) webService
                        .getTicketEvents( 1L, null, LimitArg.valueOf( "20" ) );

        assertThat( resp.getData() ).hasSize( 1 );
        assertThat( resp.getData().get( 0 ).getType() ).isEqualTo( TicketEventType.OPENED );
    }

    @Test
    public void testGetTicketEvents_notFound() {
        when( ticketService.load( 999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.getTicketEvents( 999L, null, LimitArg.valueOf( "20" ) ) )
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

    /* -------------- write-side endpoints (POST/PUT/DELETE) -------------- */

    @Test
    public void testCreateTicket_happyPath() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );
        when( ticketService.openTicket( eq( reporter ), eq( TicketType.GENERIC ), eq( "Test ticket" ), any() ) )
                .thenReturn( ticket );

        TicketsWebService.CreateTicketRequest req = new TicketsWebService.CreateTicketRequest();
        req.setType( TicketType.GENERIC );
        req.setTitle( "Test ticket" );
        TicketsWebService.TicketTargetRequest tr = new TicketsWebService.TicketTargetRequest();
        tr.setTargetType( TicketTargetType.EXPRESSION_EXPERIMENT );
        tr.setTargetId( 99L );
        req.setTargets( Collections.singletonList( tr ) );

        Response resp = webService.createTicket( req );

        assertThat( resp.getStatus() ).isEqualTo( Response.Status.CREATED.getStatusCode() );
        Object entity = resp.getEntity();
        assertThat( entity ).isInstanceOf( ResponseDataObject.class );
        @SuppressWarnings("unchecked")
        ResponseDataObject<TicketValueObject> body = ( ResponseDataObject<TicketValueObject> ) entity;
        assertThat( body.getData().getId() ).isEqualTo( 1L );
        assertThat( body.getData().getEvents() ).hasSize( 1 );
        verify( ticketService ).openTicket( eq( reporter ), eq( TicketType.GENERIC ), eq( "Test ticket" ), any() );
    }

    @Test
    public void testCreateTicket_missingTitle_throws400() {
        // Title validation runs before user resolution; no stubbing needed.
        TicketsWebService.CreateTicketRequest req = new TicketsWebService.CreateTicketRequest();
        req.setType( TicketType.GENERIC );
        TicketsWebService.TicketTargetRequest tr = new TicketsWebService.TicketTargetRequest();
        tr.setTargetType( TicketTargetType.EXPRESSION_EXPERIMENT );
        tr.setTargetId( 99L );
        req.setTargets( Collections.singletonList( tr ) );

        assertThatThrownBy( () -> webService.createTicket( req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void testCreateTicket_nullBody_throws400() {
        assertThatThrownBy( () -> webService.createTicket( null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void testUpdateTicket_stateTransition() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );
        when( ticketService.load( 1L ) ).thenReturn( ticket );
        when( ticketService.transition( eq( ticket ), eq( TicketState.IN_PROGRESS ), eq( reporter ), any() ) )
                .thenReturn( ticket );

        TicketsWebService.UpdateTicketRequest req = new TicketsWebService.UpdateTicketRequest();
        req.setState( TicketState.IN_PROGRESS );
        req.setReason( "starting work" );

        ResponseDataObject<TicketValueObject> resp = webService.updateTicket( 1L, req );

        assertThat( resp.getData() ).isNotNull();
        verify( ticketService ).transition( ticket, TicketState.IN_PROGRESS, reporter, "starting work" );
        verify( ticketService, never() ).addComment( any(), any(), any() );
        verify( ticketService, never() ).assign( any(), any(), any() );
    }

    @Test
    public void testUpdateTicket_addComment() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );
        when( ticketService.load( 1L ) ).thenReturn( ticket );
        when( ticketService.addComment( eq( ticket ), eq( reporter ), eq( "looks good" ) ) ).thenReturn( ticket );

        TicketsWebService.UpdateTicketRequest req = new TicketsWebService.UpdateTicketRequest();
        req.setComment( "looks good" );

        webService.updateTicket( 1L, req );

        verify( ticketService ).addComment( ticket, reporter, "looks good" );
    }

    @Test
    public void testUpdateTicket_clearAssignee() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );
        when( ticketService.load( 1L ) ).thenReturn( ticket );
        when( ticketService.assign( eq( ticket ), eq( reporter ), isNull() ) ).thenReturn( ticket );

        TicketsWebService.UpdateTicketRequest req = new TicketsWebService.UpdateTicketRequest();
        req.setAssigneeId( null ); // explicit null → clear
        assertThat( req.isAssigneeIdSet() ).isTrue();

        webService.updateTicket( 1L, req );

        verify( ticketService ).assign( ticket, reporter, null );
    }

    @Test
    public void testUpdateTicket_notFound_throws404() {
        when( ticketService.load( 999L ) ).thenReturn( null );
        TicketsWebService.UpdateTicketRequest req = new TicketsWebService.UpdateTicketRequest();
        req.setState( TicketState.RESOLVED );
        assertThatThrownBy( () -> webService.updateTicket( 999L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void testDeleteTicket_softCancels() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );
        when( ticketService.load( 1L ) ).thenReturn( ticket );

        Response resp = webService.deleteTicket( 1L, "test cleanup" );

        assertThat( resp.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
        verify( ticketService ).transition( ticket, TicketState.CANCELLED, reporter, "test cleanup" );
    }

    @Test
    public void testDeleteTicket_alreadyCancelled_noTransition() {
        when( userManager.getCurrentUser() ).thenReturn( reporter );
        ticket.setState( TicketState.CANCELLED );
        when( ticketService.load( 1L ) ).thenReturn( ticket );

        Response resp = webService.deleteTicket( 1L, null );

        assertThat( resp.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
        verify( ticketService, never() ).transition( any(), any(), any(), any() );
    }

    @Test
    public void testDeleteTicket_notFound_throws404() {
        when( ticketService.load( 999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.deleteTicket( 999L, null ) )
                .isInstanceOf( NotFoundException.class );
    }

    /**
     * Annotation-level auth guard: the write-side endpoints must carry
     * {@code @PreAuthorize("isAuthenticated()")}. At runtime Spring's method-
     * security AOP turns the missing/anonymous principal into a 401/403; here
     * we verify the precondition that produces that runtime behaviour without
     * standing up the Spring container.
     */
    @Test
    public void testWriteEndpoints_requireAuthentication() throws NoSuchMethodException {
        assertPreAuthorizeIsAuthenticated(
                TicketsWebService.class.getMethod( "createTicket",
                        TicketsWebService.CreateTicketRequest.class ) );
        assertPreAuthorizeIsAuthenticated(
                TicketsWebService.class.getMethod( "updateTicket",
                        Long.class, TicketsWebService.UpdateTicketRequest.class ) );
        assertPreAuthorizeIsAuthenticated(
                TicketsWebService.class.getMethod( "deleteTicket",
                        Long.class, String.class ) );
    }

    private static void assertPreAuthorizeIsAuthenticated( java.lang.reflect.Method m ) {
        PreAuthorize annot = m.getAnnotation( PreAuthorize.class );
        assertThat( annot )
                .as( "method %s must carry @PreAuthorize", m.getName() )
                .isNotNull();
        assertThat( annot.value() )
                .as( "method %s must require isAuthenticated()", m.getName() )
                .contains( "isAuthenticated()" );
    }
}
