/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.geoscrape;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoQuery;
import ubic.gemma.core.loader.expression.geo.service.GeoRetrieveConfig;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.GeoScrapeWatermark;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.expression.experiment.PreboardedExperimentService;
import ubic.gemma.persistence.util.Slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito tests for the {@link GeoScrapeServiceImpl} watermark-loop +
 * enrichment + ticket-emission paths. The persistence methods on the impl
 * (persistWatermark / updateWatermark / updateMatchedCriteria) are spied
 * out so the test doesn't touch a real {@code SessionFactory} or
 * {@code TransactionTemplate}.
 *
 * @author phase 3 geo-scrape pipeline
 */
public class GeoScrapeServiceImplTest {

    private TicketService ticketService;
    private UserManager userManager;
    private PreboardedExperimentService preboardedExperimentService;
    private GeoBrowser geoBrowser;
    private GeoScrapeServiceImpl svc;

    @BeforeEach
    public void setUp() throws Exception {
        ticketService = mock( TicketService.class );
        userManager = mock( UserManager.class );
        preboardedExperimentService = mock( PreboardedExperimentService.class );
        geoBrowser = mock( GeoBrowser.class );

        // Mockito 5 can't deep-mock SessionFactory + PlatformTransactionManager cleanly under JDK 25 —
        // pass them as nulls and spy on the impl to override the three transactional helpers.
        GeoScrapeServiceImpl real = new GeoScrapeServiceImpl(
                null /* sessionFactory */,
                preboardedExperimentService,
                Arrays.<GeoRecordMatcher>asList( new BrainKeywordMatcher(), new TfPerturbationMatcher() ),
                null /* transactionManager */,
                ticketService,
                userManager );
        svc = spy( real );

        // Replace the @Transactional helpers with no-op / id-stamping equivalents.
        AtomicLong watermarkIdGen = new AtomicLong( 100L );
        doNothing().when( svc ).updateWatermark( any( GeoScrapeWatermark.class ) );
        doNothing().when( svc ).updateMatchedCriteria( any( String.class ), any( String.class ) );
        doReturn( null ).when( svc ).getLastCompletedWatermark();
        // persistWatermark stamps an id on the wm so the ticket-target path has something to point at.
        doAnswer( inv -> {
            GeoScrapeWatermark w = inv.getArgument( 0 );
            w.setId( watermarkIdGen.getAndIncrement() );
            return w;
        } ).when( svc ).persistWatermark( any( GeoScrapeWatermark.class ) );

        svc.setGeoBrowser( geoBrowser );
        svc.setPageSize( 50 );

        // Default reporter
        User reporter = mock( User.class );
        when( userManager.getCurrentUser() ).thenReturn( reporter );

        // Default GeoBrowser plumbing — returns the configured slice from a single retrieve call.
        GeoQuery query = mock( GeoQuery.class );
        when( geoBrowser.searchGeoRecords( any(), any(), any(), any(), any(), any(), any(), any() ) ).thenReturn( query );
    }

    private GeoRecord rec( String acc, String title, String organism ) {
        GeoRecord r = new GeoRecord();
        r.setGeoAccession( acc );
        r.setTitle( title );
        r.setOrganisms( Collections.singleton( organism ) );
        r.setSeriesType( "Expression profiling by high throughput sequencing" );
        r.setLibraryStrategy( "RNA-Seq" );
        return r;
    }

    private void wireSlice( List<GeoRecord> records ) throws Exception {
        // First call returns the records; second call returns empty (loop exit).
        Slice<GeoRecord> first = new Slice<>( records, null, 0, records.size(), ( long ) records.size() );
        Slice<GeoRecord> empty = new Slice<>( new ArrayList<>(), null, records.size(), 0, ( long ) records.size() );
        when( geoBrowser.retrieveGeoRecords( any( GeoQuery.class ), eq( 0 ), any( Integer.class ), any( GeoRetrieveConfig.class ) ) )
                .thenReturn( first );
        when( geoBrowser.retrieveGeoRecords( any( GeoQuery.class ), eq( records.size() ), any( Integer.class ), any( GeoRetrieveConfig.class ) ) )
                .thenReturn( empty );
    }

    private void wirePreboardedCreate() throws Exception {
        AtomicLong idGen = new AtomicLong( 1000L );
        when( preboardedExperimentService.createPreboarded( any( String.class ), any( String.class ), any() ) )
                .thenAnswer( inv -> {
                    PreboardedExperiment pb = new PreboardedExperiment();
                    pb.setAccession( inv.getArgument( 0 ) );
                    pb.setId( idGen.getAndIncrement() );
                    return pb;
                } );
    }

