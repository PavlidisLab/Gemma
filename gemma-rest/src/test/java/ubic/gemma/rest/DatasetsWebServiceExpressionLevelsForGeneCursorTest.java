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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.DatasetsWebService.QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.QueriedAndFilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.GeneArg;
import ubic.gemma.rest.util.args.GeneArgService;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.SortArg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link DatasetsWebService#getDatasetsExpressionLevelsForGene(GeneArg, ubic.gemma.rest.util.args.QueryArg, FilterArg, OffsetArg, LimitArg, Boolean, ubic.gemma.rest.util.args.ExpLevelConsolidationArg, CursorArg)}
 * and its taxon-scoped sibling, as step 1v of {@code CURSOR_PAGINATION_STEP1_PLAN.md}.
 * <p>
 * Pure Mockito - the WebService picks cursor vs offset mode and emits the right wrapper.
 * The cursor windowing is in-memory over the +id-sorted datasetIds list returned by
 * {@code ExpressionExperimentService.loadIdsWithCache}, so this test exercises the slice
 * predicate (id > lastSeenId for forward, id < lastSeenId for backward) and the
 * limit+1 over-read used to detect hasMore.
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class DatasetsWebServiceExpressionLevelsForGeneCursorTest {

    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Mock
    private DatasetArgService datasetArgService;
    @Mock
    private GeneArgService geneArgService;

    @InjectMocks
    private DatasetsWebService webService;

    private Gene gene;
    private ExperimentExpressionLevelsValueObject vo1;
    private ExperimentExpressionLevelsValueObject vo2;
    private ExperimentExpressionLevelsValueObject vo3;

    @Before
    public void setUp() {
        gene = Gene.Factory.newInstance();
        gene.setId( 42L );
        // Stub VOs so we have something to assert on. We don't care about their internal
        // shape - they pass through the WebService verbatim.
        vo1 = mock( ExperimentExpressionLevelsValueObject.class );
        vo2 = mock( ExperimentExpressionLevelsValueObject.class );
        vo3 = mock( ExperimentExpressionLevelsValueObject.class );

        // Shared filter wiring: getFilters is the inferred-terms-collecting variant; return
        // an empty Filters so we don't have to parse a real expression. Inferred terms left
        // empty (filled per-test where it matters).
        when( datasetArgService.getFilters( any( FilterArg.class ), isNull(), any( Collection.class ) ) )
                .thenReturn( Filters.empty() );
        when( datasetArgService.getSort( any( SortArg.class ) ) )
                .thenReturn( Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
        when( geneArgService.getEntity( any() ) ).thenReturn( gene );
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

    private GeneArg<?> geneArg( String s ) {
        return GeneArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsQueriedFilteredInferredPaginatedResponse() {
        // datasetIds is +id sorted; offset variant slices it then materializes VOs.
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>( Arrays.asList( 10L, 20L, 30L ) ) );
        when( processedExpressionDataVectorService.getExpressionLevelsByIds(
                eq( Arrays.asList( 10L, 20L, 30L ) ), any(), anyBoolean(), any() ) )
                .thenReturn( Arrays.asList( vo1, vo2, vo3 ) );

        Object response = webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), /* queryArg */ null, filter( "" ), offset( "0" ), limit( "20" ),
                false, null, /* cursorArg */ null );

        assertThat( response ).isInstanceOf( QueriedAndFilteredAndInferredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> page =
                ( QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> ) response;
        assertThat( page.getData() ).containsExactly( vo1, vo2, vo3 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 3L );

        // cursor-mode VO materialization (limited window) must not be invoked twice
        verify( processedExpressionDataVectorService )
                .getExpressionLevelsByIds( any(), any(), anyBoolean(), any() );
    }

    @Test
    public void cursorModeFirstPageReturnsAscendingIdsAndEmitsNextCursorWhenMore() {
        // Five datasets in +id order. limit=2 -> the first page is [10, 20] with a nextCursor
        // pointing past 20. The DAO over-read (limit+1 = 3) decides hasMore=true.
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>( Arrays.asList( 10L, 20L, 30L, 40L, 50L ) ) );
        // Materialization is called with the windowed ids (size = limit on first page).
        when( processedExpressionDataVectorService.getExpressionLevelsByIds(
                eq( Arrays.asList( 10L, 20L ) ), any(), anyBoolean(), any() ) )
                .thenReturn( Arrays.asList( vo1, vo2 ) );

        Object response = webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "2" ),
                false, null, /* cursorArg */ null );

        // No cursor on this call - still legacy mode. Switch to cursor mode below.
        assertThat( response ).isInstanceOf( QueriedAndFilteredAndInferredAndPaginatedResponseDataObject.class );

        // Now repeat with cursor=null wire -> nope, cursor must be non-null to enter cursor mode.
        // First-page cursor mode: pass a "+datasetId" cursor with no key (we model "no cursor" by
        // omitting it). The endpoint treats a non-null cursorArg as cursor mode.
        Cursor first = new Cursor( "+datasetId", new Object[] { 0L }, Cursor.Direction.FORWARD );
        Object cursorResp = webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "2" ),
                false, null, CursorArg.valueOf( first.encode() ) );

        assertThat( cursorResp ).isInstanceOf( QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject.class );
        assertThat( cursorResp ).isInstanceOf( QueriedAndFilteredAndCursorPaginatedResponseDataObject.class );
        assertThat( cursorResp ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        assertThat( cursorResp ).isInstanceOf( CursorPaginatedResponseDataObject.class );

        @SuppressWarnings("unchecked")
        QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> page =
                ( QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> ) cursorResp;
        assertThat( page.getData() ).containsExactly( vo1, vo2 );
        // forward over-read found id=30 -> nextCursor non-null, decoded sortSpec=+datasetId, key=20
        assertThat( page.getNextCursor() ).isNotNull();
        Cursor next = Cursor.decode( page.getNextCursor() );
        assertThat( next.getSortSpec() ).isEqualTo( "+datasetId" );
        assertThat( next.getKeyTuple() ).containsExactly( 20L );
        assertThat( next.getDirection() ).isEqualTo( Cursor.Direction.FORWARD );
        // first-page from inside cursor mode (cursor was supplied) -> prevCursor populated too
        assertThat( page.getPrevCursor() ).isNotNull();
        // cursor mode never counts by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 2 );
    }

    @Test
    public void cursorModeLastPageOmitsNextCursor() {
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>( Arrays.asList( 10L, 20L, 30L ) ) );
        when( processedExpressionDataVectorService.getExpressionLevelsByIds(
                eq( Collections.singletonList( 30L ) ), any(), anyBoolean(), any() ) )
                .thenReturn( Collections.singletonList( vo3 ) );

        // Cursor at id=20 -> walk forward, only id=30 remains (limit+1 over-read fits in 1 result),
        // so hasMore=false and nextCursor=null.
        Cursor c = new Cursor( "+datasetId", new Object[] { 20L }, Cursor.Direction.FORWARD );
        Object response = webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "5" ),
                false, null, CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> page =
                ( QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> ) response;
        assertThat( page.getData() ).containsExactly( vo3 );
        assertThat( page.getNextCursor() ).isNull();
        // prevCursor still emitted because we navigated forward with an inbound cursor
        assertThat( page.getPrevCursor() ).isNotNull();
        Cursor prev = Cursor.decode( page.getPrevCursor() );
        assertThat( prev.getKeyTuple() ).containsExactly( 30L );
        assertThat( prev.getDirection() ).isEqualTo( Cursor.Direction.BACKWARD );
    }

    @Test
    public void cursorModeEmptyDatasetIdsRoundTripsEmptyPage() {
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>() );

        Cursor c = new Cursor( "+datasetId", new Object[] { 0L }, Cursor.Direction.FORWARD );
        Object response = webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "20" ),
                false, null, CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> page =
                ( QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isNull();
        // No id materialization on empty window
        verify( processedExpressionDataVectorService, never() )
                .getExpressionLevelsByIds( any(), any(), anyBoolean(), any() );
    }

    @Test
    public void cursorModeBackwardWalkReversesOrderAndYieldsAscendingPage() {
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>( Arrays.asList( 10L, 20L, 30L, 40L, 50L ) ) );
        // BACKWARD from id=40: ids < 40 in descending scan are [30, 20, 10]; with limit=2 we take
        // [30, 20] then reverse for ascending client-visible output -> [20, 30].
        when( processedExpressionDataVectorService.getExpressionLevelsByIds(
                eq( Arrays.asList( 20L, 30L ) ), any(), anyBoolean(), any() ) )
                .thenReturn( Arrays.asList( vo2, vo3 ) );

        Cursor c = new Cursor( "+datasetId", new Object[] { 40L }, Cursor.Direction.BACKWARD );
        Object response = webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "2" ),
                false, null, CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> page =
                ( QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> ) response;
        assertThat( page.getData() ).containsExactly( vo2, vo3 );
        // BACKWARD walk: nextCursor is always populated (we came from there), key = last visible id
        assertThat( page.getNextCursor() ).isNotNull();
        Cursor next = Cursor.decode( page.getNextCursor() );
        assertThat( next.getKeyTuple() ).containsExactly( 30L );
        // prevCursor populated whenever a cursor was supplied
        assertThat( page.getPrevCursor() ).isNotNull();
        Cursor prev = Cursor.decode( page.getPrevCursor() );
        assertThat( prev.getKeyTuple() ).containsExactly( 20L );
    }

    @Test
    public void cursorModeInferredTermsEchoedInResponse() {
        // Same inferred-terms collection trick as DatasetsWebServiceBlacklistedCursorTest:
        // the WebService passes an inferredTerms HashSet through getFilters(...) which the
        // arg-service populates as a side-effect; the response wrapper must echo those.
        when( datasetArgService.getFilters( any( FilterArg.class ), isNull(), any( Collection.class ) ) )
                .thenAnswer( inv -> {
                    @SuppressWarnings("unchecked")
                    Collection<OntologyTerm> inferred = ( Collection<OntologyTerm> ) inv.getArgument( 2 );
                    if ( inferred != null ) {
                        inferred.add( new ubic.gemma.core.ontology.basecode.simple.OntologyTermSimple(
                                "http://example.org/OT_1", "test-term" ) );
                    }
                    return Filters.empty();
                } );
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>( Collections.singletonList( 10L ) ) );
        when( processedExpressionDataVectorService.getExpressionLevelsByIds(
                eq( Collections.singletonList( 10L ) ), any(), anyBoolean(), any() ) )
                .thenReturn( Collections.singletonList( vo1 ) );

        Cursor c = new Cursor( "+datasetId", new Object[] { 0L }, Cursor.Direction.FORWARD );
        Object response = webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "20" ),
                false, null, CursorArg.valueOf( c.encode() ) );

        @SuppressWarnings("unchecked")
        QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> page =
                ( QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> ) response;
        assertThat( page.getInferredTerms() ).hasSize( 1 );
        assertThat( page.getInferredTerms().get( 0 ).getValueUri() ).isEqualTo( "http://example.org/OT_1" );
        // filter echo also present (empty Filters -> empty/null filter string)
        assertThat( page.getQuery() ).isNull();
    }

    @Test
    public void cursorModeForwardsGeneEntityResolution() {
        // The cursor branch resolves the gene via GeneArgService.getEntity exactly like the offset
        // variant (i.e. no double-resolution and no skipping). Verify the resolved Gene is the
        // entity passed to the materializer.
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>( Collections.singletonList( 10L ) ) );
        ArgumentCaptor<Collection<Gene>> captor = ArgumentCaptor.forClass( Collection.class );
        when( processedExpressionDataVectorService.getExpressionLevelsByIds(
                eq( Collections.singletonList( 10L ) ), captor.capture(), anyBoolean(), any() ) )
                .thenReturn( Collections.singletonList( vo1 ) );

        Cursor c = new Cursor( "+datasetId", new Object[] { 0L }, Cursor.Direction.FORWARD );
        webService.getDatasetsExpressionLevelsForGene(
                geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "20" ),
                false, null, CursorArg.valueOf( c.encode() ) );

        assertThat( captor.getValue() ).containsExactly( gene );
    }

    @Test
    public void cursorModeRejectsMismatchedSortSpec() {
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenReturn( new ArrayList<>( Arrays.asList( 10L, 20L ) ) );
        // Cursor declares "+id" instead of the expected "+datasetId" - rejected as malformed.
        Cursor c = new Cursor( "+id", new Object[] { 0L }, Cursor.Direction.FORWARD );
        try {
            webService.getDatasetsExpressionLevelsForGene(
                    geneArg( "1" ), null, filter( "" ), offset( "0" ), limit( "20" ),
                    false, null, CursorArg.valueOf( c.encode() ) );
            assert false : "expected MalformedArgException";
        } catch ( ubic.gemma.rest.util.MalformedArgException e ) {
            assertThat( e.getMessage() ).contains( "sort spec" );
        }
        verify( processedExpressionDataVectorService, never() )
                .getExpressionLevelsByIds( any(), any(), anyBoolean(), any() );
    }
}
