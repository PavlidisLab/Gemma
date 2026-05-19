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
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.FilteredAndCursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CompositeSequenceArg;
import ubic.gemma.rest.util.args.CompositeSequenceArgService;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.PlatformArg;
import ubic.gemma.rest.util.args.PlatformArgService;

import java.util.Arrays;
import java.util.Collections;
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
 * {@link PlatformsWebService#getPlatformElementGenes(PlatformArg, CompositeSequenceArg, OffsetArg, LimitArg, CursorArg)}
 * as step 1l of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to verify
 * the WebService routes cursor vs offset modes to the right helper and emits the right
 * response wrapper, not to retest the DAO (covered by the broader DAO-cursor suite).
 * <p>
 * Mirrors the step 1j {@link PlatformsWebServiceElementCursorTest} structure (same web service
 * file, same response wrappers). Differences:
 * <ul>
 *   <li>This endpoint scopes the listing to a single {@code {probe}} (not an array of probes)
 *       and maps the listing to {@link GeneValueObject} via {@code geneService::loadValueObject}.
 *   <li>The offset variant returns {@code Slice<Gene>} (entities) and the WebService maps it
 *       to {@code Slice<GeneValueObject>} via {@code Slice.map}; the cursor variant does the
 *       same via {@code CursorPage.map}.
 *   <li>The cursor variant delegates to {@link CompositeSequenceArgService#getGenesByCursor},
 *       which itself resolves the probe against the platform (mirroring the offset call
 *       {@code probeArgService.getEntityWithPlatform(probeArg, ...)}). The WebService also
 *       re-computes the echoed {@code filter} purely for the response wrapper.
 * </ul>
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformsWebServiceElementGenesCursorTest {

    @Mock
    private GeneService geneService;
    @Mock
    @SuppressWarnings("unused")
    private ArrayDesignService arrayDesignService;
    @Mock
    private CompositeSequenceService compositeSequenceService;
    @Mock
    @SuppressWarnings("unused")
    private ArrayDesignAnnotationService annotationFileService;
    @Mock
    private PlatformArgService arrayDesignArgService;
    @Mock
    private CompositeSequenceArgService probeArgService;
    @Mock
    @SuppressWarnings("unused")
    private AccessDecisionManager accessDecisionManager;
    @Mock
    @SuppressWarnings("unused")
    private TicketsWebService ticketsWebService;

    @InjectMocks
    private PlatformsWebService webService;

    private Gene gene1;
    private Gene gene2;
    private GeneValueObject vo1;
    private GeneValueObject vo2;
    private PlatformArg<?> platformArg;
    private CompositeSequenceArg<?> probeArg;
    private ArrayDesign platform;
    private CompositeSequence probe;

    @Before
    public void setUp() {
        gene1 = new Gene();
        gene1.setId( 100L );
        gene2 = new Gene();
        gene2.setId( 200L );
        vo1 = new GeneValueObject();
        vo1.setId( 100L );
        vo2 = new GeneValueObject();
        vo2.setId( 200L );
        platformArg = PlatformArg.valueOf( "42" );
        probeArg = CompositeSequenceArg.valueOf( "AFFX_a" );
        platform = new ArrayDesign();
        platform.setId( 42L );
        platform.setShortName( "GPL1355" );
        probe = new CompositeSequence();
        probe.setId( 7L );
        probe.setArrayDesign( platform );
        when( arrayDesignArgService.getEntity( any( PlatformArg.class ) ) ).thenReturn( platform );
        when( geneService.loadValueObject( gene1 ) ).thenReturn( vo1 );
        when( geneService.loadValueObject( gene2 ) ).thenReturn( vo2 );
        // Both modes echo the path-derived filter on the response wrapper; stub the arg-service
        // to return a deterministic Filters value so we can compare the wrapper's filter field.
        when( probeArgService.getFilters( any( CompositeSequenceArg.class ) ) ).thenReturn( Filters.empty() );
    }

    private OffsetArg offset( String s ) {
        return OffsetArg.valueOf( s );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsFilteredPaginatedResponse() {
        // The legacy path: probeArgService.getEntityWithPlatform → compositeSequenceService.getGenes →
        // Slice<Gene>.map(geneService::loadValueObject) → paginate(...).
        when( probeArgService.getEntityWithPlatform( any( CompositeSequenceArg.class ), eq( platform ) ) ).thenReturn( probe );
        Slice<Gene> slice = new Slice<>(
                Arrays.asList( gene1, gene2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                0, 20, 2L );
        when( compositeSequenceService.getGenes( eq( probe ), eq( 0 ), eq( 20 ), eq( true ) ) ).thenReturn( slice );

        Object response = webService.getPlatformElementGenes( platformArg, probeArg, offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( FilteredAndPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndPaginatedResponseDataObject<GeneValueObject> page =
                ( FilteredAndPaginatedResponseDataObject<GeneValueObject> ) response;
        assertThat( page.getData() ).containsExactly( vo1, vo2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        // Cursor helper must not be touched in legacy mode.
        verify( probeArgService, never() ).getGenesByCursor( any( CompositeSequenceArg.class ), any( ArrayDesign.class ), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsFilteredCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 99L }, Cursor.Direction.FORWARD );
        CursorPage<Gene> cp = new CursorPage<>(
                Arrays.asList( gene1, gene2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( probeArgService.getGenesByCursor( any( CompositeSequenceArg.class ), eq( platform ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getPlatformElementGenes( platformArg, probeArg, offset( "0" ), limit( "20" ), arg );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndCursorPaginatedResponseDataObject<GeneValueObject> page =
                ( FilteredAndCursorPaginatedResponseDataObject<GeneValueObject> ) response;
        assertThat( page.getData() ).containsExactly( vo1, vo2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // legacy compositeSequenceService.getGenes must not be touched in cursor mode
        verify( compositeSequenceService, never() ).getGenes( any( CompositeSequence.class ), anyInt(), anyInt(), eq( true ) );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to CompositeSequenceArgService.getGenesByCursor — verify the decoded Cursor value
        // arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Gene> cp = new CursorPage<>(
                Collections.singletonList( gene2 ), null, 5, null, "prev", null );
        when( probeArgService.getGenesByCursor( any( CompositeSequenceArg.class ), eq( platform ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getPlatformElementGenes( platformArg, probeArg, offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        verify( probeArgService ).getGenesByCursor( any( CompositeSequenceArg.class ), eq( platform ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeForwardsBothPlatformArgAndProbeArg() {
        // Both path args are passed straight through: platformArg is resolved by the WebService
        // (to the ArrayDesign that's forwarded to the arg-service), and probeArg is forwarded
        // unchanged. Verify the WebService doesn't drop or substitute either path arg.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Gene> cp = new CursorPage<>(
                Collections.singletonList( gene1 ), null, 10, null, null, null );
        when( probeArgService.getGenesByCursor( eq( probeArg ), eq( platform ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getPlatformElementGenes( platformArg, probeArg, offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( arrayDesignArgService ).getEntity( eq( platformArg ) );
        verify( probeArgService ).getGenesByCursor( eq( probeArg ), eq( platform ), eq( c ), eq( 10 ) );
    }

    @Test
    public void cursorModeEmptyPageProducesEmptyResponseWithNoNextCursor() {
        // An empty CursorPage (no matches under the cursor predicate) round-trips through
        // CursorPage.map and into the FilteredAndCursorPaginatedResponseDataObject without
        // synthesizing cursor tokens or borrowing the input cursor.
        Cursor c = new Cursor( "+id", new Object[] { 999999L }, Cursor.Direction.FORWARD );
        CursorPage<Gene> cp = new CursorPage<>(
                Collections.emptyList(), null, 20, /* nextCursor */ null, /* prevCursor */ null, null );
        when( probeArgService.getGenesByCursor( any( CompositeSequenceArg.class ), eq( platform ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        Object response = webService.getPlatformElementGenes( platformArg, probeArg, offset( "0" ), limit( "20" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( FilteredAndCursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        FilteredAndCursorPaginatedResponseDataObject<GeneValueObject> page =
                ( FilteredAndCursorPaginatedResponseDataObject<GeneValueObject> ) response;
        assertThat( page.getData() ).isEmpty();
        assertThat( page.getNextCursor() ).isNull();
        assertThat( page.getPrevCursor() ).isNull();
        assertThat( page.getTotalElements() ).isNull();
    }

    @Test
    public void cursorModeMapsGeneEntitiesToValueObjects() {
        // The WebService's responsibility is to map Gene → GeneValueObject via geneService::loadValueObject;
        // verify that the mapper is invoked for each returned entity (the cursor-mode counterpart of the
        // offset variant's Slice.map call).
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<Gene> cp = new CursorPage<>(
                Arrays.asList( gene1, gene2 ), null, 10, null, null, null );
        when( probeArgService.getGenesByCursor( any( CompositeSequenceArg.class ), eq( platform ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        Object response = webService.getPlatformElementGenes( platformArg, probeArg, offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( geneService ).loadValueObject( gene1 );
        verify( geneService ).loadValueObject( gene2 );
        @SuppressWarnings("unchecked")
        FilteredAndCursorPaginatedResponseDataObject<GeneValueObject> page =
                ( FilteredAndCursorPaginatedResponseDataObject<GeneValueObject> ) response;
        // List preserves DAO order (asc by gene.id)
        List<GeneValueObject> data = page.getData();
        assertThat( data ).containsExactly( vo1, vo2 );
    }
}
