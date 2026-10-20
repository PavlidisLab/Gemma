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

import org.springframework.lang.Nullable;

/**
 * Read + refresh the cached public home-page statistics snapshot.
 * <p>
 * Reads are anonymous-safe; the snapshot represents only what an anonymous user would see.
 * <p>
 * No method-level {@code @Secured} guards intentionally — the security boundary is the REST
 * surface ({@code POST /stats/home/refresh} carries
 * {@code @PreAuthorize("hasAuthority('GROUP_ADMIN')")}), and the {@code HomeStatsRefresher}
 * background thread invokes {@link #refresh()} without a SecurityContext (the startup
 * lifecycle event doesn't propagate one, and adding one would require an authenticate-as-
 * agent dance for the dev container). Instead, the impl sets up an anonymous SecurityContext
 * internally for the actual data reads, so the snapshot still represents only what an
 * anonymous user would see. Call-site discipline keeps this safe — only the REST endpoint
 * and the refresher itself call this.
 */
public interface HomeStatsService {

    /**
     * Return the most-recent snapshot, loading from disk on first access. Returns {@code null}
     * only if nothing has ever been generated AND the disk cache is missing — the REST layer
     * should handle that as a 503 rather than synchronously building the snapshot under request load.
     */
    @Nullable
    HomeStats getCached();

    /**
     * Recompute the snapshot from scratch (anonymous-user perspective), persist to disk, and
     * update the in-memory cache.
     */
    HomeStats refresh();
}