    /* ===== Phase 1 — identifyingMetadata enrichment ===== */

    @Test
    public void buildIdentifyingMetadata_emitsCuratorRelevantFields() {
        GeoRecord r = new GeoRecord();
        r.setGeoAccession( "GSE12345" );
        r.setTitle( "Single-cell RNA-seq of human cortex" );
        r.setSummary( "Brain study summary." );
        r.setOrganisms( Collections.singleton( "Homo sapiens" ) );
        r.setPlatform( "GPL16791" );
        r.setSeriesType( "Expression profiling by high throughput sequencing" );
        r.setNumSamples( 24 );
        r.setLibraryStrategy( "RNA-Seq" );
        r.setPubMedIds( Arrays.asList( "12345678" ) );

        String json = svc.buildIdentifyingMetadata( r );
        assertThat( json ).isNotNull();
        assertThat( json ).contains( "\"geoAccession\":\"GSE12345\"" );
        assertThat( json ).contains( "\"title\":\"Single-cell RNA-seq of human cortex\"" );
        assertThat( json ).contains( "\"organisms\":[\"Homo sapiens\"]" );
        assertThat( json ).contains( "\"platform\":\"GPL16791\"" );
        assertThat( json ).contains( "\"numSamples\":24" );
        assertThat( json ).contains( "\"libraryStrategy\":\"RNA-Seq\"" );
        assertThat( json ).contains( "\"pubMedIds\":[\"12345678\"]" );
        assertThat( json ).contains( "\"scrapedAt\":" );
    }

    @Test
    public void buildIdentifyingMetadata_nullSafe() {
        assertThat( svc.buildIdentifyingMetadata( null ) ).isNull();
    }

    /* ===== Phase 3 — ticket emission ===== */

    @Test
    public void scrape_happyPath_createsPreboardedsAndOneTicketWithEnrichedMetadata() throws Exception {
        GeoRecord r1 = rec( "GSE0001", "Single-cell RNA-seq of mouse cortex", "Mus musculus" );
        GeoRecord r2 = rec( "GSE0002", "Brain hippocampus profile", "Homo sapiens" );
        wireSlice( Arrays.asList( r1, r2 ) );
        wirePreboardedCreate();

        GeoScrapeService.ScrapeRequest req = new GeoScrapeService.ScrapeRequest();
        req.setMaxRecords( 10 );

        GeoScrapeWatermark wm = svc.scrape( req );

        assertThat( wm.getStatus() ).isEqualTo( GeoScrapeWatermark.Status.COMPLETED );
        assertThat( wm.getRecordsMatched() ).isEqualTo( 2 );

        // identifyingMetadata is non-empty JSON for each preboarded create call
        ArgumentCaptor<String> metaCap = ArgumentCaptor.forClass( String.class );
        verify( preboardedExperimentService, times( 2 ) )
                .createPreboarded( any( String.class ), eq( "GEO" ), metaCap.capture() );
        for ( String meta : metaCap.getAllValues() ) {
            assertThat( meta ).isNotNull();
            assertThat( meta ).startsWith( "{" );
            assertThat( meta ).contains( "\"geoAccession\":\"GSE" );
        }

        // One ticket opened, target shape correct
        ArgumentCaptor<Collection<TicketTarget>> tgtCap = ArgumentCaptor.forClass( Collection.class );
        ArgumentCaptor<String> titleCap = ArgumentCaptor.forClass( String.class );
        verify( ticketService, times( 1 ) ).openTicket(
                any( User.class ), eq( TicketType.GENERIC ), titleCap.capture(), tgtCap.capture() );
        assertThat( titleCap.getValue() ).matches( "GEO scrape \\d{4}-\\d{2}-\\d{2}: 2 candidates" );
        Collection<TicketTarget> targets = tgtCap.getValue();
        assertThat( targets ).hasSize( 1 );
        TicketTarget tt = targets.iterator().next();
        assertThat( tt.getTargetType() ).isEqualTo( TicketTargetType.GEO_SCRAPE_WATERMARK );
        assertThat( tt.getTargetId() ).isNotNull();
    }

