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
package ubic.gemma.core.analysis.report;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.ontology.providers.OntologyService;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Refreshes {@link HomeStats} on a daily cron AND on every Spring context refresh
 * (startup). The two triggers use different Spring mechanisms on purpose:
 * <p>
 * <b>Startup pass</b> rides on {@link ContextRefreshedEvent} so it fires in every
 * server context — REST, dev, scheduler — regardless of whether
 * {@code @EnableScheduling} is active (it is skipped under the
 * {@link ubic.gemma.core.context.EnvironmentProfiles#CLI} profile, which has no
 * homepage to warm). {@code SchedulerConfig} is profile-gated to
 * {@link ubic.gemma.core.context.EnvironmentProfiles#SCHEDULER}, so a {@code @Scheduled}
 * method would never fire on a local-dev container. The lifecycle event always fires.
 * It runs the refresh in a background thread so Spring startup isn't blocked by the
 * minute-or-two cold-cache aggregation pass.
 * <p>
 * <b>Daily cron</b> stays on {@link Scheduled}. It only fires on nodes with the
 * scheduler profile active (which is what we want — daily refresh is a single-node
 * responsibility on the production scheduler). The cron default is 4 AM, after the
 * 3 AM Lucene reindex in {@code ScheduledSearchReindexer} so the two heavy nightly
 * passes don't race for the connection pool.
 * <p>
 * Multi-container concurrency is benign: {@link HomeStatsService#refresh()} writes
 * to disk atomically (temp file + ATOMIC_MOVE) and the recompute itself is read-only,
 * so two nodes racing the same tick at worst do the work twice.
 */
@Component
public class HomeStatsRefresher {

    private static final Log log = LogFactory.getLog( HomeStatsRefresher.class );

    @Autowired
    private HomeStatsService homeStatsService;

    @Autowired
    private Environment environment;

    /** Enabled ontology provider services, so the startup pass can wait for their background
     *  init threads to finish before computing the first snapshot. Optional: some contexts wire
     *  no ontologies at all, in which case the wait is skipped. */
    @Autowired(required = false)
    private List<OntologyService> ontologyServices;

    /**
     * Upper bound on how long the startup pass waits for background ontology initialization before
     * computing the first snapshot. Bounded so a disabled or stuck ontology can't stall the snapshot
     * indefinitely — on timeout we compute anyway (ontology-derived buckets degrade to the catch-alls
     * exactly as before, and the daily cron re-corrects them). Default 15 minutes.
     */
    @Value("${gemma.homeStats.ontologyWarmup.timeout:900000}")
    private long ontologyWarmupTimeoutMs;

    /** Guard against re-entry — multiple {@code ContextRefreshedEvent}s fire over a
     *  context's lifetime (each child context, refresh-by-actuator, etc.). Only the
     *  first matters for the startup pass. */
    private final AtomicBoolean startupRefreshArmed = new AtomicBoolean( true );

    /**
     * Recompute the snapshot on context startup. Always runs (no skip-if-cached) so a
     * fresh redeploy picks up new aggregation shape immediately — the on-disk snapshot
     * loaded by {@link HomeStatsServiceImpl#afterPropertiesSet()} is what
     * {@code GET /stats/home} serves until this background refresh completes; the
     * stale-from-previous-build data stays available rather than 503-ing, but it's
     * replaced as soon as the new compute finishes.
     * <p>
     * Runs in a background thread so the cold-cache aggregation pass (~15-25s on a
     * fresh DB cache) doesn't block Spring startup.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void refreshOnStartup() {
        // HomeStats warms the web homepage cache — there is no homepage to serve in a
        // CLI invocation, so skip it there. This also avoids a startup race: the
        // background compute runs ACL-filtered queries, which can beat
        // AclClassIdInitializer setting AclQueryUtils.sessionFactory and throw
        // "AclQueryUtils.sessionFactory not set". Web/scheduler contexts still refresh.
        if ( environment.acceptsProfiles( EnvironmentProfiles.CLI ) ) {
            log.debug( "HomeStats: skipping startup refresh under the CLI profile." );
            return;
        }
        if ( !startupRefreshArmed.compareAndSet( true, false ) ) {
            return;
        }
        log.info( "HomeStats: startup refresh — recomputing in background" );
        Thread t = new Thread( () -> {
            try {
                awaitOntologyWarmup();
                homeStatsService.refresh();
            } catch ( Exception e ) {
                log.error( "HomeStats: startup refresh failed", e );
            }
        }, "HomeStats-startup-refresh" );
        t.setDaemon( true );
        t.start();
    }

    /**
     * Block until every enabled ontology has finished its background initialization thread, or until
     * {@code gemma.homeStats.ontologyWarmup.timeout} elapses. HomeStats' treatment / drug buckets
     * expand CHEBI / OBI / PR subtrees via {@code OntologyService}; running the first snapshot before
     * those are loaded leaves the buckets undercounted (the "parent ... not loaded in OntologyService"
     * warnings) until the next daily refresh. Waiting here makes the startup snapshot correct. This
     * runs on the daemon startup thread, so Spring startup itself is never blocked.
     */
    private void awaitOntologyWarmup() {
        if ( ontologyServices == null || ontologyServices.isEmpty() ) {
            return;
        }
        long loading = countLoadingOntologies();
        if ( loading == 0 ) {
            return; // everything already warm — compute immediately
        }
        log.info( "HomeStats: waiting up to " + ( ontologyWarmupTimeoutMs / 1000 )
                + "s for " + loading + " ontology service(s) to finish loading before the startup snapshot" );
        long t0 = System.currentTimeMillis();
        long deadline = t0 + ontologyWarmupTimeoutMs;
        while ( System.currentTimeMillis() < deadline && countLoadingOntologies() > 0 ) {
            try {
                Thread.sleep( 2000 );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        long waited = ( System.currentTimeMillis() - t0 ) / 1000;
        long stillLoading = countLoadingOntologies();
        if ( stillLoading > 0 ) {
            log.warn( "HomeStats: proceeding with startup snapshot after " + waited + "s — " + stillLoading
                    + " ontology service(s) still loading; ontology-derived buckets may be undercounted "
                    + "until the daily refresh" );
        } else {
            log.info( "HomeStats: ontology services warmed in " + waited + "s; computing startup snapshot" );
        }
    }

    /** Count enabled ontology services whose background initialization thread is still running. */
    private long countLoadingOntologies() {
        long n = 0;
        for ( OntologyService o : ontologyServices ) {
            try {
                if ( o.isEnabled() && o.isInitializationThreadAlive() ) {
                    n++;
                }
            } catch ( RuntimeException ignored ) {
                // a bean that throws from isEnabled()/isInitializationThreadAlive() can't be waited on
            }
        }
        return n;
    }

    /**
     * Daily refresh. Default cron: 4 AM, after {@code ScheduledSearchReindexer}.
     * Only fires on nodes with {@code @EnableScheduling} active (the production
     * scheduler profile); local-dev containers rely on the startup pass above.
     */
    @Scheduled(cron = "${gemma.homeStats.refresh.cron:0 0 4 * * *}")
    public void refreshDaily() {
        log.info( "HomeStats: scheduled daily refresh starting" );
        try {
            homeStatsService.refresh();
        } catch ( Exception e ) {
            log.error( "HomeStats: scheduled refresh failed", e );
        }
    }
}
