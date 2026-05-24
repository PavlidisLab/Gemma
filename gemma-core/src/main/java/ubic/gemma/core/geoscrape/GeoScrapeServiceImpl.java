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
import ubic.gemma.core.loader.expression.geo.service.GeoQuery;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;
import ubic.gemma.core.loader.expression.geo.service.GeoRetrieveConfig;
import ubic.gemma.model.expression.experiment.GeoScrapeWatermark;
import ubic.gemma.persistence.service.expression.experiment.PreboardedExperimentService;
import ubic.gemma.persistence.util.Slice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private final GeoBrowser geoBrowser;
    private final SessionFactory sessionFactory;
    private final PreboardedExperimentService preboardedExperimentService;
    private final List<GeoRecordMatcher> matchers;
    private final TransactionTemplate transactionTemplate;

    @Value("${gemma.geoScrape.pageSize:200}")
    private int pageSize = 200;

    @Autowired
    public GeoScrapeServiceImpl( GeoBrowser geoBrowser,
            SessionFactory sessionFactory,
            PreboardedExperimentService preboardedExperimentService,
            List<GeoRecordMatcher> matchers,
            PlatformTransactionManager transactionManager ) {
        this.geoBrowser = geoBrowser;
        this.sessionFactory = sessionFactory;
        this.preboardedExperimentService = preboardedExperimentService;
        this.matchers = matchers != null ? matchers : Collections.emptyList();
        this.transactionTemplate = new TransactionTemplate( transactionManager );
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
        try {
            GeoQuery query = geoBrowser.searchGeoRecords(
                    GeoRecordType.SERIES, null, null,
                    ALLOWED_TAXA, null,
                    EXPRESSION_PROFILING_TYPES );
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
                Slice<GeoRecord> slice = geoBrowser.retrieveGeoRecords( query, pageStart, thisPage,
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
                    if ( !req.isDryRun() ) {
                        try {
                            preboardedExperimentService.createPreboarded(
                                    r.getGeoAccession(),
                                    "GEO",
                                    null );
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
    private GeoScrapeWatermark getLastCompletedWatermark() {
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

    private GeoScrapeWatermark persistWatermark( GeoScrapeWatermark wm ) {
        return transactionTemplate.execute( status -> {
            sessionFactory.getCurrentSession().persist( wm );
            sessionFactory.getCurrentSession().flush();
            return wm;
        } );
    }

    private void updateWatermark( GeoScrapeWatermark wm ) {
        wm.setScannedAt( new Date() );
        transactionTemplate.execute( new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult( TransactionStatus status ) {
                sessionFactory.getCurrentSession().merge( wm );
            }
        } );
    }

    private void updateMatchedCriteria( String accession, String json ) {
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
}