    @Test
    public void scrape_forwardsSinceAndUntilToGeoBrowser() throws Exception {
        // Date-window plumbing: ScrapeRequest.since/until reach the GeoBrowser.searchGeoRecords
        // 8-arg overload verbatim. The browser-side PDAT filter is added there; this test just
        // proves the plumbing is wired so the GeoBrowser sees the dates.
        wireSlice( Collections.emptyList() );

        java.util.Date since = new java.util.GregorianCalendar( 2026, java.util.Calendar.APRIL, 1 ).getTime();
        java.util.Date until = new java.util.GregorianCalendar( 2026, java.util.Calendar.APRIL, 14 ).getTime();
        GeoScrapeService.ScrapeRequest req = new GeoScrapeService.ScrapeRequest();
        req.setSince( since );
        req.setUntil( until );
        req.setMaxRecords( 10 );
        req.setDryRun( true );

        svc.scrapeDryRun( req );

        ArgumentCaptor<java.util.Date> sinceCap = ArgumentCaptor.forClass( java.util.Date.class );
        ArgumentCaptor<java.util.Date> untilCap = ArgumentCaptor.forClass( java.util.Date.class );
        verify( geoBrowser ).searchGeoRecords( any(), any(), any(), any(), any(), any(), sinceCap.capture(), untilCap.capture() );
        assertThat( sinceCap.getValue() ).isEqualTo( since );
        assertThat( untilCap.getValue() ).isEqualTo( until );
    }

    @Test
    public void scrape_zeroMatches_doesNotOpenTicket() throws Exception {
        // Record with no brain / TF signal — neither matcher fires.
        GeoRecord r = rec( "GSE0009", "Pancreatic islet bulk expression", "Homo sapiens" );
        wireSlice( Arrays.asList( r ) );
        wirePreboardedCreate();

        GeoScrapeService.ScrapeRequest req = new GeoScrapeService.ScrapeRequest();
        req.setMaxRecords( 10 );

        GeoScrapeWatermark wm = svc.scrape( req );
        assertThat( wm.getStatus() ).isEqualTo( GeoScrapeWatermark.Status.COMPLETED );
        assertThat( wm.getRecordsMatched() ).isEqualTo( 0 );

        verify( ticketService, never() ).openTicket( any(), any(), any(), any() );
        verify( preboardedExperimentService, never() ).createPreboarded( any(), any(), any() );
    }

    @Test
    public void scrape_cancelledMidLoop_doesNotOpenTicket() throws Exception {
        // Use a slice that's larger than maxRecords; interrupt the thread before the loop turns.
        List<GeoRecord> recs = new ArrayList<>();
        for ( int i = 0; i < 5; i++ ) {
            recs.add( rec( "GSE000" + i, "Brain study " + i, "Homo sapiens" ) );
        }
        wireSlice( recs );
        wirePreboardedCreate();

        Thread.currentThread().interrupt(); // arms the cancel path on first loop check
        try {
            GeoScrapeService.ScrapeRequest req = new GeoScrapeService.ScrapeRequest();
            req.setMaxRecords( 10 );
            GeoScrapeWatermark wm = svc.scrape( req );
            assertThat( wm.getStatus() ).isEqualTo( GeoScrapeWatermark.Status.CANCELLED );
        } finally {
            // Clear any residual interrupt so it doesn't leak into other tests.
            Thread.interrupted();
        }

        verify( ticketService, never() ).openTicket( any(), any(), any(), any() );
    }

    @Test
    public void buildTicketNote_truncatesAt20WithOverflowMarker() {
        Map<String, Integer> matched = new LinkedHashMap<>();
        matched.put( "brain", 25 );
        matched.put( "tfperturb", 0 ); // zero counts dropped
        List<Long> ids = new ArrayList<>();
        for ( long i = 1; i <= 25; i++ ) ids.add( i );

        String note = GeoScrapeServiceImpl.buildTicketNote( matched, ids );
        assertThat( note ).startsWith( "matched: brain×25" );
        assertThat( note ).doesNotContain( "tfperturb" ); // zero-count entries elided
        assertThat( note ).contains( "preboarded ids: 1,2,3" );
        assertThat( note ).contains( "20" );
        assertThat( note ).doesNotContain( ",21," ); // 21st id not listed
        assertThat( note ).contains( "(...and 5 more)" );
    }

    @Test
    public void buildTicketNote_smallListDoesNotTruncate() {
        Map<String, Integer> matched = new LinkedHashMap<>();
        matched.put( "brain", 2 );
        List<Long> ids = Arrays.asList( 10L, 11L );

        String note = GeoScrapeServiceImpl.buildTicketNote( matched, ids );
        assertThat( note ).isEqualTo( "matched: brain×2; preboarded ids: 10,11" );
    }
}
