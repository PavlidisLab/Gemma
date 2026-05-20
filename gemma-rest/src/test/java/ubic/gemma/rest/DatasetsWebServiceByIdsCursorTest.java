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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.DatasetsWebService.FilteredAndInferredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.DatasetsWebService.FilteredAndInferredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.args.AbstractEntityArrayArg;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.DatasetArrayArg;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.SortArg;

import java.util.Arrays;
import java.util.Collection;
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
 * {@link DatasetsWebService#getDatasetsByIds(DatasetArrayArg, FilterArg, OffsetArg, LimitArg, SortArg, CursorArg)}
 * as step 1w of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito &mdash; the goal is to
 * verify the WebService routes cursor vs offset modes to the right helper and emits the right
 * response wrapper, not to retest the DAO. Closest analog: step 1t
 * ({@link DatasetsWebServiceBlacklistedCursorTest}) which is the same
 * {@code FilteredAndInferred} cursor wrapper on the same EE service surface, and step 1d
 * ({@code TaxaWebServiceCursorTest}) which is the same {@link DatasetArgService#getDatasetsByCursor}
 * helper.
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class DatasetsWebServiceByIdsCursorTest {

    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private DatasetArgService datasetArgService;

    @InjectMocks
    private DatasetsWebService webService;

    private ExpressionExperimentValueObject ee1;
    private ExpressionExperimentValueObject ee2;

    @BeforeEach
    public void setUp() {
        ee1 = new ExpressionExperimentValueObject( 100L );
        ee2 = new ExpressionExperimentValueObject( 200L );
        // The filter arg's getFilters(...) goes through DatasetArgService.getFilters(FilterArg, null, inferredTerms);
        // return an empty Filters so we don't need a real parse. We don't populate inferredTerms.
        when( datasetArgService.getFilters( any( FilterArg.class ), any(), any( Collection.class ) ) )
                .thenReturn( Filters.empty() );
        // The {dataset} path arg flows through DatasetArgService.getFilters(AbstractEntityArrayArg);
        // return an empty Filters so the .and(...) composition produces a non-null Filters instance
        // identical to the inferred-terms branch.
        when( datasetArgService.getFilters( any( AbstractEntityArrayArg.class ) ) )
                .thenReturn( Filters.empty() );
    }

    private FilterArg<ExpressionExperiment> filter( String s ) {
        return FilterArg.valueOf( s );
    }

    private OffsetArg offset( String s ) {
        return OffsetArg.valueOf( s );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    private SortArg<ExpressionExperiment> sort( String s ) {
        return SortArg.valueOf( s );
    }

    private DatasetArrayArg datasets( String s ) {
        return DatasetArrayArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsFilteredInferredPaginatedResponse() {
        Sort idAsc = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        Slice<ExpressionExperimentValueObject> slice = new Slice<>(
                Arrays.asList( ee1, ee2 ), idAsc, 0, 20, 2L );
        when( datasetArgService.getSort( any( SortArg.class ) ) ).thenReturn( idAsc );
        when( expressionExperimentService.loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() ) )
                .thenReturn( slice );

        Object response = webService.getDatasetsByIds(
                datasets( "100,200" ), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), null );

        assertThat( response ).isInstanceOf( FilteredAndInferredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ee1, ee2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        // cursor helper must not be touched in offset mode
        verify( datasetArgService, never() ).getDatasetsByCursor( any(), any(), anyInt() );
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
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) )
                .thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getDatasetsByIds(
                datasets( "100,200" ), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), arg );

        // FilteredAndInferred subclass of FilteredAndCursor; both type-checks should hold.
        assertThat( response ).isInstanceOf( FilteredAndInferredAndCursorPaginatedResponseDataObject.class );
        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ee1, ee2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );
        // inferredTerms list is always emitted (empty here because the mocked
        // datasetArgService.getFilters never populates the collection)
        assertThat( page.getInferredTerms() ).isEmpty();

        // offset-mode legacy helper must not be touched
        verify( expressionExperimentService, never() ).loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (base64-decode) through to
        // DatasetArgService.getDatasetsByCursor &mdash; verify the decoded Cursor arrives
        // equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee2 ), null, 5, null, "prev", null );
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getDatasetsByIds(
                datasets( "200" ), filter( "" ), offset( "0" ), limit( "5" ), sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndInferredAndCursorPaginatedResponseDataObject.class );
        verify( datasetArgService ).getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeSkipsUserSortArg() {
        // Cursor mode currently restricts to id-asc (DAO-enforced). The user's ?sort= must not be
        // resolved via DatasetArgService.getSort(SortArg) when a cursor is present.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee1 ), null, 10, null, null, null );
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getDatasetsByIds( datasets( "100" ), filter( "" ), offset( "0" ), limit( "10" ),
                sort( "+name" ) /* would-be problematic sort */, CursorArg.valueOf( c.encode() ) );

        verify( datasetArgService, never() ).getSort( any( SortArg.class ) );
        verify( expressionExperimentService, never() ).loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeEmptyPageRoundTripsWithoutSynthesizedTokens() {
        // An empty page (no datasets match the path arg + filter) must propagate unchanged:
        // null nextCursor/prevCursor, totalElements echoed (0 here), empty data.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> empty = new CursorPage<>(
                java.util.Collections.emptyList(),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ null,
                /* prevCursor */ null,
                /* totalElements */ 0L );
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( empty );

        Object response = webService.getDatasetsByIds(
                datasets( "100" ), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isEqualTo( 0L );
    }

    @Test
    public void cursorModeForwardsComposedFiltersIncludingPathDatasetArg() {
        // The endpoint composes datasetArgService.getFilters(FilterArg, null, inferredTerms)
        // .and(datasetArgService.getFilters(DatasetArrayArg)). Both halves must be observed.
        // Use empty Filters for both so the .and(...) yields a deterministic result we can verify
        // is forwarded to the cursor helper.
        Filters userFilter = Filters.empty();
        Filters pathFilter = Filters.empty();
        when( datasetArgService.getFilters( any( FilterArg.class ), any(), any( Collection.class ) ) )
                .thenReturn( userFilter );
        when( datasetArgService.getFilters( any( AbstractEntityArrayArg.class ) ) )
                .thenReturn( pathFilter );

        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee1 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20, null, null, null );
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        webService.getDatasetsByIds(
                datasets( "100" ), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        // Both Filters builders must have been invoked in cursor mode (the path-derived
        // {dataset} constraint must still apply on top of the user ?filter=).
        verify( datasetArgService ).getFilters( any( FilterArg.class ), any(), any( Collection.class ) );
        verify( datasetArgService ).getFilters( any( AbstractEntityArrayArg.class ) );
        // And the composed Filters reaches the cursor helper.
        verify( datasetArgService ).getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) );
    }

    @Test
    public void cursorModeInferredTermsEchoedInResponse() {
        // The inferredTerms HashSet is collected inside the endpoint, populated as a side-effect
        // of datasetArgService.getFilters(filterArg, null, inferredTerms). When the arg-service
        // populates the collection, those terms must be echoed in the response wrapper.
        when( datasetArgService.getFilters( any( FilterArg.class ), any(), any( Collection.class ) ) )
                .thenAnswer( inv -> {
                    @SuppressWarnings("unchecked")
                    Collection<ubic.gemma.core.ontology.basecode.model.OntologyTerm> inferred =
                            ( Collection<ubic.gemma.core.ontology.basecode.model.OntologyTerm> ) inv.getArgument( 2 );
                    if ( inferred != null ) {
                        inferred.add( new ubic.gemma.core.ontology.basecode.simple.OntologyTermSimple(
                                "http://example.org/OT_1", "test-term" ) );
                    }
                    return Filters.empty();
                } );

        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee1 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20, null, null, null );
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        Object response = webService.getDatasetsByIds(
                datasets( "100" ), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getInferredTerms() ).hasSize( 1 );
        assertThat( page.getInferredTerms().get( 0 ).getValueUri() ).isEqualTo( "http://example.org/OT_1" );
    }
}
