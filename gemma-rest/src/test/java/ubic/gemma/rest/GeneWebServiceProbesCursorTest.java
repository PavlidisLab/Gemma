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
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.GeneArg;
import ubic.gemma.rest.util.args.GeneArgService;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import java.util.Arrays;
import java.util.Collections;
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
 * {@link GeneWebService#getGeneProbes(GeneArg, OffsetArg, LimitArg, CursorArg)} as step 1m
 * of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to verify the
 * WebService routes cursor vs offset modes to the right helper and emits the right
 * response wrapper, not to retest the DAO (covered by the broader DAO-cursor suite).
 * <p>
 * Mirrors the step 1l {@link PlatformsWebServiceElementGenesCursorTest} structure, which
 * is the symmetric endpoint (probe&rarr;genes; this one is gene&rarr;probes). Differences:
 * <ul>
 *   <li>The offset variant already returns a {@code Slice<CompositeSequenceValueObject>}
 *       (the service does the VO mapping), so the WebService doesn't need a
 *       {@code Slice.map(loadValueObject)} call — both modes simply forward.
 *   <li>The response wrappers are the bare {@link PaginatedResponseDataObject} and
 *       {@link CursorPaginatedResponseDataObject} (no path-derived filter is echoed back —
 *       matches the existing offset wrapper).
 *   <li>The cursor variant delegates to {@link GeneArgService#getGeneProbesByCursor},
 *       which itself resolves the gene against the request (mirroring the offset call
 *       {@code geneArgService.getGeneProbes(geneArg, ...)}).
 * </ul>
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class GeneWebServiceProbesCursorTest {

    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired on the WebService
    private GeneService geneService;

    @Mock
    private GeneArgService geneArgService;

    @Mock
    @SuppressWarnings("unused")
    private TableMaintenanceUtil tableMaintenanceUtil;

    @InjectMocks
    private GeneWebService webService;

    private CompositeSequenceValueObject probe1;
    private CompositeSequenceValueObject probe2;
    private GeneArg<?> geneArg;

    @BeforeEach
    public void setUp() {
        probe1 = new CompositeSequenceValueObject();
        probe1.setId( 10L );
        probe1.setName( "probe-10" );
        probe2 = new CompositeSequenceValueObject();
        probe2.setId( 20L );
        probe2.setName( "probe-20" );
        geneArg = GeneArg.valueOf( "BRCA1" );
    }

    private OffsetArg offset( String s ) {
        return OffsetArg.valueOf( s );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsPaginatedResponse() {
        // The legacy path: GeneArgService.getGeneProbes → paginate(Slice).
        Slice<CompositeSequenceValueObject> slice = new Slice<>(
                Arrays.asList( probe1, probe2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                0, 20, 2L );
        when( geneArgService.getGeneProbes( any( GeneArg.class ), eq( 0 ), eq( 20 ) ) ).thenReturn( slice );

        Object response = webService.getGeneProbes( geneArg, offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( PaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<CompositeSequenceValueObject> page =
                ( PaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
        assertThat( page.getData() ).containsExactly( probe1, probe2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        // Cursor helper must not be touched in legacy mode.
        verify( geneArgService, never() ).getGeneProbesByCursor( any( GeneArg.class ), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 9L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                Arrays.asList( probe1, probe2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( geneArgService.getGeneProbesByCursor( any( GeneArg.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getGeneProbes( geneArg, offset( "0" ), limit( "20" ), arg );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<CompositeSequenceValueObject> page =
                ( CursorPaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
        assertThat( page.getData() ).containsExactly( probe1, probe2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // Legacy getGeneProbes must not be touched in cursor mode.
        verify( geneArgService, never() ).getGeneProbes( any( GeneArg.class ), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to GeneArgService.getGeneProbesByCursor — verify the decoded Cursor value arrives
        // equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                Collections.singletonList( probe2 ), null, 5, null, "prev", null );
        when( geneArgService.getGeneProbesByCursor( any( GeneArg.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getGeneProbes( geneArg, offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( geneArgService ).getGeneProbesByCursor( any( GeneArg.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeForwardsGeneArgUnchanged() {
        // The path arg is passed straight through: geneArg is forwarded unchanged. Verify the
        // WebService doesn't drop or substitute the path arg.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                Collections.singletonList( probe1 ), null, 10, null, null, null );
        when( geneArgService.getGeneProbesByCursor( eq( geneArg ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getGeneProbes( geneArg, offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( geneArgService ).getGeneProbesByCursor( eq( geneArg ), eq( c ), eq( 10 ) );
    }

    @Test
    public void cursorModeEmptyPageProducesEmptyResponseWithNoNextCursor() {
        // An empty CursorPage (no matches under the cursor predicate) round-trips through the
        // CursorPaginatedResponseDataObject wrapper without synthesizing cursor tokens or borrowing
        // the input cursor.
        Cursor c = new Cursor( "+id", new Object[] { 999999L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                Collections.emptyList(), null, 20, /* nextCursor */ null, /* prevCursor */ null, null );
        when( geneArgService.getGeneProbesByCursor( any( GeneArg.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        Object response = webService.getGeneProbes( geneArg, offset( "0" ), limit( "20" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<CompositeSequenceValueObject> page =
                ( CursorPaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isNull();
    }

    @Test
    public void cursorModePreservesAscendingIdOrderFromDao() {
        // Cursor mode is forced to ascending cs.id by the DAO; the WebService is responsible for
        // surfacing whatever order the DAO returns. Verify the page data preserves the DAO's order.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                Arrays.asList( probe1, probe2 ), null, 10, null, null, null );
        when( geneArgService.getGeneProbesByCursor( any( GeneArg.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        Object response = webService.getGeneProbes( geneArg, offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<CompositeSequenceValueObject> page =
                ( CursorPaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
        List<CompositeSequenceValueObject> data = page.getData();
        assertThat( data ).containsExactly( probe1, probe2 );
    }
}
