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
import org.springframework.security.access.AccessDecisionManager;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
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
 * {@link PlatformsWebService#getPlatformDatasets(PlatformArg, OffsetArg, LimitArg, CursorArg)} as
 * step 1f of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to verify the
 * WebService routes cursor vs offset modes to the right helper and emits the right response
 * wrapper, not to retest the DAO (covered by {@code ExpressionExperimentDaoCursorTest} +
 * the broader DAO-cursor suite).
 *
 * Mirrors {@link PlatformsWebServiceElementsCursorTest} (step 1e). Differences:
 * - The listing returns {@link ExpressionExperimentValueObject experiments} (datasets) for the
 *   platform, not design elements; the path-derived constraint is
 *   {@code bioAssays.arrayDesignUsed.id = ?} (composed inside
 *   {@code PlatformArgService.getExperimentsByCursor}, not in the WebService).
 * - The offset-mode response wrapper is {@link PaginatedResponseDataObject} (no echoed
 *   {@code filter} field — the endpoint has no {@code ?filter=} query param), so the
 *   cursor-mode response uses the plain {@link CursorPaginatedResponseDataObject}.
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class PlatformsWebServiceDatasetsCursorTest {

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

    private ExpressionExperimentValueObject ee1;
    private ExpressionExperimentValueObject ee2;
    private PlatformArg<?> platformArg;

    @BeforeEach
    public void setUp() {
        ee1 = new ExpressionExperimentValueObject( 100L );
        ee2 = new ExpressionExperimentValueObject( 200L );
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
        Slice<ExpressionExperimentValueObject> slice = new Slice<>(
                Arrays.asList( ee1, ee2 ),
                Sort.by( null, "bioAssays.arrayDesignUsed.id", null, Sort.NullMode.LAST, "bioAssays.arrayDesignUsed.id" ),
                0, 20, 2L );
        when( arrayDesignArgService.getExperiments( any( PlatformArg.class ), anyInt(), anyInt() ) ).thenReturn( slice );

        Object response = webService.getPlatformDatasets( platformArg, offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( PaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( PaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ee1, ee2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        verify( arrayDesignArgService, never() ).getExperimentsByCursor( any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 99L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                Arrays.asList( ee1, ee2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( arrayDesignArgService.getExperimentsByCursor( any( PlatformArg.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getPlatformDatasets( platformArg, offset( "0" ), limit( "20" ), arg );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( CursorPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ee1, ee2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // offset-mode legacy helper must not be touched
        verify( arrayDesignArgService, never() ).getExperiments( any( PlatformArg.class ), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to PlatformArgService.getExperimentsByCursor — verify the decoded Cursor value
        // arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee2 ), null, 5, null, "prev", null );
        when( arrayDesignArgService.getExperimentsByCursor( any( PlatformArg.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getPlatformDatasets( platformArg, offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( arrayDesignArgService ).getExperimentsByCursor( any( PlatformArg.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeForwardsPlatformArg() {
        // The PlatformArg is passed straight through to the arg-service; the path-derived
        // bioAssays.arrayDesignUsed.id = ? filter is composed inside
        // PlatformArgService.getExperimentsByCursor, not in the WebService. Verify the WebService
        // doesn't drop or substitute the arg.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee1 ), null, 10, null, null, null );
        when( arrayDesignArgService.getExperimentsByCursor( eq( platformArg ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getPlatformDatasets( platformArg, offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( arrayDesignArgService ).getExperimentsByCursor( eq( platformArg ), eq( c ), eq( 10 ) );
    }
}
