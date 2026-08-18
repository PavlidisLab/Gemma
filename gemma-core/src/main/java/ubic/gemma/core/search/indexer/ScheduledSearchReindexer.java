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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.hibernate.HibernateUtils;
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
 * last successful reindex, or whose index no longer holds one document per row.
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
 *   <li>Staleness is decided by two independent signals, either of which triggers a
 *       rebuild. The first is {@link AuditEventService#getUpdatedSinceDate(Class, Date)},
 *       which is only well-defined for {@link Auditable} entity classes. The two
 *       indexed roots that satisfy this AND are touched by curation are
 *       {@link ExpressionExperiment} and {@link ArrayDesign}. Non-auditable indexed
 *       roots (Gene, BioMaterial, CompositeSequence) are populated by bulk loaders
 *       and stay in the manual-reindex flow ({@code IndexGemmaCLI}).</li>
 *   <li>The second signal is {@link #describeIndexDrift(Class)}: Lucene document count
 *       against database row count. The audit check cannot see deletions — it selects
 *       live rows, and a deleted entity has neither a row nor an audit trail — so
 *       without this an index keeps serving documents for entities that no longer
 *       exist. See that method for the full rationale and its one blind spot.</li>
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
 *       only rebuild a class's index on days where its rows actually changed. The
 *       destructiveness is what makes a ghost document self-healing once anything
 *       triggers a rebuild — the purge takes the whole directory with it.</li>
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

    @Autowired
    private SessionFactory sessionFactory;

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
        int updated = stale != null ? stale.size() : 0;
        String drift = describeIndexDrift( clazz );
        if ( updated == 0 && drift == null ) {
            log.debug( "Scheduled reindex: no " + name + " rows updated since " + cutoff
                    + " and the index document count matches the database, skipping" );
            return;
        }
        String reason;
        if ( updated > 0 && drift != null ) {
            reason = updated + " rows updated since " + cutoff + ", and " + drift;
        } else if ( updated > 0 ) {
            reason = updated + " rows updated since " + cutoff;
        } else {
            reason = drift;
        }
        Date startedAt = new Date();
        log.info( "Scheduled reindex: " + name + " is stale (" + reason + "), rebuilding Lucene index" );
        indexerService.index( clazz );
        writeMarker( marker, startedAt );
        log.info( "Scheduled reindex: " + name + " index rebuilt, marker advanced to " + startedAt );
        if ( drift != null ) {
            String residual = describeIndexDrift( clazz );
            if ( residual != null ) {
                // The rebuild was supposed to reconcile the counts. If it didn't, the drift check
                // will fire again tomorrow and every night after, so say so loudly rather than
                // silently rebuilding forever: something is refusing to index (a row the
                // MassIndexer skipped, an entity whose bridge throws) and needs a human.
                log.warn( "Scheduled reindex: " + name + " still shows " + residual
                        + " immediately after a full rebuild. The nightly drift check will keep"
                        + " triggering rebuilds until this is resolved." );
            }
        }
    }

    /**
     * Second staleness signal, complementing the audit-event check: compare the number of Lucene
     * documents for {@code clazz} against the number of rows in the database.
     * <p>
     * Why this is needed: {@link AuditEventService#getUpdatedSinceDate} resolves to
     * {@code select adb from <Entity> adb join adb.auditTrail atr join atr.events ae where ...},
     * which selects <em>live entity rows</em>. A deleted entity has neither a row nor an audit
     * trail, so it can never appear in that result — deletions are structurally invisible to the
     * audit check. The index keeps serving documents for entities that no longer exist, and
     * because search post-filters resolve hits against the database (ACLs, entity loads) those
     * ghost documents surface as errors rather than as merely stale results. One such ghost
     * (ExpressionExperiment 91719) took down every dataset search that matched it until
     * {@code HibernateSearchSource} was taught to drop unresolvable hits.
     * <p>
     * A count comparison catches deletions with one cheap query per class and no schema change.
     * It also happens to catch <em>creations</em>, which the audit check misses for a different
     * reason (creation events carry {@code eventType is null} and are filtered out).
     * <p>
     * Known blind spot: equal numbers of insertions and deletions in the same window leave the
     * counts matching. That window closes on the next audit-detected update, since a rebuild is
     * destructive and wholesale.
     *
     * @return a human-readable description of the discrepancy, or {@code null} when the index
     * document count agrees with the database row count.
     */
    @Nullable
    // visible for testing
    String describeIndexDrift( Class<? extends Auditable> clazz ) {
        // openSession() rather than getCurrentSession(): the scheduled task runs outside any
        // request or transaction, mirroring IndexerServiceImpl and SearchIndexBootstrapper.
        try ( Session session = sessionFactory.openSession() ) {
            long rows;
            try {
                String entityName = HibernateUtils.getEntityName( sessionFactory, clazz );
                Long count = session.createQuery( "select count(*) from " + entityName, Long.class )
                        .uniqueResult();
                rows = count != null ? count : 0L;
            } catch ( RuntimeException e ) {
                // Without a row count there is nothing to compare against. Report no drift and
                // leave the audit check as the sole signal rather than forcing a rebuild on the
                // strength of a failed query.
                log.warn( "Scheduled reindex: could not count " + clazz.getSimpleName()
                        + " rows, skipping the index drift check", e );
                return null;
            }
            long docs;
            try {
                docs = Search.session( session )
                        .search( clazz )
                        .where( f -> f.matchAll() )
                        .fetchTotalHitCount();
            } catch ( RuntimeException e ) {
                // Same treatment SearchIndexBootstrapper gives an uncountable index: assume it is
                // missing or corrupt, which is a reason to rebuild.
                return "an uncountable Lucene index (likely missing or corrupt: " + e.getMessage() + ")";
            }
            if ( docs == rows ) {
                return null;
            }
            return String.format( "%d Lucene documents against %d database rows (%+d)", docs, rows, docs - rows );
        }
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
