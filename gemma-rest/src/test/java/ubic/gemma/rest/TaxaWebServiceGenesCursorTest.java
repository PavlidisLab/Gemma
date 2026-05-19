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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.GeneArgService;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.TaxonArg;
import ubic.gemma.rest.util.args.TaxonArgService;

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
 * {@link TaxaWebService#getTaxonGenes(TaxonArg, OffsetArg, LimitArg, CursorArg)} as
 * step 1g of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to verify the
 * WebService routes cursor vs offset modes to the right helper and emits the right response
 * wrapper, not to retest the DAO (covered by the broader DAO-cursor suite).
 *
 * Mirrors {@link PlatformsWebServiceDatasetsCursorTest} (step 1f, closest analog — same shape:
 * path-derived scope filter, no echoed user {@code ?filter=}, so cursor mode emits a plain
 * {@link CursorPaginatedResponseDataObject} not a Filtered one). Differences:
 * - The listing returns {@link GeneValueObject genes} for the taxon, not experiments; the
 *   path-derived constraint is {@code taxon.id = ?} (composed inside
 *   {@code GeneArgService.getGenesInTaxonByCursor}, not in the WebService).
 * - {@code GeneService.populateAssociatedExperimentCount} is called on both the legacy
 *   {@link Slice} and the new {@link CursorPage} to keep the response shape identical to the
 *   pre-cursor endpoint (each {@link GeneValueObject} carries an experiment count).
 *
 * @author phase3
 */
@RunWith(MockitoJUnitRunner.class)
public class TaxaWebServiceGenesCursorTest {

    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
    private TaxonService taxonService;
    @Mock
    @SuppressWarnings("unused")
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private TaxonArgService taxonArgService;
    @Mock
    @SuppressWarnings("unused")
    private DatasetArgService datasetArgService;
    @Mock
    private GeneArgService geneArgService;
    @Mock
    private GeneService geneService;

    @InjectMocks
    private TaxaWebService webService;

    private Taxon taxon;
    private GeneValueObject gene1;
    private GeneValueObject gene2;

    @Before
    public void setUp() {
        taxon = new Taxon();
        taxon.setId( 42L );
        gene1 = new GeneValueObject( 100L );
        gene2 = new GeneValueObject( 200L );
        when( taxonArgService.getEntity( any( TaxonArg.class ) ) ).thenReturn( taxon );
    }

    private TaxonArg<?> taxonArg() {
        return TaxonArg.valueOf( "42" );
    }

    private OffsetArg offset( String s ) {
        return OffsetArg.valueOf( s );
    }

    private LimitArg limit( String s ) {
        return LimitArg.valueOf( s );
    }

    @Test
    public void offsetModeRoutesToLegacyHelperAndReturnsPaginatedResponse() {
        Slice<GeneValueObject> slice = new Slice<>(
                Arrays.asList( gene1, gene2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                0, 20, 2L );
        when( geneArgService.getGenesInTaxon( eq( taxon ), anyInt(), anyInt() ) ).thenReturn( slice );

        Object response = webService.getTaxonGenes( taxonArg(), offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( PaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<GeneValueObject> page =
                ( PaginatedResponseDataObject<GeneValueObject> ) response;
        assertThat( page.getData() ).containsExactly( gene1, gene2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        // associated-experiment-count enrichment runs in both modes; verify the legacy path
        verify( geneService ).populateAssociatedExperimentCount( ( Collection<GeneValueObject> ) slice );
        verify( geneArgService, never() ).getGenesInTaxonByCursor( any(), any(), anyInt() );
    }

    @Test
    public void cursorModeRoutesToCursorHelperAndReturnsCursorResponse() {
        Cursor c = new Cursor( "+id", new Object[] { 99L }, Cursor.Direction.FORWARD );
        CursorPage<GeneValueObject> cp = new CursorPage<>(
                Arrays.asList( gene1, gene2 ),
                Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ),
                20,
                /* nextCursor */ "next-cursor-token",
                /* prevCursor */ "prev-cursor-token",
                /* totalElements */ null );
        when( geneArgService.getGenesInTaxonByCursor( eq( taxon ), eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getTaxonGenes( taxonArg(), offset( "0" ), limit( "20" ), arg );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<GeneValueObject> page =
                ( CursorPaginatedResponseDataObject<GeneValueObject> ) response;
        assertThat( page.getData() ).containsExactly( gene1, gene2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        // associated-experiment-count enrichment must also run in cursor mode so the response
        // shape matches the legacy endpoint
        verify( geneService ).populateAssociatedExperimentCount( ( Collection<GeneValueObject> ) cp );
        // offset-mode legacy helper must not be touched
        verify( geneArgService, never() ).getGenesInTaxon( any( Taxon.class ), anyInt(), anyInt() );
    }

    @Test
    public void cursorModeDecodesCursorAndForwardsToArgService() {
        // The cursor flows from CursorArg.valueOf(...) (which base64-decodes the token) all the way
        // through to GeneArgService.getGenesInTaxonByCursor — verify the decoded Cursor value
        // arrives equal to what we put in.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<GeneValueObject> cp = new CursorPage<>(
                List.of( gene2 ), null, 5, null, "prev", null );
        when( geneArgService.getGenesInTaxonByCursor( eq( taxon ), eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getTaxonGenes( taxonArg(), offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( geneArgService ).getGenesInTaxonByCursor( eq( taxon ), eq( c ), eq( 5 ) );
    }

    @Test
    public void cursorModeForwardsResolvedTaxon() {
        // The Taxon resolved from the TaxonArg is forwarded straight through to the arg-service;
        // the path-derived taxon.id = ? filter is composed inside
        // GeneArgService.getGenesInTaxonByCursor, not in the WebService. Verify the WebService
        // doesn't drop or substitute the resolved taxon entity.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<GeneValueObject> cp = new CursorPage<>(
                List.of( gene1 ), null, 10, null, null, null );
        when( geneArgService.getGenesInTaxonByCursor( eq( taxon ), eq( c ), eq( 10 ) ) ).thenReturn( cp );

        webService.getTaxonGenes( taxonArg(), offset( "0" ), limit( "10" ), CursorArg.valueOf( c.encode() ) );

        verify( geneArgService ).getGenesInTaxonByCursor( eq( taxon ), eq( c ), eq( 10 ) );
    }
}
