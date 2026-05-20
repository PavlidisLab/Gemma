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
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link TicketsWebService#getTickets(boolean, Long, TicketPriority, OffsetArg, LimitArg, CursorArg)}
 * as step 1o of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to
 * verify the WebService routes cursor vs offset modes to the right helper and emits the
 * right response wrapper, not to retest the DAO (the keyset HQL is covered separately).
 * <p>
 * Mirrors the pattern of the prior cursor-mode WebService tests (1b–1n): one test per
 * mode-routing branch, one test verifying the encoded cursor round-trips intact through
 * {@link CursorArg#valueOf(String)} into the service call, one test for empty pages.
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class TicketsWebServiceCursorTest {

    @Mock
    private TicketService ticketService;

    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
    private UserManager userManager;

    @Mock
    @SuppressWarnings("unused")
    private UserReadService userReadService;

    @InjectMocks
    private TicketsWebService webService;

    private User reporter;
    private Ticket ticket1;
    private Ticket ticket2;

    @Before
    public void setUp() {
        reporter = new User();
        reporter.setId( 42L );
        reporter.setName( "alice" );

        ticket1 = Ticket.Factory.newInstance( TicketType.GENERIC, "Test ticket 1", reporter );
        ticket1.setId( 10L );
        ticket1.setState( TicketState.OPEN );
        ticket1.setPriority( TicketPriority.HIGH );
        ticket1.setCreatedAt( new Date() );
        ticket1.setUpdatedAt( new Date() );
        TicketTarget tt1 = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 99L );
        tt1.setTicket( ticket1 );
        ticket1.setTargets( new HashSet<>( Collections.singletonList( tt1 ) ) );
        ticket1.setEvents( new HashSet<>() );

        ticket2 = Ticket.Factory.newInstance( TicketType.GENERIC, "Test ticket 2", reporter );
        ticket2.setId( 20L );
        ticket2.setState( TicketState.IN_PROGRESS );
        ticket2.setPriority( TicketPriority.LOW );
        ticket2.setCreatedAt( new Date() );
        ticket2.setUpdatedAt( new Date() );
        TicketTarget tt2 = TicketTarget.Factory.newInstance( TicketTargetType.ARRAY_DESIGN, 7L );
        tt2.setTicket( ticket2 );
        ticket2.setTargets( new HashSet<>( Collections.singletonList( tt2 ) ) );
        ticket2.setEvents( new HashSet<>() );
    }

    private OffsetArg offset( String s ) {
        return OffsetArg.valueOf( s );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsPaginatedResponse() {
        // Legacy path: TicketService.findTickets + countTickets → paginate(Slice).
        when( ticketService.findTickets( eq( false ), eq( ( Long ) null ), eq( ( TicketPriority ) null ), eq( 0 ), eq( 20 ) ) )
                .thenReturn( Arrays.asList( ticket1, ticket2 ) );
        when( ticketService.countTickets( eq( false ), eq( ( Long ) null ), eq( ( TicketPriority ) null ) ) ).thenReturn( 2L );

        Object response = webService.getTickets( false, null, null, offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( PaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<TicketValueObject> page =
                ( PaginatedResponseDataObject<TicketValueObject> ) response;
        assertThat( page.getData() ).hasSize( 2 );
        assertThat( page.getData().get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( page.getData().get( 1 ).getId() ).isEqualTo( 20L );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        // Cursor helper must not be touched in legacy mode.
        verify( ticketService, never() ).findTicketsByCursor( anyBoolean(), any(), any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 5L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Arrays.asList( ticket1, ticket2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( ticketService.findTicketsByCursor( eq( false ), eq( ( Long ) null ), eq( ( TicketPriority ) null ), eq( c ), eq( 20 ) ) )
                .thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getTickets( false, null, null, offset( "0" ), limit( "20" ), arg );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketValueObject> ) response;
        assertThat( page.getData() ).hasSize( 2 );
        assertThat( page.getData().get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( page.getData().get( 1 ).getId() ).isEqualTo( 20L );
        // list view omits events (same projection as offset mode)
        assertThat( page.getData().get( 0 ).getEvents() ).isEmpty();
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // Legacy helpers must not be touched in cursor mode.
        verify( ticketService, never() ).findTickets( anyBoolean(), any(), any(), anyInt(), anyInt() );
        verify( ticketService, never() ).countTickets( anyBoolean(), any(), any() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all
        // the way through to TicketService.findTicketsByCursor — verify the decoded Cursor
        // value arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Collections.singletonList( ticket2 ), null, 5, null, "prev", null );
        when( ticketService.findTicketsByCursor( eq( true ), eq( 42L ), eq( TicketPriority.HIGH ), eq( c ), eq( 5 ) ) )
                .thenReturn( cp );

        Object response = webService.getTickets( true, 42L, TicketPriority.HIGH,
                offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( ticketService ).findTicketsByCursor( true, 42L, TicketPriority.HIGH, c, 5 );
    }

    @Test
    public void cursorModeForwardsFilterTripleUnchanged() {
        // The filter triple (openOnly, assigneeId, priority) is honoured identically in
        // cursor mode; the WebService doesn't drop or substitute any component.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Collections.singletonList( ticket1 ), null, 10, null, null, null );
        when( ticketService.findTicketsByCursor( eq( true ), eq( 7L ), eq( TicketPriority.LOW ), eq( c ), eq( 10 ) ) )
                .thenReturn( cp );

        webService.getTickets( true, 7L, TicketPriority.LOW,
                offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( ticketService ).findTicketsByCursor( true, 7L, TicketPriority.LOW, c, 10 );
    }

    @Test
    public void cursorModeEmptyPageProducesEmptyResponseWithNoCursors() {
        // An empty CursorPage (no matches under the cursor predicate) round-trips through
        // the CursorPaginatedResponseDataObject wrapper without synthesizing cursor tokens.
        Cursor c = new Cursor( "+id", new Object[] { 999999L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Collections.emptyList(), null, 20, /* nextCursor */ null, /* prevCursor */ null, null );
        when( ticketService.findTicketsByCursor( anyBoolean(), any(), any(), eq( c ), eq( 20 ) ) )
                .thenReturn( cp );

        Object response = webService.getTickets( false, null, null,
                offset( "0" ), limit( "20" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isNull();
    }

    @Test
    public void cursorModePreservesAscendingIdOrderFromService() {
        // Cursor mode is forced to ascending id by the DAO; the WebService is responsible
        // for surfacing whatever order the service returns. Verify the page data preserves
        // the service's order (does not re-sort by the legacy updatedAt-desc rule).
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Arrays.asList( ticket1, ticket2 ), null, 10, null, null, null );
        when( ticketService.findTicketsByCursor( anyBoolean(), any(), any(), eq( c ), eq( 10 ) ) )
                .thenReturn( cp );

        Object response = webService.getTickets( false, null, null,
                offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketValueObject> ) response;
        List<TicketValueObject> data = page.getData();
        assertThat( data.get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( data.get( 1 ).getId() ).isEqualTo( 20L );
    }
}
