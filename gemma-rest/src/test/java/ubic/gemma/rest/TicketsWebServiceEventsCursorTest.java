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
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.LimitArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link TicketsWebService#getTicketEvents(Long, CursorArg, LimitArg)} as step 1r of
 * {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito &mdash; the goal is to verify
 * the WebService routes cursor vs legacy modes to the right helper and emits the
 * right response wrapper, not to retest the DAO (the keyset HQL is covered separately).
 * <p>
 * Mirrors the path-derived-constraint pattern of step 1q
 * ({@code DatasetsWebServiceAuditEventsCursorTest}): the legacy mode is NOT
 * offset-paginated for this endpoint &mdash; it returns an unpaginated
 * {@link ResponseDataObject}{@code <List<TicketEventValueObject>>}. The cursor branch
 * is therefore strictly additive (an opt-in via {@code ?cursor=}). The ticket scope
 * is preserved in both modes; cursor mode forces id-asc ordering inside the DAO.
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class TicketsWebServiceEventsCursorTest {

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

    private Ticket ticket;
    private TicketEvent event1;
    private TicketEvent event2;

    @BeforeEach
    public void setUp() {
        User reporter = new User();
        reporter.setId( 42L );
        reporter.setName( "alice" );

        ticket = Ticket.Factory.newInstance( TicketType.GENERIC, "Test ticket", reporter );
        ticket.setId( 100L );
        ticket.setState( TicketState.OPEN );
        ticket.setCreatedAt( new Date() );
        ticket.setUpdatedAt( new Date() );
        ticket.setEvents( new HashSet<>() );

        when( ticketService.load( 100L ) ).thenReturn( ticket );

        event1 = TicketEvent.Factory.newInstance( TicketEventType.OPENED, reporter, null );
        event1.setTicket( ticket );
        event1.setOccurredAt( new Date() );
        event1.setId( 10L );

        event2 = TicketEvent.Factory.newInstance( TicketEventType.COMMENTED, reporter, "looks good" );
        event2.setTicket( ticket );
        event2.setOccurredAt( new Date() );
        event2.setId( 20L );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void legacyModeWithoutCursorReturnsUnpaginatedResponseDataObject() {
        // Legacy path: TicketService.load -> Ticket, projected through TicketValueObject.from(t, true)
        // into a ResponseDataObject<List<TicketEventValueObject>>. The cursor helper must NOT be touched.
        ticket.getEvents().add( event1 );
        ticket.getEvents().add( event2 );

        Object response = webService.getTicketEvents( 100L, null, limit( "20" ) );

        assertThat( response ).isInstanceOf( ResponseDataObject.class );
        @SuppressWarnings("unchecked")
        ResponseDataObject<List<TicketEventValueObject>> r =
                ( ResponseDataObject<List<TicketEventValueObject>> ) response;
        assertThat( r.getData() ).hasSize( 2 );
        // Legacy mode sorts by occurredAt; both timestamps are roughly equal in this fixture so
        // membership (not order) is the contract being verified here.
        assertThat( r.getData() ).extracting( TicketEventValueObject::getId )
                .containsExactlyInAnyOrder( 10L, 20L );

        // Cursor helper must not be touched in legacy mode.
        verify( ticketService, never() ).findEventsByCursor( any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorPaginatedResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 5L }, Cursor.Direction.FORWARD );
        CursorPage<TicketEvent> cp = new CursorPage<>(
                Arrays.asList( event1, event2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( ticketService.findEventsByCursor( eq( ticket ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getTicketEvents( 100L, arg, limit( "20" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketEventValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketEventValueObject> ) response;
        assertThat( page.getData() ).hasSize( 2 );
        assertThat( page.getData().get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( page.getData().get( 1 ).getId() ).isEqualTo( 20L );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the
        // way through to TicketService.findEventsByCursor — verify the decoded Cursor arrives
        // equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<TicketEvent> cp = new CursorPage<>(
                Collections.singletonList( event2 ), null, 5, null, "prev", null );
        when( ticketService.findEventsByCursor( eq( ticket ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getTicketEvents( 100L, CursorArg.valueOf( c.encode() ), limit( "5" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( ticketService ).findEventsByCursor( ticket, c, 5 );
    }

    @Test
    public void cursorModePreservesTicketScope() {
        // The path-derived ticket (loaded by id 100) is forwarded unchanged to the service;
        // the WebService never substitutes or drops it.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<TicketEvent> cp = new CursorPage<>(
                Collections.singletonList( event1 ), null, 10, null, null, null );
        when( ticketService.findEventsByCursor( eq( ticket ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getTicketEvents( 100L, CursorArg.valueOf( c.encode() ), limit( "10" ) );

        verify( ticketService ).findEventsByCursor( ticket, c, 10 );
    }

    @Test
    public void cursorModeEmptyPageProducesEmptyResponseWithNoCursors() {
        // An empty CursorPage round-trips through the CursorPaginatedResponseDataObject
        // wrapper without synthesizing cursor tokens.
        Cursor c = new Cursor( "+id", new Object[] { 999999L }, Cursor.Direction.FORWARD );
        CursorPage<TicketEvent> cp = new CursorPage<>(
                Collections.emptyList(), null, 20, /* nextCursor */ null, /* prevCursor */ null, null );
        when( ticketService.findEventsByCursor( any(), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        Object response = webService.getTicketEvents( 100L, CursorArg.valueOf( c.encode() ), limit( "20" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketEventValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketEventValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isNull();
    }

    @Test
    public void cursorModePreservesAscendingIdOrderFromService() {
        // Cursor mode is forced to ascending id by the DAO; the WebService surfaces whatever
        // order the service returns. Verify the page data preserves the service's order.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<TicketEvent> cp = new CursorPage<>(
                Arrays.asList( event1, event2 ), null, 10, null, null, null );
        when( ticketService.findEventsByCursor( any(), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        Object response = webService.getTicketEvents( 100L, CursorArg.valueOf( c.encode() ), limit( "10" ) );

        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketEventValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketEventValueObject> ) response;
        List<TicketEventValueObject> data = page.getData();
        assertThat( data.get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( data.get( 1 ).getId() ).isEqualTo( 20L );
    }
}
