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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.model.GeoSeriesType;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowserImpl;
import ubic.gemma.core.loader.expression.geo.service.GeoQuery;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Default {@link GeoScrapeService} implementation.
 *
 * <p>Pages through {@code GeoBrowser} results, filters to
 * expression-profiling records on the allowed taxa, evaluates each registered
 * {@link GeoRecordMatcher} against each record, and creates a
 * {@code PreboardedExperiment} for any record at least one matcher flagged.
 * Tracks progress on a single {@link GeoScrapeWatermark} row per invocation.</p>
 *
 * <p>The watermark is written via a short-lived programmatic transaction so
 * IN_PROGRESS / COMPLETED / FAILED / CANCELLED state changes are visible to
 * the {@code GET /admin/geo-scrape/last} endpoint even while a scrape is in
 * flight. Preboarded creation routes through {@link PreboardedExperimentService}
 * which handles dedupe + audit emission.</p>
 *
 * @author phase 3 geo-scrape pipeline
 */
@Service
public class GeoScrapeServiceImpl implements GeoScrapeService {

    private static final Log log = LogFactory.getLog( GeoScrapeServiceImpl.class );

    /** Allowed taxa for the v1 pipeline. Anything outside this set is dropped. */
    static final Set<String> ALLOWED_TAXA = Collections.unmodifiableSet( new LinkedHashSet<>( Arrays.asList(
            "Homo sapiens", "Mus musculus", "Rattus norvegicus"
    ) ) );

    /** Series types that count as expression profiling. */
    static final Set<GeoSeriesType> EXPRESSION_PROFILING_TYPES = Collections.unmodifiableSet( new LinkedHashSet<>( Arrays.asList(
            GeoSeriesType.EXPRESSION_PROFILING_BY_ARRAY,
            GeoSeriesType.EXPRESSION_PROFILING_BY_HIGH_THROUGHPUT_SEQUENCING,
            GeoSeriesType.EXPRESSION_PROFILING_BY_MPSS,
            GeoSeriesType.EXPRESSION_PROFILING_BY_RT_PRC,
            GeoSeriesType.EXPRESSION_PROFILING_BY_SAGE,
            GeoSeriesType.EXPRESSION_PROFILING_BY_SNP_ARRAY,
            GeoSeriesType.EXPRESSION_PROFILING_BY_TILING_ARRAY
    ) ) );

    private static final int DEFAULT_MAX_RECORDS = 1000;

    private final SessionFactory sessionFactory;
    private final PreboardedExperimentService preboardedExperimentService;
    private final List<GeoRecordMatcher> matchers;
    private final TransactionTemplate transactionTemplate;
    private final TicketService ticketService;
    private final UserManager userManager;
    private final ObjectMapper jsonMapper;

    @Value("${gemma.geoScrape.pageSize:200}")
    private int pageSize = 200;

    /**
     * NCBI API key used by the lazily-constructed {@link GeoBrowserImpl}. Same wire
     * pattern as {@code AdminWebService}'s geo-grab endpoint — GeoBrowser is not a
     * Spring-managed bean in this codebase; CLIs and services construct it on demand.
     */
    @Value("${entrez.efetch.apikey:}")
    private String ncbiApiKey;

    @Nullable
    private GeoBrowser geoBrowser;

    @Autowired
    public GeoScrapeServiceImpl( SessionFactory sessionFactory,
            PreboardedExperimentService preboardedExperimentService,
            List<GeoRecordMatcher> matchers,
            PlatformTransactionManager transactionManager,
            TicketService ticketService,
            UserManager userManager ) {
        this.sessionFactory = sessionFactory;
        this.preboardedExperimentService = preboardedExperimentService;
        this.matchers = matchers != null ? matchers : Collections.emptyList();
        this.transactionTemplate = new TransactionTemplate( transactionManager );
        this.ticketService = ticketService;
        this.userManager = userManager;
        this.jsonMapper = new ObjectMapper()
                .configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
    }

    private synchronized GeoBrowser resolveGeoBrowser() {
        if ( geoBrowser == null ) {
            geoBrowser = new GeoBrowserImpl( ncbiApiKey == null ? "" : ncbiApiKey );
        }
        return geoBrowser;
    }

    /** Test seam. */
    synchronized void setGeoBrowser( GeoBrowser browser ) {
        this.geoBrowser = browser;
    }

    /** Test seam — package-private setter. */
    void setPageSize( int pageSize ) {
        this.pageSize = pageSize;
    }

