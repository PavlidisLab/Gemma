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
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.GeneArgService;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the cursor-pagination branch added to
 * {@link GeneWebService#getGenes(OffsetArg, LimitArg, CursorArg)} as proof-of-concept for
 * step 1b of {@code CURSOR_PAGINATION_STEP1_PLAN.md}. Pure Mockito — the goal is to verify
 * the WebService routes cursor vs offset modes to the right helper and emits the right
 * response wrapper, not to retest the DAO (covered by {@code ExpressionExperimentDaoCursorTest}).
 *
 * @author phase3
 */
@ExtendWith(MockitoExtension.class)
public class GeneWebServiceCursorTest {

    @Mock
    private GeneService geneService;

    @Mock
    private GeneArgService geneArgService;

    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @InjectMocks
    private GeneWebService webService;

    private GeneValueObject gene1;
    private GeneValueObject gene2;

    @BeforeEach
    public void setUp() {
        gene1 = new GeneValueObject();
        gene1.setId( 100L );
        gene1.setOfficialSymbol( "G1" );
        gene2 = new GeneValueObject();
        gene2.setId( 200L );
        gene2.setOfficialSymbol( "G2" );
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
        when( geneArgService.getGenes( 0, 20 ) ).thenReturn( slice );

        Object response = webService.getGenes( offset( "0" ), limit( "20" ), null );

        assertThat( response ).isInstanceOf( PaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        PaginatedResponseDataObject<GeneValueObject> page = ( PaginatedResponseDataObject<GeneValueObject> ) response;
        assertThat( page.getData() ).containsExactly( gene1, gene2 );
        assertThat( page.getOffset() ).isEqualTo( 0 );
        assertThat( page.getLimit() ).isEqualTo( 20 );
        assertThat( page.getTotalElements() ).isEqualTo( 2L );

        verify( geneService ).populateAssociatedExperimentCount( slice );
        verify( geneArgService, never() ).getGenesByCursor( any(), eq( 20 ) );
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
        when( geneArgService.getGenesByCursor( eq( c ), eq( 20 ) ) ).thenReturn( cp );

        CursorArg arg = CursorArg.valueOf( c.encode() );
        Object response = webService.getGenes( offset( "0" ), limit( "20" ), arg );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        @SuppressWarnings("unchecked")
        CursorPaginatedResponseDataObject<GeneValueObject> page = ( CursorPaginatedResponseDataObject<GeneValueObject> ) response;
        assertThat( page.getData() ).containsExactly( gene1, gene2 );
        assertThat( page.getNextCursor() ).isEqualTo( "next-cursor-token" );
        assertThat( page.getPrevCursor() ).isEqualTo( "prev-cursor-token" );
        // cursor mode does not count by default
        assertThat( page.getTotalElements() ).isNull();
        assertThat( page.getLimit() ).isEqualTo( 20 );

        verify( geneService ).populateAssociatedExperimentCount( cp );
        verify( geneArgService, never() ).getGenes( eq( 0 ), eq( 20 ) );
    }

    @Test
    public void cursorModePassesNullCursorThrough() {
        // First-page request in cursor mode: a client may pass cursor=<empty> or omit it; only
        // when cursorArg is non-null do we enter cursor mode, but the cursor *inside* the arg
        // may still be a valid token. Here we verify that GeneArgService.getGenesByCursor is
        // called with the decoded cursor (not null) for a real token.
        Cursor c = new Cursor( "+id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        CursorPage<GeneValueObject> cp = new CursorPage<>(
                List.of( gene2 ), null, 5, null, "prev", null );
        when( geneArgService.getGenesByCursor( eq( c ), eq( 5 ) ) ).thenReturn( cp );

        Object response = webService.getGenes( offset( "0" ), limit( "5" ), CursorArg.valueOf( c.encode() ) );

        assertThat( response ).isInstanceOf( CursorPaginatedResponseDataObject.class );
        verify( geneArgService ).getGenesByCursor( eq( c ), eq( 5 ) );
    }
}
