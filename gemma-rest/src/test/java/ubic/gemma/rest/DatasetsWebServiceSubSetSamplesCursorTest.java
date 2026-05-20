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
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.LimitArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link DatasetsWebService#getDatasetSubSetSamples(DatasetArg, Long, CursorArg, LimitArg)}
 * as step 1u of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to
 * verify the WebService routes cursor vs legacy modes to the right helper and emits the
 * right response wrapper, not to retest the DAO (covered by the broader DAO-cursor
 * suite).
 * <p>
 * Mirrors {@code DatasetsWebServiceSamplesCursorTest} (step 1k): the legacy mode is NOT
 * offset-paginated for this endpoint — it returns an unpaginated
 * {@link ResponseDataObject}{@code <List<BioAssayValueObject>>}. The cursor branch is
 * therefore strictly additive (an opt-in via {@code ?cursor=}). Unlike step 1k, this
 * endpoint has no {@code quantitationType} parameter, so there is no
 * mutual-exclusion-with-cursor case to test.
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class DatasetsWebServiceSubSetSamplesCursorTest {

    @Mock
    private DatasetArgService datasetArgService;

    @InjectMocks
    private DatasetsWebService webService;

    private BioAssayValueObject ba1;
    private BioAssayValueObject ba2;
    private DatasetArg<?> datasetArg;
    private static final Long SUBSET_ID = 7L;

    @Before
    public void setUp() {
        ba1 = new BioAssayValueObject();
        ba1.setId( 10L );
        ba2 = new BioAssayValueObject();
        ba2.setId( 20L );
        datasetArg = DatasetArg.valueOf( "GSE1234" );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void legacyModeWithoutCursorReturnsUnpaginatedResponseDataObject() {
        List<BioAssayValueObject> all = Arrays.asList( ba1, ba2 );
        when( datasetArgService.getSubSetSamples( any( DatasetArg.class ), eq( SUBSET_ID ) ) ).thenReturn( all );

        Object response = webService.getDatasetSubSetSamples( datasetArg, SUBSET_ID, null, limit( "20" ) );

        assertThat( response ).isInstanceOf( ResponseDataObject.class );
        @SuppressWarnings("unchecked")
        ResponseDataObject<List<BioAssayValueObject>> r = ( ResponseDataObject<List<BioAssayValueObject>> ) response;
        assertThat( r.getData() ).containsExactly( ba1, ba2 );

        // Cursor helper must not be touched in legacy mode.
        verify( datasetArgService, never() ).getSubSetSamplesByCursor( any( DatasetArg.class ), anyLong(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorPaginatedResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 99L }, Cursor.Direction.FORWARD );
        CursorPage<BioAssayValueObject> cp = new CursorPage<>(
                Arrays.asList( ba1, ba2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( datasetArgService.getSubSetSamplesByCursor( any( DatasetArg.class ), eq( SUBSET_ID ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getDatasetSubSetSamples( datasetArg, SUBSET_ID, arg, limit( "20" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<BioAssayValueObject> page =
                ( CursorPaginatedResponseDataObject<BioAssayValueObject> ) response;
        assertThat( page.getData() ).containsExactly( ba1, ba2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // Legacy helper must not be touched in cursor mode.
        verify( datasetArgService, never() ).getSubSetSamples( any( DatasetArg.class ), anyLong() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to DatasetArgService.getSubSetSamplesByCursor — verify the decoded Cursor value
        // arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<BioAssayValueObject> cp = new CursorPage<>(
                Collections.singletonList( ba2 ), null, 5, null, "prev", null );
        when( datasetArgService.getSubSetSamplesByCursor( any( DatasetArg.class ), eq( SUBSET_ID ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getDatasetSubSetSamples( datasetArg, SUBSET_ID, CursorArg.valueOf( c.encode() ), limit( "5" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( datasetArgService ).getSubSetSamplesByCursor( any( DatasetArg.class ), eq( SUBSET_ID ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeWithEmptyPageRoundTripsThroughWrapper() {
        // An empty result page must still produce a CursorPaginatedResponseDataObject; verify
        // empty data + null cursor tokens survive the wrapper construction (mirrors the
        // step-1k empty-page contract).
        Cursor c = new Cursor( "+id", new Object[] { 9999L }, Cursor.Direction.FORWARD );
        CursorPage<BioAssayValueObject> cp = new CursorPage<>(
                Collections.emptyList(),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20, null, null, null );
        when( datasetArgService.getSubSetSamplesByCursor( any( DatasetArg.class ), eq( SUBSET_ID ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        Object response = webService.getDatasetSubSetSamples( datasetArg, SUBSET_ID, CursorArg.valueOf( c.encode() ), limit( "20" ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<BioAssayValueObject> page =
                ( CursorPaginatedResponseDataObject<BioAssayValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isNull();
    }
}