    @Override
    public GeoScrapeWatermark scrape( ScrapeRequest req ) {
        if ( req == null ) req = new ScrapeRequest();
        List<GeoRecordMatcher> active = selectActive( req.getCriteria() );
        int maxRecords = req.getMaxRecords() != null ? req.getMaxRecords() : DEFAULT_MAX_RECORDS;

        Date scanFrom = req.getSince();
        if ( scanFrom == null ) {
            GeoScrapeWatermark prev = getLastCompletedWatermark();
            scanFrom = prev != null ? prev.getScanTo() : null;
        }
        Date scanTo = new Date();

        GeoScrapeWatermark wm = new GeoScrapeWatermark();
        wm.setScannedAt( new Date() );
        wm.setScanFrom( scanFrom );
        wm.setScanTo( null );
        wm.setStatus( GeoScrapeWatermark.Status.IN_PROGRESS );
        wm.setCriteriaApplied( joinCriteria( active ) );
        wm = persistWatermark( wm );

        int scanned = 0, matched = 0;
        List<Long> createdPreboardedIds = new ArrayList<>();
        Map<String, Integer> matchedByCriterion = new LinkedHashMap<>();
        for ( GeoRecordMatcher m : active ) {
            matchedByCriterion.put( m.name(), 0 );
        }
        try {
            GeoQuery query = resolveGeoBrowser().searchGeoRecords(
                    GeoRecordType.SERIES, null, null,
                    ALLOWED_TAXA, null,
                    EXPRESSION_PROFILING_TYPES,
                    req.getSince(), req.getUntil() );
            int pageStart = 0;
            int effectivePage = Math.max( 1, pageSize );
            while ( scanned < maxRecords ) {
                if ( Thread.interrupted() ) {
                    wm.setStatus( GeoScrapeWatermark.Status.CANCELLED );
                    wm.setRecordsScanned( scanned );
                    wm.setRecordsMatched( matched );
                    wm.setScanTo( scanTo );
                    updateWatermark( wm );
                    return wm;
                }
                int remaining = maxRecords - scanned;
                int thisPage = Math.min( effectivePage, remaining );
                Slice<GeoRecord> slice = resolveGeoBrowser().retrieveGeoRecords( query, pageStart, thisPage,
                        GeoRetrieveConfig.DETAILED );
                if ( slice == null || slice.isEmpty() ) {
                    break;
                }
                for ( GeoRecord r : slice ) {
                    scanned++;
                    if ( !isAllowedTaxon( r ) ) continue;
                    if ( !isExpressionProfiling( r ) ) continue;
                    List<String> matchedNames = new ArrayList<>( active.size() );
                    for ( GeoRecordMatcher m : active ) {
                        if ( m.evaluate( r ).isMatched() ) {
                            matchedNames.add( m.name() );
                        }
                    }
                    if ( matchedNames.isEmpty() ) continue;
                    matched++;
                    for ( String n : matchedNames ) {
                        matchedByCriterion.merge( n, 1, Integer::sum );
                    }
                    if ( !req.isDryRun() ) {
                        try {
                            String enriched = buildIdentifyingMetadata( r );
                            PreboardedExperiment pb = preboardedExperimentService.createPreboarded(
                                    r.getGeoAccession(),
                                    "GEO",
                                    enriched );
                            if ( pb != null && pb.getId() != null ) {
                                createdPreboardedIds.add( pb.getId() );
                            }
                            updateMatchedCriteria( r.getGeoAccession(), toJsonArray( matchedNames ) );
                        } catch ( PreboardedExperimentService.AccessionAlreadyExistsException ex ) {
                            // Already preboarded or loaded — skip; not an error condition during a scrape.
                            log.debug( "Skipping " + r.getGeoAccession() + " (already exists): " + ex.getMessage() );
                        }
                    }
                }
                pageStart += thisPage;
            }
            wm.setStatus( GeoScrapeWatermark.Status.COMPLETED );
            wm.setScanTo( scanTo );
            wm.setRecordsScanned( scanned );
            wm.setRecordsMatched( matched );
            updateWatermark( wm );
            if ( !req.isDryRun() && matched > 0 ) {
                openScrapeBatchTicket( wm, matchedByCriterion, createdPreboardedIds );
            }
        } catch ( IOException e ) {
            log.warn( "GEO scrape failed: " + e.getMessage() );
            wm.setStatus( GeoScrapeWatermark.Status.FAILED );
            wm.setErrorMessage( e.getClass().getSimpleName() + ": " + e.getMessage() );
            wm.setRecordsScanned( scanned );
            wm.setRecordsMatched( matched );
            updateWatermark( wm );
        } catch ( RuntimeException e ) {
            log.error( "GEO scrape failed unexpectedly", e );
            wm.setStatus( GeoScrapeWatermark.Status.FAILED );
            wm.setErrorMessage( e.getClass().getSimpleName() + ": " + e.getMessage() );
            wm.setRecordsScanned( scanned );
            wm.setRecordsMatched( matched );
            updateWatermark( wm );
            throw e;
        }
        return wm;
    }

