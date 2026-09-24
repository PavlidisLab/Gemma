/*
 * The gemma-rest project
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
import org.mockito.ArgumentCaptor;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchMatchType;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.rest.util.args.TaxonArg;
import ubic.gemma.rest.util.args.TaxonArgService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Regression guard for the candidate window on {@code GET /genes/search}.
 * <p>
 * {@code searchGenes} runs two local passes over whatever the search service hands back — a
 * within-score-band re-rank, and a taxon backstop that drops cross-taxa hits. Both only reorder
 * or discard; neither can retrieve. So the width requested from the search service has to exceed
 * the caller's {@code limit}, or the cut decides the answer before either pass runs.
 * <p>
 * It did, on prod: {@code ?query=Myc&taxon=mouse&limit=1} returned an EMPTY list, because search
 * returned exactly one arbitrary {@code Myc} ortholog (rat) and the mouse backstop then dropped
 * it. Ranking was limit-dependent for the same reason — {@code limit=1} answered rat while
 * {@code limit=3} put mouse first.
 * <p>
 * Constructed directly rather than through a Spring context, per the repo's
 * {@code AbstractAsyncFactoryBean} / spring-test 6.2 guidance.
 */
class GeneSearchCandidateWindowTest {

    private GeneWebService service;
    private SearchService searchService;
    private GeneService geneService;
    private TaxonArgService taxonArgService;

    private static final Long MOUSE_ID = 2L;
    private static final Long RAT_ID = 3L;

    @BeforeEach
    void setUp() {
        service = new GeneWebService();
        searchService = mock( SearchService.class );
        geneService = mock( GeneService.class );
        taxonArgService = mock( TaxonArgService.class );
        setField( service, "searchService", searchService );
        setField( service, "geneService", geneService );
        setField( service, "taxonArgService", taxonArgService );
    }

    /**
     * The width asked of the search service must not be the caller's {@code limit}. This is the
     * assertion that would have failed before the fix: {@code maxResults} was {@code limit}.
     */
    @Test
    void searchIsAskedForAWiderWindowThanTheCallerRequested() throws SearchException {
        stubSearchWith( geneResult( "Myc", MOUSE_ID ) );

        service.searchGenes( "Myc", null, 1 );

        ArgumentCaptor<SearchSettings> captured = ArgumentCaptor.forClass( SearchSettings.class );
        verify( searchService ).search( captured.capture(), any() );
        assertThat( captured.getValue().getMaxResults() )
                .as( "candidate window handed to SearchService" )
                .isGreaterThan( GeneWebService.SEARCH_MAX_LIMIT );
    }

    /**
     * The reported failure, end to end: a taxon-scoped single-result query whose best global hit
     * belongs to another taxon must still answer with the requested taxon's gene, not an empty
     * list.
     */
    @Test
    void taxonScopedSingleResultSurvivesAHigherRankedOtherTaxonHit() throws SearchException {
        // Rat ranks first globally; mouse is the one actually asked for.
        stubSearchWith( geneResult( "Myc", RAT_ID ), geneResult( "Myc", MOUSE_ID ) );
        TaxonArg<?> mouseArg = mock( TaxonArg.class );
        when( taxonArgService.getEntity( mouseArg ) ).thenReturn( taxon( MOUSE_ID ) );

        List<GeneValueObject> got = service.searchGenes( "Myc", mouseArg, 1 ).getData();

        assertThat( got ).singleElement()
                .satisfies( vo -> assertThat( vo.getTaxon().getId() ).isEqualTo( MOUSE_ID ) );
    }

    /** The caller's limit is still honoured — the wider window must not leak into the response. */
    @Test
    void callersLimitStillTruncatesTheResponse() throws SearchException {
        stubSearchWith( geneResult( "Myc", MOUSE_ID ), geneResult( "Mycl", MOUSE_ID ),
                geneResult( "Mycn", MOUSE_ID ), geneResult( "Mycbp", MOUSE_ID ) );

        assertThat( service.searchGenes( "Myc", null, 2 ).getData() ).hasSize( 2 );
    }

    /** Aliases are batch-loaded, and only for what is actually returned. */
    @Test
    void aliasLoadRunsOnTheTruncatedListNotTheCandidateWindow() throws SearchException {
        stubSearchWith( geneResult( "Myc", MOUSE_ID ), geneResult( "Mycl", MOUSE_ID ),
                geneResult( "Mycn", MOUSE_ID ), geneResult( "Mycbp", MOUSE_ID ) );

        service.searchGenes( "Myc", null, 2 );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GeneValueObject>> captured = ArgumentCaptor.forClass( List.class );
        verify( geneService ).populateAliases( captured.capture() );
        assertThat( captured.getValue() ).hasSize( 2 );
    }

    /**
     * Stand in for the search service, <b>honouring {@code maxResults}</b> — that truncation is the
     * whole mechanism under test. A stub that ignored it would pass against the buggy code, because
     * the endpoint's own filtering would still see every candidate.
     */
    private void stubSearchWith( SearchResult<?>... results ) throws SearchException {
        doAnswer( inv -> {
            SearchSettings settings = inv.getArgument( 0 );
            int width = settings.getMaxResults() > 0
                    ? Math.min( settings.getMaxResults(), results.length )
                    : results.length;
            SearchService.SearchResultMap map = mock( SearchService.SearchResultMap.class );
            when( map.toList() ).thenReturn( new ArrayList<>( List.of( results ).subList( 0, width ) ) );
            return map;
        } ).when( searchService ).search( any( SearchSettings.class ), any() );
    }

    private static Taxon taxon( Long id ) {
        Taxon t = new Taxon();
        t.setId( id );
        return t;
    }

    private static SearchResult<?> geneResult( String symbol, Long taxonId ) {
        GeneValueObject vo = new GeneValueObject();
        vo.setId( ( long ) symbol.hashCode() );
        vo.setOfficialSymbol( symbol );
        vo.setTaxon( new ubic.gemma.model.genome.TaxonValueObject( taxon( taxonId ) ) );
        SearchResult<GeneValueObject> sr = SearchResult.from( Gene.class, vo, 1.0, null, "test" );
        sr.setMatchKind( SearchMatchType.EXACT_SYMBOL );
        return sr;
    }
}
