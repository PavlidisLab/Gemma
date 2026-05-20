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
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.LimitArg;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
 * {@link DatasetsWebService#getDatasetAuditEvents(DatasetArg, CursorArg, LimitArg)}
 * as step 1q of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito &mdash; the
 * goal is to verify the WebService routes cursor vs legacy modes to the right
 * helper and emits the right response wrapper, not to retest the DAO (the
 * keyset HQL is covered separately).
 * <p>
 * Mirrors the path-derived-constraint pattern of step 1k
 * ({@code DatasetsWebServiceSamplesCursorTest}) and step 1p
 * ({@code DatasetsWebServiceTicketsCursorTest}): the legacy mode is NOT
 * offset-paginated for this endpoint &mdash; it returns an unpaginated
 * {@link ResponseDataObject}{@code <List<AuditEventValueObject>>}. The cursor
 * branch is therefore strictly additive (an opt-in via {@code ?cursor=}). The
 * dataset (auditable) scope is preserved in both modes; cursor mode forces
 * id-asc ordering inside the DAO.
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class DatasetsWebServiceAuditEventsCursorTest {

    @Mock
    private DatasetArgService datasetArgService;
    @Mock
    private AuditEventService auditEventService;

    @InjectMocks
    private DatasetsWebService webService;

    private DatasetArg<?> datasetArg;
    private ExpressionExperiment ee;
    private AuditEvent event1;
    private AuditEvent event2;

    @BeforeEach
    public void setUp() throws Exception {
        datasetArg = DatasetArg.valueOf( "GSE1234" );

        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 99L );
        ee.setShortName( "GSE1234" );
        AuditTrail trail = AuditTrail.Factory.newInstance();
        trail.setId( 7L );
        ee.setAuditTrail( trail );

        when( datasetArgService.getEntity( any( DatasetArg.class ) ) ).thenReturn( ee );

        User performer = new User();
        performer.setId( 42L );
        performer.setName( "alice" );
        performer.setUserName( "alice" );

        event1 = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "note A", "detail A", performer, null );
        setId( event1, 10L );

        event2 = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "note B", "detail B", performer, null );
        setId( event2, 20L );

        // Wire the field-injected AuditEventService for the @InjectMocks DatasetsWebService.
        // (Mockito's @InjectMocks handles this for matching field names, but the WebService
        // has many fields and an explicit wire keeps the test resilient to renames.)
        Field f = DatasetsWebService.class.getDeclaredField( "auditEventService" );
        f.setAccessible( true );
        f.set( webService, auditEventService );
    }

    private static void setId( AuditEvent ae, Long id ) throws Exception {
        // AuditEvent extends AbstractIdentifiable which has a protected setId; walk the
        // hierarchy via reflection so the test does not depend on visibility changes.
        Class<?> c = ae.getClass();
        while ( c != null ) {
            try {
                java.lang.reflect.Method m = c.getDeclaredMethod( "setId", Long.class );
                m.setAccessible( true );
                m.invoke( ae, id );
                return;
            } catch ( NoSuchMethodException ignored ) {
                c = c.getSuperclass();
            }
        }
        throw new IllegalStateException( "no setId(Long) found on AuditEvent hierarchy" );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void legacyModeWithoutCursorReturnsUnpaginatedResponseDataObject() {
        // Legacy path: AuditEventService.getEvents -> List<AuditEvent>, mapped to VOs and
        // wrapped in a ResponseDataObject.
        when( auditEventService.getEvents( ee ) )
                .thenReturn( Arrays.asList( event1, event2 ) );

        Object response = webService.getDatasetAuditEvents( datasetArg, null, limit( "20" ) );

        assertThat( response ).isInstanceOf( ResponseDataObject.class );
        @SuppressWarnings("unchecked")
        ResponseDataObject<List<AuditEventValueObject>> r =
                ( ResponseDataObject<List<AuditEventValueObject>> ) response;
        assertThat( r.getData() ).hasSize( 2 );
        assertThat( r.getData().get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( r.getData().get( 1 ).getId() ).isEqualTo( 20L );

        // Cursor helper must not be touched in legacy mode.
        verify( auditEventService, never() ).getEventsByCursor( any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorPaginatedResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 5L }, Cursor.Direction.FORWARD );
        CursorPage<AuditEvent> cp = new CursorPage<>(
                Arrays.asList( event1, event2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( auditEventService.getEventsByCursor( eq( ee ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getDatasetAuditEvents( datasetArg, arg, limit( "20" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<AuditEventValueObject> page =
                ( CursorPaginatedResponseDataObject<AuditEventValueObject> ) response;
        assertThat( page.getData() ).hasSize( 2 );
        assertThat( page.getData().get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( page.getData().get( 1 ).getId() ).isEqualTo( 20L );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // Legacy helper must not be touched in cursor mode.
        verify( auditEventService, never() ).getEvents( any() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToService() {
        // The cursor flows from CursorArg.valueOf(...) (base64-decodes the token) into the
        // AuditEventService cursor helper — verify the decoded Cursor arrives equal to
        // what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<AuditEvent> cp = new CursorPage<>(
                Collections.singletonList( event2 ), null, 5, null, "prev", null );
        when( auditEventService.getEventsByCursor( eq( ee ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getDatasetAuditEvents( datasetArg, CursorArg.valueOf( c.encode() ), limit( "5" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( auditEventService ).getEventsByCursor( ee, c, 5 );
    }

    @Test
    public void cursorModePreservesDatasetScope() {
        // The path-derived auditable (ExpressionExperiment with id 99) is forwarded
        // unchanged to the service; the WebService never substitutes or drops it.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<AuditEvent> cp = new CursorPage<>(
                Collections.singletonList( event1 ), null, 10, null, null, null );
        when( auditEventService.getEventsByCursor( eq( ee ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getDatasetAuditEvents( datasetArg, CursorArg.valueOf( c.encode() ), limit( "10" ) );

        verify( auditEventService ).getEventsByCursor( ee, c, 10 );
    }

    @Test
    public void cursorModeEmptyPageProducesEmptyResponseWithNoCursors() {
        // An empty CursorPage round-trips through the CursorPaginatedResponseDataObject
        // wrapper without synthesizing cursor tokens.
        Cursor c = new Cursor( "+id", new Object[] { 999999L }, Cursor.Direction.FORWARD );
        CursorPage<AuditEvent> cp = new CursorPage<>(
                Collections.emptyList(), null, 20, /* nextCursor */ null, /* prevCursor */ null, null );
        when( auditEventService.getEventsByCursor( any(), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        Object response = webService.getDatasetAuditEvents( datasetArg, CursorArg.valueOf( c.encode() ), limit( "20" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<AuditEventValueObject> page =
                ( CursorPaginatedResponseDataObject<AuditEventValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isNull();
    }

    @Test
    public void cursorModePreservesAscendingIdOrderFromService() {
        // Cursor mode is forced to ascending id by the DAO; the WebService surfaces
        // whatever order the service returns. Verify the page data preserves the
        // service's order.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<AuditEvent> cp = new CursorPage<>(
                Arrays.asList( event1, event2 ), null, 10, null, null, null );
        when( auditEventService.getEventsByCursor( any(), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        Object response = webService.getDatasetAuditEvents( datasetArg, CursorArg.valueOf( c.encode() ), limit( "10" ) );

        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<AuditEventValueObject> page =
                ( CursorPaginatedResponseDataObject<AuditEventValueObject> ) response;
        List<AuditEventValueObject> data = page.getData();
        assertThat( data.get( 0 ).getId() ).isEqualTo( 10L );
        assertThat( data.get( 1 ).getId() ).isEqualTo( 20L );
    }
}