    @Override
    public List<GeoScrapeDryRunCandidate> scrapeDryRun( ScrapeRequest req ) {
        if ( req == null ) req = new ScrapeRequest();
        List<GeoRecordMatcher> active = selectActive( req.getCriteria() );
        int maxRecords = req.getMaxRecords() != null ? req.getMaxRecords() : DEFAULT_MAX_RECORDS;

        List<GeoScrapeDryRunCandidate> out = new ArrayList<>();
        int scanned = 0;
        try {
            GeoQuery query = resolveGeoBrowser().searchGeoRecords(
                    GeoRecordType.SERIES, null, null,
                    ALLOWED_TAXA, null,
                    EXPRESSION_PROFILING_TYPES,
                    req.getSince(), req.getUntil() );
            int pageStart = 0;
            int effectivePage = Math.max( 1, pageSize );
            while ( scanned < maxRecords ) {
                if ( Thread.interrupted() ) {
                    break;
                }
                int remaining = maxRecords - scanned;
                int thisPage = Math.min( effectivePage, remaining );
                Slice<GeoRecord> slice = resolveGeoBrowser().retrieveGeoRecords( query, pageStart, thisPage,
                        GeoRetrieveConfig.DETAILED );
                if ( slice == null || slice.isEmpty() ) {
                    break;
                }
                for ( GeoRecord r : slice ) {
                    scanned++;
                    if ( !isAllowedTaxon( r ) ) continue;
                    if ( !isExpressionProfiling( r ) ) continue;
                    List<String> matchedNames = new ArrayList<>( active.size() );
                    for ( GeoRecordMatcher m : active ) {
                        if ( m.evaluate( r ).isMatched() ) {
                            matchedNames.add( m.name() );
                        }
                    }
                    if ( matchedNames.isEmpty() ) continue;
                    GeoScrapeDryRunCandidate c = new GeoScrapeDryRunCandidate();
                    c.preboardedId = null;
                    c.accession = r.getGeoAccession();
                    c.source = "GEO";
                    c.identifyingMetadata = buildIdentifyingMetadata( r );
                    c.state = "Preboarded";
                    c.enteredCurrentStateAt = null;
                    c.proposalCount = 0L;
                    c.latestProposal = null;
                    c.auditTrailUrl = null;
                    c.matchedCriteria = matchedNames;
                    out.add( c );
                }
                pageStart += thisPage;
            }
        } catch ( IOException e ) {
            log.warn( "GEO dry-run scrape failed: " + e.getMessage() );
            // Surface partial results; caller is interactive and can retry.
        }
        return out;
    }

    @Override
    @Nullable
    public GeoScrapeWatermark getLastWatermark() {
        return transactionTemplate.execute( status -> {
            @SuppressWarnings("unchecked")
            List<GeoScrapeWatermark> rows = sessionFactory.getCurrentSession()
                    .createQuery( "from GeoScrapeWatermark w order by w.scannedAt desc" )
                    .setMaxResults( 1 )
                    .list();
            return rows.isEmpty() ? null : rows.get( 0 );
        } );
    }

    @Nullable
    GeoScrapeWatermark getLastCompletedWatermark() {
        return transactionTemplate.execute( status -> {
            @SuppressWarnings("unchecked")
            List<GeoScrapeWatermark> rows = sessionFactory.getCurrentSession()
                    .createQuery( "from GeoScrapeWatermark w where w.status = :s order by w.scannedAt desc" )
                    .setParameter( "s", GeoScrapeWatermark.Status.COMPLETED )
                    .setMaxResults( 1 )
                    .list();
            return rows.isEmpty() ? null : rows.get( 0 );
        } );
    }

    GeoScrapeWatermark persistWatermark( GeoScrapeWatermark wm ) {
        return transactionTemplate.execute( status -> {
            sessionFactory.getCurrentSession().persist( wm );
            sessionFactory.getCurrentSession().flush();
            return wm;
        } );
    }

