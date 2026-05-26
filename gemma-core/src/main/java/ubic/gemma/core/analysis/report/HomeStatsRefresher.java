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
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;

/**
 * Daily {@code @Scheduled} task that recomputes {@link HomeStats}. Profile-gated to
 * {@link EnvironmentProfiles#SCHEDULER} so only the production scheduler node fires
 * the recompute — REST / CLI / dev contexts read the cached snapshot but never refresh.
 * <p>
 * A startup pass runs once shortly after boot so a freshly-deployed scheduler node
 * doesn't wait until the next cron tick to populate the snapshot. The cron default
 * is 4 AM daily, after the 3 AM Lucene reindex in {@code ScheduledSearchReindexer} so
 * the two heavy nightly passes don't race for the connection pool.
 */
@Component
@Profile(EnvironmentProfiles.SCHEDULER)
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
