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
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;

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
                homeStatsService.refresh();
            } catch ( Exception e ) {
                log.error( "HomeStats: startup refresh failed", e );
            }
        }, "HomeStats-startup-refresh" );
        t.setDaemon( true );
        t.start();
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