    void updateWatermark( GeoScrapeWatermark wm ) {
        wm.setScannedAt( new Date() );
        transactionTemplate.execute( new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult( TransactionStatus status ) {
                sessionFactory.getCurrentSession().merge( wm );
            }
        } );
    }

    void updateMatchedCriteria( String accession, String json ) {
        transactionTemplate.execute( new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult( TransactionStatus status ) {
                sessionFactory.getCurrentSession()
                        .createQuery( "update PreboardedExperiment p set p.matchedCriteria = :j where p.accession = :a" )
                        .setParameter( "j", json )
                        .setParameter( "a", accession )
                        .executeUpdate();
            }
        } );
    }

    private List<GeoRecordMatcher> selectActive( @Nullable Collection<String> requested ) {
        if ( requested == null || requested.isEmpty() ) {
            return matchers;
        }
        Set<String> want = new LinkedHashSet<>();
        for ( String s : requested ) {
            if ( s != null && !s.trim().isEmpty() ) {
                want.add( s.trim().toLowerCase( Locale.ROOT ) );
            }
        }
        if ( want.isEmpty() ) return matchers;
        List<GeoRecordMatcher> picked = new ArrayList<>();
        for ( GeoRecordMatcher m : matchers ) {
            if ( want.contains( m.name().toLowerCase( Locale.ROOT ) ) ) {
                picked.add( m );
            }
        }
        return picked;
    }

    private static boolean isAllowedTaxon( GeoRecord r ) {
        if ( r.getOrganisms() == null || r.getOrganisms().isEmpty() ) return false;
        for ( String o : r.getOrganisms() ) {
            if ( o != null && ALLOWED_TAXA.contains( o ) ) return true;
        }
        return false;
    }

    private static boolean isExpressionProfiling( GeoRecord r ) {
        String st = r.getSeriesType();
        if ( st == null || st.isEmpty() ) return false;
        for ( GeoSeriesType t : EXPRESSION_PROFILING_TYPES ) {
            if ( t.getIdentifier().equalsIgnoreCase( st ) ) return true;
        }
        // Fallback: prefix match — the SOFT identifier sometimes carries a
        // suffix on prod, and we'd rather over-include than drop legit profiles.
        return st.toLowerCase( Locale.ROOT ).startsWith( "expression profiling" );
    }

    private static String joinCriteria( List<GeoRecordMatcher> active ) {
        if ( active.isEmpty() ) return "";
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < active.size(); i++ ) {
            if ( i > 0 ) sb.append( ',' );
            sb.append( active.get( i ).name() );
        }
        return sb.toString();
    }

    private static String toJsonArray( List<String> names ) {
        StringBuilder sb = new StringBuilder( names.size() * 12 );
        sb.append( '[' );
        for ( int i = 0; i < names.size(); i++ ) {
            if ( i > 0 ) sb.append( ',' );
            sb.append( '"' ).append( names.get( i ).replace( "\"", "\\\"" ) ).append( '"' );
        }
        sb.append( ']' );
        return sb.toString();
    }

    /**
     * Serialize the curator-relevant fields of a {@link GeoRecord} into a
     * compact JSON object suitable for the
     * {@code PreboardedExperiment.identifyingMetadata} column.
     *
     * <p>Null / empty fields are omitted (Jackson is configured to write
     * dates as ISO-8601 strings). Returns {@code null} if the record
     * carries nothing worth recording, or if serialization fails (the
     * scrape continues — the matched-criteria column is the curator's
     * fallback signal).</p>
     */
    @Nullable
    String buildIdentifyingMetadata( GeoRecord r ) {
        if ( r == null ) return null;
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfNotBlank( payload, "geoAccession", r.getGeoAccession() );
        putIfNotBlank( payload, "title", r.getTitle() );
        putIfNotBlank( payload, "summary", r.getSummary() );
        putIfNotBlank( payload, "overallDesign", r.getOverallDesign() );
        if ( r.getOrganisms() != null && !r.getOrganisms().isEmpty() ) {
            payload.put( "organisms", new ArrayList<>( r.getOrganisms() ) );
        }
        putIfNotBlank( payload, "platform", r.getPlatform() );
        putIfNotBlank( payload, "seriesType", r.getSeriesType() );
        if ( r.getNumSamples() > 0 ) {
            payload.put( "numSamples", r.getNumSamples() );
        }
        if ( r.getReleaseDate() != null ) {
            SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd" );
            fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
            payload.put( "releaseDate", fmt.format( r.getReleaseDate() ) );
        }
        putIfNotBlank( payload, "libraryStrategy", r.getLibraryStrategy() );
        putIfNotBlank( payload, "librarySource", r.getLibrarySource() );
        putIfNotBlank( payload, "sampleDetails", r.getSampleDetails() );
        if ( r.getPubMedIds() != null && !r.getPubMedIds().isEmpty() ) {
            payload.put( "pubMedIds", new ArrayList<>( r.getPubMedIds() ) );
        }
        if ( r.getMeshHeadings() != null && !r.getMeshHeadings().isEmpty() ) {
            payload.put( "meshHeadings", new ArrayList<>( r.getMeshHeadings() ) );
        }
        SimpleDateFormat iso = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
        iso.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        payload.put( "scrapedAt", iso.format( new Date() ) );
        if ( payload.isEmpty() ) return null;
        try {
            return jsonMapper.writeValueAsString( payload );
        } catch ( JsonProcessingException e ) {
            log.warn( "Failed to serialize GeoRecord identifyingMetadata for "
                    + r.getGeoAccession() + ": " + e.getMessage() );
            return null;
        }
    }

    private static void putIfNotBlank( Map<String, Object> m, String key, @Nullable String value ) {
        if ( value != null && !value.trim().isEmpty() ) {
            m.put( key, value );
        }
    }

    /**
     * Open one ticket summarising a successful scrape batch. Target is the
     * {@link GeoScrapeWatermark} row so the curator queue can pivot from
     * "open work" back to "what scrape produced this".
     *
     * <p>The note lists per-criterion match counts plus the preboarded
     * ids, truncated at 20 ids with a trailing "(...and N more)" marker
     * to keep the payload bounded for very large batches.</p>
     */
    void openScrapeBatchTicket( GeoScrapeWatermark wm,
            Map<String, Integer> matchedByCriterion,
            List<Long> preboardedIds ) {
        User reporter;
        try {
            reporter = userManager.getCurrentUser();
        } catch ( RuntimeException e ) {
            log.warn( "No current user resolvable for scrape ticket; skipping ticket creation: " + e.getMessage() );
            return;
        }
        if ( reporter == null ) {
            log.warn( "No current user resolved for scrape ticket; skipping ticket creation." );
            return;
        }
        SimpleDateFormat dayFmt = new SimpleDateFormat( "yyyy-MM-dd" );
        dayFmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        String day = dayFmt.format( wm.getScannedAt() != null ? wm.getScannedAt() : new Date() );
        String title = "GEO scrape " + day + ": " + wm.getRecordsMatched() + " candidates";
        String note = buildTicketNote( matchedByCriterion, preboardedIds );
        TicketTarget target = TicketTarget.Factory.newInstance(
                TicketTargetType.GEO_SCRAPE_WATERMARK, wm.getId() );
        try {
            ticketService.openTicket( reporter, TicketType.GENERIC, title,
                    Collections.singleton( target ) );
            log.info( "Opened GEO scrape batch ticket for watermark " + wm.getId()
                    + " (" + wm.getRecordsMatched() + " matches, "
                    + preboardedIds.size() + " preboarded)." );
            if ( !note.isEmpty() ) {
                // ticketService.openTicket doesn't accept an opening payload — the OPEN event
                // carries no body. Comment is appended as a second event for the curator note.
                List<ubic.gemma.model.common.auditAndSecurity.curation.Ticket> open =
                        ticketService.findOpenForTarget( TicketTargetType.GEO_SCRAPE_WATERMARK, wm.getId() );
                if ( !open.isEmpty() ) {
                    ticketService.addComment( open.get( 0 ), reporter, note );
                }
            }
        } catch ( RuntimeException e ) {
            // Don't fail the scrape over a ticket-emission glitch; the watermark itself is the audit row.
            log.warn( "Failed to open GEO scrape batch ticket: " + e.getMessage(), e );
        }
    }

    static String buildTicketNote( Map<String, Integer> matchedByCriterion, List<Long> preboardedIds ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "matched: " );
        boolean first = true;
        for ( Map.Entry<String, Integer> e : matchedByCriterion.entrySet() ) {
            if ( e.getValue() == null || e.getValue() == 0 ) continue;
            if ( !first ) sb.append( ", " );
            sb.append( e.getKey() ).append( '×' ).append( e.getValue() );
            first = false;
        }
        if ( first ) {
            // no per-criterion counts (shouldn't happen — caller gates on matched>0) — drop the prefix
            sb.setLength( 0 );
        }
        if ( preboardedIds != null && !preboardedIds.isEmpty() ) {
            if ( sb.length() > 0 ) sb.append( "; " );
            sb.append( "preboarded ids: " );
            int cap = Math.min( 20, preboardedIds.size() );
            for ( int i = 0; i < cap; i++ ) {
                if ( i > 0 ) sb.append( ',' );
                sb.append( preboardedIds.get( i ) );
            }
            if ( preboardedIds.size() > cap ) {
                sb.append( " (...and " ).append( preboardedIds.size() - cap ).append( " more)" );
            }
        }
        return sb.toString();
    }
}
