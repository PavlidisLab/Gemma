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

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.ExcludeArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.QuantitationTypeArg;
import ubic.gemma.rest.util.args.QuantitationTypeArgService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link DatasetsWebService#getDatasetSamples(DatasetArg, QuantitationTypeArg, boolean, CursorArg, LimitArg, ExcludeArg, boolean)}
 * as step 1k of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to
 * verify the WebService routes cursor vs legacy modes to the right helper and emits the
 * right response wrapper, not to retest the DAO (covered by the broader DAO-cursor
 * suite).
 * <p>
 * Differences from the previous 1e-1j cursor tests:
 * - The legacy mode is NOT offset-paginated for this endpoint — it returns an
 *   unpaginated {@link ResponseDataObject}{@code <List<BioAssayValueObject>>}. The
 *   cursor branch is therefore strictly additive (an opt-in via {@code ?cursor=}).
 * - The endpoint also accepts {@code quantitationType} and
 *   {@code useProcessedQuantitationType} that select QT-narrowed assay listings; these
 *   intentionally remain in the legacy unpaginated mode (their sort and dimension
 *   restriction are not expressible as an id-only cursor under the step 1b restriction),
 *   so supplying {@code cursor} together with either is a {@code 400 Bad Request}.
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class DatasetsWebServiceSamplesCursorTest {

    @Mock
    private DatasetArgService datasetArgService;
    @Mock
    private QuantitationTypeArgService quantitationTypeArgService;

    @InjectMocks
    private DatasetsWebService webService;

    private BioAssayValueObject ba1;
    private BioAssayValueObject ba2;
    private DatasetArg<?> datasetArg;

    @BeforeEach
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
        when( datasetArgService.getSamples( any( DatasetArg.class ), anyBoolean() ) ).thenReturn( all );

        // No limit: legacy mode is only reachable when the caller asked for no page size at all.
        Object response = webService.getDatasetSamples( datasetArg, null, false, null, null, null, false );

        assertThat( response ).isInstanceOf( ResponseDataObject.class );
        @SuppressWarnings("unchecked")
        ResponseDataObject<List<BioAssayValueObject>> r = ( ResponseDataObject<List<BioAssayValueObject>> ) response;
        assertThat( r.getData() ).containsExactly( ba1, ba2 );

        // Cursor helper must not be touched in legacy mode.
        verify( datasetArgService, never() ).getSamplesByCursor( any( DatasetArg.class ), any(), anyInt(), anyBoolean() );
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
        when( datasetArgService.getSamplesByCursor( any( DatasetArg.class ), eq( c ), eq( 20 ), anyBoolean() ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getDatasetSamples( datasetArg, null, false, arg, limit( "20" ), null, false );

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
        verify( datasetArgService, never() ).getSamples( any( DatasetArg.class ), anyBoolean() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to DatasetArgService.getSamplesByCursor — verify the decoded Cursor value arrives
        // equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<BioAssayValueObject> cp = new CursorPage<>(
                Collections.singletonList( ba2 ), null, 5, null, "prev", null );
        when( datasetArgService.getSamplesByCursor( any( DatasetArg.class ), eq( c ), eq( 5 ), anyBoolean() ) ).thenReturn( cp );

        Object response = webService.getDatasetSamples( datasetArg, null, false, CursorArg.valueOf( c.encode() ), limit( "5" ), null, false );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( datasetArgService ).getSamplesByCursor( any( DatasetArg.class ), eq( c ), eq( 5 ), anyBoolean() );
    }

    @Test
    public void cursorWithQuantitationTypeIsRejectedAs400() {
        // The QT-narrowed sample listings sort by assay name and apply a BioAssayDimension restriction
        // (see DatasetArgService.getSamples(DatasetArg, QuantitationType)). Neither is expressible as
        // an id-only cursor under the step 1b restriction, so refuse the combination instead of
        // silently ignoring one input.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        QuantitationTypeArg<?> qtArg = QuantitationTypeArg.valueOf( "42" );

        assertThatThrownBy( () -> webService.getDatasetSamples( datasetArg, qtArg, false, CursorArg.valueOf( c.encode() ), limit( "20" ), null, false ) )
                .isInstanceOf( BadRequestException.class );

        verify( datasetArgService, never() ).getSamplesByCursor( any( DatasetArg.class ), any(), anyInt(), anyBoolean() );
        verify( datasetArgService, never() ).getSamples( any( DatasetArg.class ), anyBoolean() );
    }

    /**
     * 🛑 The route bound {@code limit} and then never used it outside cursor mode.
     * <p>
     * Measured on production ({@code gemma2}, dataset 7332 = GSE2109, 2158 samples):
     * {@code GET /datasets/7332/samples?limit=20} answered with all 2158 assays — the same body as the
     * no-parameter call, 22,846,518 bytes at the time — and nothing in it said the page size had been
     * dropped. Truncating to 20 instead is no better: the legacy response is an unpaginated
     * {@link ResponseDataObject} with no {@code totalElements} and no {@code nextCursor}, so it has
     * nowhere to declare that 2138 rows were left out. A caller must not be able to receive a different
     * number of rows than it asked for without being told, so the parameter is refused.
     */
    @Test
    public void limitWithoutCursorIsRejectedAs400() {
        assertThatThrownBy( () -> webService.getDatasetSamples( datasetArg, null, false, null, limit( "20" ), null, false ) )
                .isInstanceOf( BadRequestException.class );

        // Neither listing may run: the point is that no body is produced at all, not that a truncated one is.
        verify( datasetArgService, never() ).getSamples( any( DatasetArg.class ), anyBoolean() );
        verify( datasetArgService, never() ).getSamplesByCursor( any( DatasetArg.class ), any(), anyInt(), anyBoolean() );
    }

    /**
     * The QT-narrowed listings are legacy-mode too, so they refuse a limit on the same grounds — the
     * rejection is a property of "not paginating", not of the plain branch.
     */
    @Test
    public void limitWithQuantitationTypeAndNoCursorIsRejectedAs400() {
        QuantitationTypeArg<?> qtArg = QuantitationTypeArg.valueOf( "42" );

        assertThatThrownBy( () -> webService.getDatasetSamples( datasetArg, qtArg, false, null, limit( "20" ), null, false ) )
                .isInstanceOf( BadRequestException.class );
        assertThatThrownBy( () -> webService.getDatasetSamples( datasetArg, null, true, null, limit( "20" ), null, false ) )
                .isInstanceOf( BadRequestException.class );

        verify( datasetArgService, never() ).getSamples( any( DatasetArg.class ), any( QuantitationType.class ), anyBoolean() );
        verify( datasetArgService, never() ).getPreferredQuantitationType( any( DatasetArg.class ) );
    }

    /**
     * Cursor mode still has a page size when the caller sends none — dropping {@code @DefaultValue("20")}
     * from the parameter moved that default into the method, and it has to still be there.
     */
    @Test
    public void cursorModeWithoutLimitUsesTheDefaultPageSize() {
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<BioAssayValueObject> cp = new CursorPage<>(
                Collections.singletonList( ba1 ), null, 20, null, null, null );
        when( datasetArgService.getSamplesByCursor( any( DatasetArg.class ), eq( c ), eq( 20 ), anyBoolean() ) ).thenReturn( cp );

        Object response = webService.getDatasetSamples( datasetArg, null, false, CursorArg.valueOf( c.encode() ), null, null, false );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( datasetArgService ).getSamplesByCursor( any( DatasetArg.class ), eq( c ), eq( 20 ), anyBoolean() );
    }

    @Test
    public void cursorWithUseProcessedQuantitationTypeIsRejectedAs400() {
        // Same rationale as cursorWithQuantitationTypeIsRejectedAs400 — the implicit "preferred QT"
        // path also narrows by BioAssayDimension and is not id-cursorable.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );

        assertThatThrownBy( () -> webService.getDatasetSamples( datasetArg, null, true, CursorArg.valueOf( c.encode() ), limit( "20" ), null, false ) )
                .isInstanceOf( BadRequestException.class );

        verify( datasetArgService, never() ).getSamplesByCursor( any( DatasetArg.class ), any(), anyInt(), anyBoolean() );
        verify( datasetArgService, never() ).getSamples( any( DatasetArg.class ), anyBoolean() );
    }
}
