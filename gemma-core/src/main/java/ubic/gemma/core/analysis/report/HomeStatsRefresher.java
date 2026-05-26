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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily {@code @Scheduled} task that recomputes {@link HomeStats}. Fires in every
 * Spring context (REST, CLI, dev, scheduler) — dropping the previous
 * {@code @Profile(EnvironmentProfiles.SCHEDULER)} gate so local-dev containers
 * populate the snapshot on startup without needing the scheduler profile active.
 * <p>
 * The startup pass runs once a couple of minutes after boot so a freshly-deployed
 * container doesn't wait until the next cron tick to populate the snapshot. The cron
 * default is 4 AM daily, after the 3 AM Lucene reindex in
 * {@code ScheduledSearchReindexer} so the two heavy nightly passes don't race for
 * the connection pool.
 * <p>
 * Multi-container concurrency is benign: {@link HomeStatsService#refresh()} writes
 * to disk atomically (temp file + ATOMIC_MOVE) and the recompute itself is read-only,
 * so two nodes racing the same cron tick at worst do the work twice.
 */
@Component
public class HomeStatsRefresher {

    private static final Log log = LogFactory.getLog( HomeStatsRefresher.class );

    @Autowired
    private HomeStatsService homeStatsService;

    /**
     * Initial population a couple of minutes after startup if the on-disk snapshot
     * is absent or stale. A late initialDelay keeps Spring context startup fast.
     */
    @Scheduled(initialDelay = 120_000L, fixedDelay = Long.MAX_VALUE)
    public void refreshOnStartup() {
        if ( homeStatsService.getCached() != null ) {
            log.info( "HomeStats: startup refresh skipped — disk snapshot already loaded" );
            return;
        }
        log.info( "HomeStats: startup refresh — no cached snapshot found" );
        try {
            homeStatsService.refresh();
        } catch ( Exception e ) {
            log.error( "HomeStats: startup refresh failed; will retry at next scheduled tick", e );
        }
    }

    /**
     * Daily refresh. Default cron: 4 AM, after {@code ScheduledSearchReindexer}.
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
