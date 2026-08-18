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
package ubic.gemma.cli.util;

import org.apache.commons.logging.Log;
import ubic.gemma.core.util.GemmaRestApiClient;

import java.util.Arrays;
import java.util.List;

/**
 * Tells the running gemma-rest to drop what a CLI rebuild has just made stale.
 *
 * <p>A rebuild command can evict the Hibernate query regions in its own JVM — and every one of them
 * does — but gemma-cli is a separate process. gemma-rest learns nothing from that eviction and goes
 * on serving the rows it cached before the rebuild ran. Twice this has looked exactly like the
 * rebuild having failed: {@code reference subject role} kept serving 1,967 deleted rows on
 * 2026-08-17, and on 2026-08-18 a search read zero relations for MEC-1 against a table that held
 * one, after the CLO rebuild had gone in correctly.</p>
 *
 * <p>Both regions, not just the first. {@code default-query-results-region} holds the cached rows;
 * {@code default-update-timestamps-region} is what Hibernate consults to decide whether a cached
 * query is still valid. Flushing the rows while leaving the timestamps can repopulate the cache from
 * a timestamp asserting the table has not moved.</p>
 *
 * <p>🛑 Failures are reported, never swallowed. The pre-existing {@code updateEe2c} ping discarded
 * its response and logged success unconditionally, so a refresh that 404'd or hit the wrong host was
 * indistinguishable from one that worked.</p>
 */
public final class RestCacheEviction {

    private RestCacheEviction() {
    }

    /** The regions a table rebuild leaves stale. */
    public static final List<String> AFTER_REBUILD_REGIONS = Arrays.asList(
            "default-query-results-region", "default-update-timestamps-region" );

    /**
     * Evict the after-rebuild regions on the configured gemma-rest host.
     *
     * <p>Never throws: a rebuild that wrote its rows correctly has done the important half, and
     * failing the command afterwards would misreport that. The log says plainly what did not happen
     * and what to run by hand.</p>
     *
     * @return true if every region was evicted
     */
    public static boolean evictAfterRebuild( GemmaRestApiClient client, Log log ) {
        if ( client == null ) {
            log.warn( "No REST client is configured; gemma-rest will keep serving its cached rows"
                    + " until they expire. Run scripts/evict_caches.py to flush them." );
            return false;
        }
        boolean allOk = true;
        for ( String region : AFTER_REBUILD_REGIONS ) {
            try {
                GemmaRestApiClient.Response response = client.delete( "/admin/caches/" + region );
                if ( response instanceof GemmaRestApiClient.ErrorResponse ) {
                    GemmaRestApiClient.ErrorResponse.Error error =
                            ( ( GemmaRestApiClient.ErrorResponse ) response ).getError();
                    log.warn( "Could not evict " + region + " on " + client.getHostUrl() + ": "
                            + error.getCode() + " " + error.getMessage() );
                    allOk = false;
                } else {
                    log.info( "Evicted " + region + " on " + client.getHostUrl() + "." );
                }
            } catch ( Exception e ) {
                // Includes the host being unreachable, which is the failure the old ping hid.
                log.warn( "Could not evict " + region + " on " + client.getHostUrl() + ": " + e.getMessage() );
                allOk = false;
            }
        }
        if ( !allOk ) {
            log.warn( "gemma-rest may keep serving pre-rebuild rows. Flush by hand with"
                    + " scripts/evict_caches.py, or DELETE /admin/caches/{region} for each of "
                    + AFTER_REBUILD_REGIONS + "." );
        }
        return allOk;
    }
}
