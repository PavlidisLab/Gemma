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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.CompositeSequenceArgService;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.PlatformArgService;
import ubic.gemma.rest.util.args.SortArg;

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
 * {@link PlatformsWebService#getPlatforms(FilterArg, OffsetArg, LimitArg, SortArg, CursorArg)} as
 * proof-of-concept for step 1c of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the
 * goal is to verify the WebService routes cursor vs offset modes to the right helper and emits
 * the right response wrapper, not to retest the DAO (covered by {@code ExpressionExperimentDaoCursorTest}).
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformsWebServiceCursorTest {

    @Mock
    private GeneService geneService;
    @Mock
    private ArrayDesignService arrayDesignService;
    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
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

    private ArrayDesignValueObject ad1;
    private ArrayDesignValueObject ad2;

    @Before
    public void setUp() {
        ad1 = new ArrayDesignValueObject( 100L );
        ad2 = new ArrayDesignValueObject( 200L );
        // The filter arg's getFilters(...) goes through PlatformArgService.getFilters(FilterArg);
        // return an empty Filters so we don't need a real parse.
        when( arrayDesignArgService.getFilters( any( FilterArg.class ) ) ).thenReturn( Filters.empty() );
    }

    private FilterArg<ArrayDesign> filter( String s ) {
        return FilterArg.valueOf( s );
    }

    private OffsetArg offset( String s ) {
        return OffsetArg.valueOf( s );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    private SortArg<ArrayDesign> sort( String s ) {
        return SortArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsFilteredPaginatedResponse() {
        Slice<ArrayDesignValueObject> slice = new Slice<>(
                Arrays.asList( ad1, ad2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                0, 20, 2L );
        when( arrayDesignArgService.getSort( any( SortArg.class ) ) ).thenReturn(
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
        when( arrayDesignService.loadValueObjects( any(), any(), anyInt(), anyInt() ) ).thenReturn( slice );

        Object response = webService.getPlatforms(
                filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), null );

        assertThat( response ).isInstanceOf( FilteredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> page =
                ( FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ad1, ad2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        verify( arrayDesignArgService, never() ).getPlatformsByCursor( any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 99L }, Cursor.Direction.FORWARD );
        CursorPage<ArrayDesignValueObject> cp = new CursorPage<>(
                Arrays.asList( ad1, ad2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( arrayDesignArgService.getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getPlatforms(
                filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), arg );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndCursorPaginatedResponseDataObject<ArrayDesignValueObject> page =
                ( FilteredAndCursorPaginatedResponseDataObject<ArrayDesignValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ad1, ad2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // offset-mode legacy helper must not be touched
        verify( arrayDesignService, never() ).loadValueObjects( any(), any(), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to PlatformArgService.getPlatformsByCursor — verify the decoded Cursor value
        // arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ArrayDesignValueObject> cp = new CursorPage<>(
                List.of( ad2 ), null, 5, null, "prev", null );
        when( arrayDesignArgService.getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getPlatforms(
                filter( "" ), offset( "0" ), limit( "5" ), sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        verify( arrayDesignArgService ).getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeSkipsUserSortArg() {
        // Cursor mode currently restricts to id-asc (DAO-enforced). The user's ?sort= must not be
        // resolved via PlatformArgService.getSort(SortArg) when a cursor is present — otherwise
        // a user passing ?sort=+name with a cursor would surface a useless intermediate Sort.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ArrayDesignValueObject> cp = new CursorPage<>(
                List.of( ad1 ), null, 10, null, null, null );
        when( arrayDesignArgService.getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getPlatforms( filter( "" ), offset( "0" ), limit( "10" ),
                sort( "+name" ) /* would-be problematic sort */, CursorArg.valueOf( c.encode() ) );

        verify( arrayDesignArgService, never() ).getSort( any( SortArg.class ) );
        verify( arrayDesignService, never() ).loadValueObjects( any(), any(), anyInt(), anyInt() );
    }
}
