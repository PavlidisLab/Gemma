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
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.LimitArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link DatasetsWebService#getDatasetTickets(DatasetArg, CursorArg, LimitArg)}
 * as step 1p of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito &mdash;
 * the goal is to verify the WebService routes cursor vs legacy modes to the
 * right helper and emits the right response wrapper, not to retest the DAO
 * (the keyset HQL is covered separately).
 * <p>
 * Mirrors the path-derived-constraint pattern of step 1k
 * ({@code DatasetsWebServiceSamplesCursorTest}): the legacy mode is NOT
 * offset-paginated for this endpoint &mdash; it returns an unpaginated
 * {@link ResponseDataObject}{@code <List<TicketValueObject>>}. The cursor
 * branch is therefore strictly additive (an opt-in via {@code ?cursor=}).
 * The dataset-id scope (path-derived {@code targetType =
 * EXPRESSION_EXPERIMENT, targetId = ee.id}) is preserved in both modes; the
 * open-state restriction (OPEN/IN_PROGRESS) lives in the DAO and is
 * cross-checked at the service edge.
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class DatasetsWebServiceTicketsCursorTest {

    @Mock
    private DatasetArgService datasetArgService;
    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketsWebService ticketsWebService;

    @InjectMocks
    private DatasetsWebService webService;

    private DatasetArg<?> datasetArg;
    private ExpressionExperiment ee;
    private Ticket ticket1;
    private Ticket ticket2;

    @BeforeEach
    public void setUp() throws Exception {
        datasetArg = DatasetArg.valueOf( "GSE1234" );

        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 99L );
        ee.setShortName( "GSE1234" );

        when( datasetArgService.getEntity( any( DatasetArg.class ) ) ).thenReturn( ee );

        User reporter = new User();
        reporter.setId( 42L );
        reporter.setName( "alice" );

        ticket1 = Ticket.Factory.newInstance( TicketType.GENERIC, "Ticket A", reporter );
        ticket1.setId( 10L );
        ticket1.setState( TicketState.OPEN );
        ticket1.setPriority( TicketPriority.HIGH );
        ticket1.setCreatedAt( new Date() );
        ticket1.setUpdatedAt( new Date() );
        TicketTarget tt1 = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 99L );
        tt1.setTicket( ticket1 );
        ticket1.setTargets( new HashSet<>( Collections.singletonList( tt1 ) ) );
        ticket1.setEvents( new HashSet<>() );

        ticket2 = Ticket.Factory.newInstance( TicketType.GENERIC, "Ticket B", reporter );
        ticket2.setId( 20L );
        ticket2.setState( TicketState.IN_PROGRESS );
        ticket2.setPriority( TicketPriority.LOW );
        ticket2.setCreatedAt( new Date() );
        ticket2.setUpdatedAt( new Date() );
        TicketTarget tt2 = TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, 99L );
        tt2.setTicket( ticket2 );
        ticket2.setTargets( new HashSet<>( Collections.singletonList( tt2 ) ) );
        ticket2.setEvents( new HashSet<>() );

        // Wire the TicketsWebService delegate that DatasetsWebService calls into.
        java.lang.reflect.Field f = DatasetsWebService.class.getDeclaredField( "ticketsWebService" );
        f.setAccessible( true );
        f.set( webService, ticketsWebService );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void legacyModeWithoutCursorReturnsUnpaginatedResponseDataObject() {
        // Legacy path: TicketService.findOpenForTarget -> List<TicketValueObject>
        // wrapped in a ResponseDataObject.
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, 99L ) )
                .thenReturn( Arrays.asList( ticket1, ticket2 ) );

        Object response = webService.getDatasetTickets( datasetArg, null, limit( "20" ) );

        assertThat( response ).isInstanceOf( ResponseDataObject.class );
        @SuppressWarnings("unchecked")
        ResponseDataObject<List<TicketValueObject>> r =
                ( ResponseDataObject<List<TicketValueObject>> ) response;
        assertThat( r.getData() ).hasSize( 2 );
        assertThat( r.getData().get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( r.getData().get( 1 ).getId() ).isEqualTo( 20L );

        // Cursor helper must not be touched in legacy mode.
        verify( ticketService, never() ).findOpenForTargetByCursor(
                any( TicketTargetType.class ), anyLong(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorPaginatedResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 5L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Arrays.asList( ticket1, ticket2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( ticketService.findOpenForTargetByCursor(
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ), eq( 99L ), eq( c ), eq( 20 ) ) )
                .thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getDatasetTickets( datasetArg, arg, limit( "20" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketValueObject> ) response;
        assertThat( page.getData() ).hasSize( 2 );
        assertThat( page.getData().get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( page.getData().get( 1 ).getId() ).isEqualTo( 20L );
        // list view omits events (TicketValueObject.from(t) with default detail mode)
        assertThat( page.getData().get( 0 ).getEvents() ).isEmpty();
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // Legacy helper must not be touched in cursor mode.
        verify( ticketService, never() ).findOpenForTarget(
                any( TicketTargetType.class ), anyLong() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToService() {
        // The cursor flows from CursorArg.valueOf(...) (base64-decodes the token) through
        // TicketsWebService.openTicketsForExpressionExperimentByCursor down to
        // TicketService.findOpenForTargetByCursor — verify the decoded Cursor arrives
        // equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Collections.singletonList( ticket2 ), null, 5, null, "prev", null );
        when( ticketService.findOpenForTargetByCursor(
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ), eq( 99L ), eq( c ), eq( 5 ) ) )
                .thenReturn( cp );

        Object response = webService.getDatasetTickets( datasetArg, CursorArg.valueOf( c.encode() ), limit( "5" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( ticketService ).findOpenForTargetByCursor(
                TicketTargetType.EXPRESSION_EXPERIMENT, 99L, c, 5 );
    }

    @Test
    public void cursorModePreservesDatasetScope() {
        // The dataset-id path constraint (ExpressionExperiment id 99) is forwarded as the
        // TicketTargetType + targetId pair; the WebService never substitutes or drops it.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Collections.singletonList( ticket1 ), null, 10, null, null, null );
        when( ticketService.findOpenForTargetByCursor(
                eq( TicketTargetType.EXPRESSION_EXPERIMENT ), eq( 99L ), eq( c ), eq( 10 ) ) )
                .thenReturn( cp );

        webService.getDatasetTickets( datasetArg, CursorArg.valueOf( c.encode() ), limit( "10" ) );

        verify( ticketService ).findOpenForTargetByCursor(
                TicketTargetType.EXPRESSION_EXPERIMENT, 99L, c, 10 );
    }

    @Test
    public void cursorModeEmptyPageProducesEmptyResponseWithNoCursors() {
        // An empty CursorPage round-trips through the CursorPaginatedResponseDataObject
        // wrapper without synthesizing cursor tokens.
        Cursor c = new Cursor( "+id", new Object[] { 999999L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Collections.emptyList(), null, 20, /* nextCursor */ null, /* prevCursor */ null, null );
        when( ticketService.findOpenForTargetByCursor(
                any( TicketTargetType.class ), anyLong(), eq( c ), eq( 20 ) ) )
                .thenReturn( cp );

        Object response = webService.getDatasetTickets( datasetArg, CursorArg.valueOf( c.encode() ), limit( "20" ) );

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
        // Cursor mode is forced to ascending id by the DAO; the WebService surfaces whatever
        // order the service returns. Verify the page data preserves the service's order.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Ticket> cp = new CursorPage<>(
                Arrays.asList( ticket1, ticket2 ), null, 10, null, null, null );
        when( ticketService.findOpenForTargetByCursor(
                any( TicketTargetType.class ), anyLong(), eq( c ), eq( 10 ) ) )
                .thenReturn( cp );

        Object response = webService.getDatasetTickets( datasetArg, CursorArg.valueOf( c.encode() ), limit( "10" ) );

        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<TicketValueObject> page =
                ( CursorPaginatedResponseDataObject<TicketValueObject> ) response;
        List<TicketValueObject> data = page.getData();
        assertThat( data.get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( data.get( 1 ).getId() ).isEqualTo( 20L );
    }
}
