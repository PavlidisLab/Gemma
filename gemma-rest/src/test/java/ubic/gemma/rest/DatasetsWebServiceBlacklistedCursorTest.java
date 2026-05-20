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
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.SortArg;

import java.util.Arrays;
import java.util.Collection;
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
 * {@link DatasetsWebService#getBlacklistedDatasets(FilterArg, SortArg, OffsetArg, LimitArg, CursorArg)}
 * as step 1t of {@code CURSOR_PAGINATION_STEP1_PLAN.md} (the EE-targeted twin of step 1h's
 * {@code /platforms/blacklisted}). Pure Mockito &mdash; the goal is to verify the WebService
 * routes cursor vs offset modes to the right helper and emits the right response wrapper, not
 * to retest the DAO (blacklist filter composition is covered by the DAO impl).
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class DatasetsWebServiceBlacklistedCursorTest {

    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private DatasetArgService datasetArgService;

    @InjectMocks
    private DatasetsWebService webService;

    private ExpressionExperimentValueObject ee1;
    private ExpressionExperimentValueObject ee2;

    @Before
    public void setUp() {
        ee1 = new ExpressionExperimentValueObject( 100L );
        ee2 = new ExpressionExperimentValueObject( 200L );
        // The filter arg's getFilters(...) goes through DatasetArgService.getFilters(FilterArg, null, inferredTerms);
        // return an empty Filters so we don't need a real parse. We don't populate inferredTerms.
        when( datasetArgService.getFilters( any( FilterArg.class ), any(), any( Collection.class ) ) )
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

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsFilteredInferredPaginatedResponse() {
        Sort idAsc = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        Slice<ExpressionExperimentValueObject> slice = new Slice<>(
                Arrays.asList( ee1, ee2 ), idAsc, 0, 20, 2L );
        when( datasetArgService.getSort( any( SortArg.class ) ) ).thenReturn( idAsc );
        when( expressionExperimentService.loadBlacklistedValueObjects( any(), any(), anyInt(), anyInt() ) )
                .thenReturn( slice );

        Object response = webService.getBlacklistedDatasets(
                filter( "" ), sort( "+id" ), offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( FilteredAndInferredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ee1, ee2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        // cursor helper must not be touched in offset mode
        verify( datasetArgService, never() ).getBlacklistedDatasetsByCursor( any(), any(), anyInt() );
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
        when( datasetArgService.getBlacklistedDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) )
                .thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getBlacklistedDatasets(
                filter( "" ), sort( "+id" ), offset( "0" ), limit( "20" ), arg );

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
        verify( expressionExperimentService, never() ).loadBlacklistedValueObjects( any(), any(), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (base64-decode) through to
        // DatasetArgService.getBlacklistedDatasetsByCursor &mdash; verify the decoded Cursor arrives
        // equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee2 ), null, 5, null, "prev", null );
        when( datasetArgService.getBlacklistedDatasetsByCursor( any( Filters.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getBlacklistedDatasets(
                filter( "" ), sort( "+id" ), offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndInferredAndCursorPaginatedResponseDataObject.class );
        verify( datasetArgService ).getBlacklistedDatasetsByCursor( any( Filters.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeSkipsUserSortArg() {
        // Cursor mode currently restricts to id-asc (DAO-enforced). The user's ?sort= must not be
        // resolved via DatasetArgService.getSort(SortArg) when a cursor is present.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee1 ), null, 10, null, null, null );
        when( datasetArgService.getBlacklistedDatasetsByCursor( any( Filters.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getBlacklistedDatasets( filter( "" ), sort( "+name" ) /* would-be problematic sort */,
                offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( datasetArgService, never() ).getSort( any( SortArg.class ) );
        verify( expressionExperimentService, never() ).loadBlacklistedValueObjects( any(), any(), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeEmptyPageRoundTripsWithoutSynthesizedTokens() {
        // The DAO short-circuits to an empty page when there are no blacklisted EEs (or the
        // composed filters select nothing). The WebService must propagate that emptiness
        // unchanged &mdash; null nextCursor/prevCursor, totalElements echoed (0 here), empty data.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> empty = new CursorPage<>(
                java.util.Collections.emptyList(),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ null,
                /* prevCursor */ null,
                /* totalElements */ 0L );
        when( datasetArgService.getBlacklistedDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( empty );

        Object response = webService.getBlacklistedDatasets(
                filter( "" ), sort( "+id" ), offset( "0" ), limit( "20" ), CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isEqualTo( 0L );
    }

    @Test
    public void cursorModeForwardsFiltersBuiltByArgService() {
        // The Filters built by datasetArgService.getFilters(...) must be the exact instance
        // forwarded to the cursor helper, on top of which the DAO composes the blacklist
        // predicate. Use a non-empty Filters to make the forward observable.
        Filters f = Filters.empty();
        when( datasetArgService.getFilters( any( FilterArg.class ), any(), any( Collection.class ) ) )
                .thenReturn( f );

        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee1 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20, null, null, null );
        when( datasetArgService.getBlacklistedDatasetsByCursor( eq( f ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        webService.getBlacklistedDatasets(
                filter( "" ), sort( "+id" ), offset( "0" ), limit( "20" ), CursorArg.valueOf( c.encode() ) );

        verify( datasetArgService ).getBlacklistedDatasetsByCursor( eq( f ), eq( c ), eq( 20 ) );
    }

    @Test
    public void cursorModeInferredTermsEchoedInResponse() {
        // The inferredTerms HashSet is collected inside the endpoint, populated as a side-effect
        // of datasetArgService.getFilters(filterArg, null, inferredTerms). When the arg-service
        // populates the collection, those terms must be echoed in the response wrapper. Use a
        // stub that fills the inferredTerms argument with one OntologyTerm.
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
        when( datasetArgService.getBlacklistedDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        Object response = webService.getBlacklistedDatasets(
                filter( "" ), sort( "+id" ), offset( "0" ), limit( "20" ), CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getInferredTerms() ).hasSize( 1 );
        assertThat( page.getInferredTerms().get( 0 ).getValueUri() ).isEqualTo( "http://example.org/OT_1" );
    }
}
