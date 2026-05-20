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
import ubic.gemma.rest.util.args.CompositeSequenceArgService;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.PlatformArgService;
import ubic.gemma.rest.util.args.PlatformArrayArg;
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
 * {@link PlatformsWebService#getPlatformsByIds(PlatformArrayArg, FilterArg, OffsetArg, LimitArg, SortArg, CursorArg)}
 * — the {@code GET /platforms/{platform}} variant (step 1x). Pure Mockito — the goal is to verify
 * the WebService routes cursor vs offset modes to the right helper and emits the right response
 * wrapper, and that the path-derived id-set predicate is preserved in cursor mode (not silently
 * dropped). DAO behaviour is covered by {@code ArrayDesignDaoCursorTest}.
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class PlatformsWebServiceByIdsCursorTest {

    @Mock
    @SuppressWarnings("unused")
    private GeneService geneService;
    @Mock
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

    private ArrayDesignValueObject ad1;
    private ArrayDesignValueObject ad2;

    @BeforeEach
    public void setUp() {
        ad1 = new ArrayDesignValueObject( 100L );
        ad2 = new ArrayDesignValueObject( 200L );
        // Both getFilters(FilterArg) and getFilters(AbstractEntityArrayArg) should return empties
        // so we don't need a real parse / a real entity lookup.
        when( arrayDesignArgService.getFilters( any( FilterArg.class ) ) ).thenReturn( Filters.empty() );
        when( arrayDesignArgService.getFilters( any( PlatformArrayArg.class ) ) ).thenReturn( Filters.empty() );
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

    private PlatformArrayArg platformsArg() {
        return PlatformArrayArg.valueOf( "100,200" );
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

        Object response = webService.getPlatformsByIds(
                platformsArg(), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), null );

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
        Object response = webService.getPlatformsByIds(
                platformsArg(), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), arg );

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
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ArrayDesignValueObject> cp = new CursorPage<>(
                List.of( ad2 ), null, 5, null, "prev", null );
        when( arrayDesignArgService.getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getPlatformsByIds(
                platformsArg(), filter( "" ), offset( "0" ), limit( "5" ), sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        verify( arrayDesignArgService ).getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeSkipsUserSortArg() {
        // Cursor mode currently restricts to id-asc (DAO-enforced). The user's ?sort= must not be
        // resolved via PlatformArgService.getSort(SortArg) when a cursor is present.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ArrayDesignValueObject> cp = new CursorPage<>(
                List.of( ad1 ), null, 10, null, null, null );
        when( arrayDesignArgService.getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getPlatformsByIds( platformsArg(), filter( "" ), offset( "0" ), limit( "10" ),
                sort( "+name" ) /* would-be problematic sort */, CursorArg.valueOf( c.encode() ) );

        verify( arrayDesignArgService, never() ).getSort( any( SortArg.class ) );
        verify( arrayDesignService, never() ).loadValueObjects( any(), any(), anyInt(), anyInt() );
    }

    @Test
    public void cursorModePreservesPathIdSetPredicate() {
        // The path-derived id-set predicate must be composed into the Filters passed to the DAO.
        // Use a non-empty Filters from the platforms-arg getFilters() and verify it survives.
        Filters idSetFilters = Filters.empty().and( "ad", "id", Long.class,
                ubic.gemma.persistence.util.Filter.Operator.in,
                Arrays.asList( 100L, 200L ) );
        when( arrayDesignArgService.getFilters( any( PlatformArrayArg.class ) ) ).thenReturn( idSetFilters );

        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ArrayDesignValueObject> cp = new CursorPage<>(
                List.of( ad1 ), null, 10, null, null, null );
        when( arrayDesignArgService.getPlatformsByCursor( any( Filters.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getPlatformsByIds( platformsArg(), filter( "" ), offset( "0" ), limit( "10" ),
                sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        // Verify the id-set predicate Filters actually reached the cursor helper (i.e. wasn't dropped).
        org.mockito.ArgumentCaptor<Filters> captor = org.mockito.ArgumentCaptor.forClass( Filters.class );
        verify( arrayDesignArgService ).getPlatformsByCursor( captor.capture(), eq( c ), eq( 10 ) );
        Filters passed = captor.getValue();
        assertThat( passed ).isNotNull();
        assertThat( passed.isEmpty() ).isFalse();
    }
}
