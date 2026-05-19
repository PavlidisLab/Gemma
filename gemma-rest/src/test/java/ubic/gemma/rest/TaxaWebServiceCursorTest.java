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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.GeneArgService;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.SortArg;
import ubic.gemma.rest.util.args.TaxonArg;
import ubic.gemma.rest.util.args.TaxonArgService;

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
 * {@link TaxaWebService#getTaxonDatasets(TaxonArg, FilterArg, OffsetArg, LimitArg, SortArg, CursorArg)} as
 * step 1d of {@code CURSOR_PAGINATION_STEP1_PLAN.md}, following the same pattern as
 * {@link PlatformsWebServiceCursorTest} (step 1c) and {@link GeneWebServiceCursorTest} (step 1b).
 * Pure Mockito — the goal is to verify the WebService routes cursor vs offset modes to the right
 * helper and emits the right response wrapper, not to retest the DAO (covered by
 * {@code ExpressionExperimentDaoCursorTest}).
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class TaxaWebServiceCursorTest {

    @Mock
    private TaxonService taxonService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private TaxonArgService taxonArgService;
    @Mock
    private DatasetArgService datasetArgService;
    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
    private GeneArgService geneArgService;
    @Mock
    @SuppressWarnings("unused")
    private GeneService geneService;

    @InjectMocks
    private TaxaWebService webService;

    private Taxon taxon;
    private ExpressionExperimentValueObject ee1;
    private ExpressionExperimentValueObject ee2;

    @Before
    public void setUp() {
        taxon = new Taxon();
        taxon.setId( 42L );
        ee1 = new ExpressionExperimentValueObject( 100L );
        ee2 = new ExpressionExperimentValueObject( 200L );
        when( taxonArgService.getEntity( any( TaxonArg.class ) ) ).thenReturn( taxon );
        // datasetArgService.getFilters(FilterArg) returns an empty Filters so the .and(taxon.id=...)
        // composition produces a single-clause Filters without needing a real parse.
        when( datasetArgService.getFilters( any( FilterArg.class ) ) ).thenReturn( Filters.empty() );
        // The taxon.id = ? sub-filter is built via expressionExperimentService.getFilter — stub it
        // to return a representative Filter; equality of Filters objects is not asserted, only
        // that the cursor mode forwards a non-null Filters to the arg-service.
        when( expressionExperimentService.getFilter( eq( "taxon.id" ), eq( Long.class ), eq( Filter.Operator.eq ), eq( taxon.getId() ) ) )
                .thenReturn( Filter.by( "ee", "taxon.id", Long.class, Filter.Operator.eq, taxon.getId() ) );
    }

    private TaxonArg<?> taxonArg() {
        return TaxonArg.valueOf( "42" );
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
    public void offsetModeRoutesToLegacyHelperAndReturnsFilteredPaginatedResponse() {
        Slice<ExpressionExperimentValueObject> slice = new Slice<>(
                Arrays.asList( ee1, ee2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                0, 20, 2L );
        when( datasetArgService.getSort( any( SortArg.class ) ) ).thenReturn(
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
        when( expressionExperimentService.loadValueObjects( any(), any(), anyInt(), anyInt() ) ).thenReturn( slice );

        Object response = webService.getTaxonDatasets(
                taxonArg(), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), null );

        assertThat( response ).isInstanceOf( FilteredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ee1, ee2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

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
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getTaxonDatasets(
                taxonArg(), filter( "" ), offset( "0" ), limit( "20" ), sort( "+id" ), arg );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> page =
                ( FilteredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ee1, ee2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // offset-mode legacy helper must not be touched
        verify( expressionExperimentService, never() ).loadValueObjects( any(), any(), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to DatasetArgService.getDatasetsByCursor — verify the decoded Cursor value
        // arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee2 ), null, 5, null, "prev", null );
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getTaxonDatasets(
                taxonArg(), filter( "" ), offset( "0" ), limit( "5" ), sort( "+id" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        verify( datasetArgService ).getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeSkipsUserSortArg() {
        // Cursor mode currently restricts to id-asc (DAO-enforced). The user's ?sort= must not be
        // resolved via DatasetArgService.getSort(SortArg) when a cursor is present — otherwise
        // a user passing ?sort=+name with a cursor would surface a useless intermediate Sort.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<ExpressionExperimentValueObject> cp = new CursorPage<>(
                List.of( ee1 ), null, 10, null, null, null );
        when( datasetArgService.getDatasetsByCursor( any( Filters.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getTaxonDatasets( taxonArg(), filter( "" ), offset( "0" ), limit( "10" ),
                sort( "+name" ) /* would-be problematic sort */, CursorArg.valueOf( c.encode() ) );

        verify( datasetArgService, never() ).getSort( any( SortArg.class ) );
        verify( expressionExperimentService, never() ).loadValueObjects( any(), any(), anyInt(), anyInt() );
    }
}
