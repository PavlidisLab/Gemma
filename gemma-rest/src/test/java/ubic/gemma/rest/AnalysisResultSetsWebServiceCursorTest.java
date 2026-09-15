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
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatabaseEntryArgService;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.ExpressionAnalysisResultSetArgService;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.SortArg;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link AnalysisResultSetsWebService#getResultSets} for step 1i of
 * {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to verify the
 * WebService routes cursor vs offset modes to the right helper and emits the right
 * response wrapper, not to retest the DAO.
 */
@ExtendWith(MockitoExtension.class)
public class AnalysisResultSetsWebServiceCursorTest {

    @Mock
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    @SuppressWarnings("unused")
    private ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService;
    @Mock
    private ExpressionAnalysisResultSetArgService expressionAnalysisResultSetArgService;
    @Mock
    @SuppressWarnings("unused")
    private DatasetArgService datasetArgService;
    @Mock
    @SuppressWarnings("unused")
    private DatabaseEntryArgService databaseEntryArgService;

    @InjectMocks
    private AnalysisResultSetsWebService webService;

    private DifferentialExpressionAnalysisResultSetValueObject vo1;
    private DifferentialExpressionAnalysisResultSetValueObject vo2;

    @BeforeEach
    public void setUp() {
        vo1 = makeVo( 100L );
        vo2 = makeVo( 200L );
        // The filter arg goes through ExpressionAnalysisResultSetArgService.getFilters(FilterArg);
        // return an empty Filters so we don't need a real parse.
        when( expressionAnalysisResultSetArgService.getFilters( any( FilterArg.class ) ) ).thenReturn( Filters.empty() );
    }

    private static DifferentialExpressionAnalysisResultSetValueObject makeVo( Long id ) {
        DifferentialExpressionAnalysisResultSetValueObject vo = new DifferentialExpressionAnalysisResultSetValueObject();
        vo.setId( id );
        return vo;
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsFilteredPaginatedResponse() {
        Slice<DifferentialExpressionAnalysisResultSetValueObject> slice = new Slice<>(
                Arrays.asList( vo1, vo2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                0, 20, 2L );
        when( expressionAnalysisResultSetArgService.getSort( any( SortArg.class ) ) ).thenReturn(
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
        when( expressionAnalysisResultSetService.findByBioAssaySetInAndDatabaseEntryInLimit(
                isNull(), isNull(), any( Filters.class ), eq( 0 ), eq( 20 ), any( Sort.class ) ) ).thenReturn( slice );

        Object response = webService.getResultSets(
                null, null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ),
                null );

        assertThat( response ).isInstanceOf( FilteredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> page =
                ( FilteredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> ) response;
        assertThat( page.getData() ).containsExactly( vo1, vo2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        // cursor-mode arg-service helper must not be touched in offset mode
        verify( expressionAnalysisResultSetArgService, never() )
                .getResultSetsByCursor( any(), any(), any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 99L }, Cursor.Direction.FORWARD );
        CursorPage<DifferentialExpressionAnalysisResultSetValueObject> cp = new CursorPage<>(
                Arrays.asList( vo1, vo2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( expressionAnalysisResultSetArgService.getResultSetsByCursor(
                isNull(), isNull(), any( Filters.class ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getResultSets(
                null, null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ),
                arg );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndCursorPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> page =
                ( FilteredAndCursorPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> ) response;
        assertThat( page.getData() ).containsExactly( vo1, vo2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // offset-mode legacy helper must not be touched
        verify( expressionAnalysisResultSetService, never() ).findByBioAssaySetInAndDatabaseEntryInLimit(
                any(), any(), any(), anyInt(), anyInt(), any() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the
        // way through to ExpressionAnalysisResultSetArgService.getResultSetsByCursor — verify
        // the decoded Cursor value arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<DifferentialExpressionAnalysisResultSetValueObject> cp = new CursorPage<>(
                List.of( vo2 ), null, 5, null, "prev", null );
        when( expressionAnalysisResultSetArgService.getResultSetsByCursor(
                isNull(), isNull(), any( Filters.class ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getResultSets(
                null, null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "5" ),
                SortArg.valueOf( "+id" ),
                CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        verify( expressionAnalysisResultSetArgService ).getResultSetsByCursor(
                isNull(), isNull(), any( Filters.class ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeSkipsUserSortArg() {
        // Cursor mode currently restricts to id-asc (DAO-enforced). The user's ?sort= must not
        // be resolved via ExpressionAnalysisResultSetArgService.getSort(SortArg) when a cursor
        // is present (parallels steps 1c/1f/1g/1h).
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<DifferentialExpressionAnalysisResultSetValueObject> cp = new CursorPage<>(
                List.of( vo1 ), null, 10, null, null, null );
        when( expressionAnalysisResultSetArgService.getResultSetsByCursor(
                isNull(), isNull(), any( Filters.class ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getResultSets(
                null, null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+name" ) /* would-be problematic sort */,
                CursorArg.valueOf( c.encode() ) );

        verify( expressionAnalysisResultSetArgService, never() ).getSort( any( SortArg.class ) );
        verify( expressionAnalysisResultSetService, never() ).findByBioAssaySetInAndDatabaseEntryInLimit(
                any(), any(), any(), anyInt(), anyInt(), any() );
    }
}
