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
import org.springframework.security.access.AccessDecisionManager;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CompositeSequenceArgService;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.PlatformArg;
import ubic.gemma.rest.util.args.PlatformArgService;

import java.util.Arrays;
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
 * {@link PlatformsWebService#getPlatformElements(PlatformArg, OffsetArg, LimitArg, CursorArg)} as
 * step 1e of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to verify the
 * WebService routes cursor vs offset modes to the right helper and emits the right response
 * wrapper, not to retest the DAO (covered by {@code ExpressionExperimentDaoCursorTest} +
 * the broader DAO-cursor suite).
 *
 * Mirrors {@link PlatformsWebServiceCursorTest} (step 1c) and
 * {@link TaxaWebServiceCursorTest} (step 1d). Differences:
 * - The path-derived {@code arrayDesign.id = ?} constraint is composed inside
 *   {@code PlatformArgService.getElementsByCursor} (not in the WebService), so the
 *   WebService only forwards the {@link PlatformArg}, cursor, and limit.
 * - The offset-mode response wrapper is {@link PaginatedResponseDataObject} (no echoed
 *   {@code filter} field — the endpoint has no {@code ?filter=} query param), so the
 *   cursor-mode response uses the plain {@link CursorPaginatedResponseDataObject}.
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformsWebServiceElementsCursorTest {

    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
    private GeneService geneService;
    @Mock
    @SuppressWarnings("unused")
    private ArrayDesignService arrayDesignService;
    @Mock
    @SuppressWarnings("unused")
    private CompositeSequenceService compositeSequenceService;
    @Mock
    @SuppressWarnings("unused")
    private ArrayDesignAnnotationService annotationFileService;
    @Mock
    private PlatformArgService arrayDesignArgService;
    @Mock
    @SuppressWarnings("unused")
    private CompositeSequenceArgService probeArgService;
    @Mock
    @SuppressWarnings("unused")
    private AccessDecisionManager accessDecisionManager;
    @Mock
    @SuppressWarnings("unused")
    private TicketsWebService ticketsWebService;

    @InjectMocks
    private PlatformsWebService webService;

    private CompositeSequenceValueObject cs1;
    private CompositeSequenceValueObject cs2;
    private PlatformArg<?> platformArg;

    @Before
    public void setUp() {
        cs1 = new CompositeSequenceValueObject( 10L );
        cs2 = new CompositeSequenceValueObject( 20L );
        platformArg = PlatformArg.valueOf( "42" );
    }

    private OffsetArg offset( String s ) {
        return OffsetArg.valueOf( s );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsPaginatedResponse() {
        Slice<CompositeSequenceValueObject> slice = new Slice<>(
                Arrays.asList( cs1, cs2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                0, 20, 2L );
        when( arrayDesignArgService.getElements( any( PlatformArg.class ), anyInt(), anyInt() ) ).thenReturn( slice );

        Object response = webService.getPlatformElements( platformArg, offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( PaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<CompositeSequenceValueObject> page =
                ( PaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
        assertThat( page.getData() ).containsExactly( cs1, cs2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        verify( arrayDesignArgService, never() ).getElementsByCursor( any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 99L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                Arrays.asList( cs1, cs2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( arrayDesignArgService.getElementsByCursor( any( PlatformArg.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getPlatformElements( platformArg, offset( "0" ), limit( "20" ), arg );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<CompositeSequenceValueObject> page =
                ( CursorPaginatedResponseDataObject<CompositeSequenceValueObject> ) response;
        assertThat( page.getData() ).containsExactly( cs1, cs2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // offset-mode legacy helper must not be touched
        verify( arrayDesignArgService, never() ).getElements( any( PlatformArg.class ), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to PlatformArgService.getElementsByCursor — verify the decoded Cursor value
        // arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                List.of( cs2 ), null, 5, null, "prev", null );
        when( arrayDesignArgService.getElementsByCursor( any( PlatformArg.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getPlatformElements( platformArg, offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( arrayDesignArgService ).getElementsByCursor( any( PlatformArg.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeForwardsPlatformArg() {
        // The PlatformArg is passed straight through to the arg-service; the path-derived
        // arrayDesign.id = ? filter is composed inside PlatformArgService.getElementsByCursor,
        // not in the WebService. Verify the WebService doesn't drop or substitute the arg.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<CompositeSequenceValueObject> cp = new CursorPage<>(
                List.of( cs1 ), null, 10, null, null, null );
        when( arrayDesignArgService.getElementsByCursor( eq( platformArg ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getPlatformElements( platformArg, offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( arrayDesignArgService ).getElementsByCursor( eq( platformArg ), eq( c ), eq( 10 ) );
    }
}
