/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.search.indexer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nightly {@code @Scheduled} task that runs Hibernate Search's MassIndexer for any
 * {@code @Indexed} entity class whose underlying rows have been touched since the
 * last successful reindex.
 * <p>
 * Why this exists: Gemma sets
 * {@code hibernate.search.indexing.listeners.enabled=false} (see {@code HibernateConfig}
 * line ~230) — write-through Lucene indexing is off so bulk loaders (GEO, MassIndexer
 * itself, etc.) do not pay the autoindex tax. The trade-off is that routine curation
 * mutations never propagate to the search index; users see "I just curated this EE
 * but it's not findable" for days-to-weeks until an operator runs
 * {@code IndexGemmaCLI}. {@code PERF_PROBE_SEARCH.md} Top Finding #2 (RED).
 * <p>
 * Design choices:
 * <ul>
 *   <li>Profile-gated on {@link EnvironmentProfiles#SCHEDULER} so the task only fires
 *       on the production node that runs scheduled jobs. CLI / REST / dev contexts
 *       don't activate {@code scheduler}, so this bean isn't even instantiated.</li>
 *   <li>Stale-check uses {@link AuditEventService#getUpdatedSinceDate(Class, Date)},
 *       which is only well-defined for {@link Auditable} entity classes. The two
 *       indexed roots that satisfy this AND are touched by curation are
 *       {@link ExpressionExperiment} and {@link ArrayDesign}. Non-auditable indexed
 *       roots (Gene, BioMaterial, CompositeSequence) are populated by bulk loaders
 *       and stay in the manual-reindex flow ({@code IndexGemmaCLI}).</li>
 *   <li>Last-successful-reindex timestamps live in
 *       {@code ${gemma.appdata.home}/search/last_reindex.&lt;EntityName&gt;.timestamp}.
 *       A filesystem marker is the minimum-cost path: no schema migration, no extra
 *       table, survives JVM restart. Per-class so a failed reindex of one entity
 *       doesn't block others.</li>
 *   <li>{@link IndexerService#index(Class)} sets {@code purgeAllOnStart(true)} — it is
 *       a destructive rebuild of the entity's Lucene directory. This is the only
 *       public surface; HS 7 does not expose a "reindex only changed rows" path
 *       without listener-driven autoindex, which the recce explicitly warns against
 *       (write amplification on bulk loads). The mitigation is the stale-check: we
 *       only rebuild a class's index on days where its rows actually changed.</li>
 *   <li>Exceptions are caught per class — a failure to reindex EE shouldn't prevent
 *       AD reindex from running. The timestamp marker for a failing class is left
 *       untouched so the next run retries.</li>
 * </ul>
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code gemma.search.reindex.cron} — Quartz/Spring cron expression (6-field).
 *       Default {@code 0 0 3 * * *} (3 AM daily).</li>
 *   <li>{@code gemma.appdata.home} — root for the marker file; {@code search/}
 *       sub-directory is created on demand.</li>
 * </ul>
 *
 * @author pavlidis-lab (PERF_PROBE_SEARCH Top Finding #2)
 */
@Component
@Profile(EnvironmentProfiles.SCHEDULER)
public class ScheduledSearchReindexer {

    private static final Log log = LogFactory.getLog( ScheduledSearchReindexer.class );

    /**
     * Indexed entity classes the nightly task tracks. Order is the rebuild order on a
     * day where multiple are stale. Both classes implement {@link Auditable}, which is
     * required for the {@link AuditEventService#getUpdatedSinceDate} stale check.
     */
    private static final Map<String, Class<? extends Auditable>> AUDITABLE_INDEXED_CLASSES = buildClassMap();

    private static Map<String, Class<? extends Auditable>> buildClassMap() {
        Map<String, Class<? extends Auditable>> m = new LinkedHashMap<>();
        m.put( ExpressionExperiment.class.getSimpleName(), ExpressionExperiment.class );
        m.put( ArrayDesign.class.getSimpleName(), ArrayDesign.class );
        return m;
    }

    @Autowired
    private IndexerService indexerService;

    @Autowired
    private AuditEventService auditEventService;

    /**
     * Root for the marker file. Falls through to {@code java.io.tmpdir}-derived default
     * via property resolution (see {@code default.properties}).
     */
    @Value("${gemma.appdata.home}")
    private String appdataHome;

    /**
     * Daily MassIndexer pass. Cron expression is the standard Spring 6-field format
     * (sec min hour day-of-month month day-of-week). Default is 3 AM daily — late
     * enough that bulk loaders typically aren't running, early enough that the index
     * is fresh by the start of the curation team's day.
     */
    @Scheduled(cron = "${gemma.search.reindex.cron:0 0 3 * * *}")
    public void reindexStale() {
        Path markerDir = Paths.get( appdataHome, "search" );
        try {
            Files.createDirectories( markerDir );
        } catch ( IOException e ) {
            log.error( "Scheduled reindex: could not create marker directory " + markerDir + ", aborting", e );
            return;
        }

        for ( Map.Entry<String, Class<? extends Auditable>> entry : AUDITABLE_INDEXED_CLASSES.entrySet() ) {
            String name = entry.getKey();
            Class<? extends Auditable> clazz = entry.getValue();
            Path marker = markerDir.resolve( "last_reindex." + name + ".timestamp" );
            try {
                reindexIfStale( clazz, name, marker );
            } catch ( Exception e ) {
                // Per-class isolation: one bad class shouldn't abort the rest. The marker
                // file is intentionally NOT written on failure so the next run retries.
                log.error( "Scheduled reindex of " + name + " failed; marker not advanced", e );
            }
        }
    }

    private void reindexIfStale( Class<? extends Auditable> clazz, String name, Path marker ) {
        Date cutoff = readMarker( marker );
        Collection<? extends Auditable> stale = auditEventService.getUpdatedSinceDate( clazz, cutoff );
        if ( stale == null || stale.isEmpty() ) {
            log.debug( "Scheduled reindex: no " + name + " rows updated since " + cutoff + ", skipping" );
            return;
        }
        Date startedAt = new Date();
        log.info( "Scheduled reindex: " + stale.size() + " " + name + " rows updated since " + cutoff + ", rebuilding Lucene index" );
        indexerService.index( clazz );
        writeMarker( marker, startedAt );
        log.info( "Scheduled reindex: " + name + " index rebuilt, marker advanced to " + startedAt );
    }

    /**
     * Read the last-successful-reindex timestamp. Missing / unreadable / unparseable
     * markers are treated as epoch zero, which forces a first-pass full reindex —
     * this is the correct fail-safe behaviour (better to over-index than to silently
     * skip an entity whose marker got truncated).
     */
    private Date readMarker( Path marker ) {
        if ( !Files.exists( marker ) ) {
            return new Date( 0L );
        }
        try {
            String contents = new String( Files.readAllBytes( marker ) ).trim();
            if ( contents.isEmpty() ) {
                return new Date( 0L );
            }
            return Date.from( Instant.parse( contents ) );
        } catch ( IOException | DateTimeParseException e ) {
            log.warn( "Scheduled reindex: marker " + marker + " unreadable, treating as epoch zero", e );
            return new Date( 0L );
        }
    }

    private void writeMarker( Path marker, Date timestamp ) {
        try {
            Files.write( marker, timestamp.toInstant().toString().getBytes() );
        } catch ( IOException e ) {
            // A reindex that succeeded but couldn't advance the marker is worse than a
            // reindex that failed outright — next run will rebuild again unnecessarily.
            // Log loud so ops notices.
            log.error( "Scheduled reindex: index rebuild succeeded but marker " + marker + " could not be written; next run will redundantly reindex", e );
        }
    }
}
