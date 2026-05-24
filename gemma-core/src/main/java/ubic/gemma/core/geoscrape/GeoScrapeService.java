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

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.GeoScrapeWatermark;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Service surface for the GEO scrape & preboard pipeline.
 *
 * <p>The pipeline pulls recent GEO records via {@code GeoBrowser}, filters by
 * taxon + matcher criteria, and creates {@code PreboardedExperiment} rows for
 * curator inspection. Each invocation is logged as one
 * {@link GeoScrapeWatermark} row; the latest {@link GeoScrapeWatermark.Status#COMPLETED}
 * row's {@code scanTo} is the resume watermark.</p>
 *
 * @author phase 3 geo-scrape pipeline
 */
public interface GeoScrapeService {

    /**
     * Run a scrape. Synchronous; intended to be invoked from a
     * {@link ubic.gemma.core.tasks.maintenance.GeoScrapeTaskCommand} running on
     * the task-runner. Honors {@link Thread#interrupted()} between pages
     * — the watermark row is marked {@link GeoScrapeWatermark.Status#CANCELLED}
     * if the worker is interrupted mid-scan.
     *
     * @return the persisted watermark row covering this run (so the caller can
     *         summarize counts/timings).
     */
    GeoScrapeWatermark scrape( ScrapeRequest req );

    /**
     * Evaluate matchers against the same window {@link #scrape(ScrapeRequest)}
     * would inspect, but persist nothing — no watermark row, no
     * {@code PreboardedExperiment} row, no ticket. Intended for downstream
     * callers (currently gemma-curation-agents) that mock-persist the
     * results locally to evaluate curation methods without writing to prod
     * gemd. Wire shape per candidate mirrors the existing preboarded GET
     * response so the dry-run flag can flip off later without a shape
     * change.
     *
     * @return the candidate list in scan order; {@code dryRun} on the
     *         request is ignored (treated as {@code true} by contract).
     */
    List<GeoScrapeDryRunCandidate> scrapeDryRun( ScrapeRequest req );

    /**
     * @return the most recent {@link GeoScrapeWatermark} row, or {@code null}
     *         if no scrape has ever been run.
     */
    @Nullable
    GeoScrapeWatermark getLastWatermark();

    /**
     * Parameters for a single scrape run.
     */
    class ScrapeRequest {
        /** Lower bound of the scrape window. Null means "resume from the last successful scrape". */
        @Nullable
        private Date since;
        /** Hard cap on the number of GEO records examined; null means use the service default. */
        @Nullable
        private Integer maxRecords;
        /** Subset of matcher names to apply; null/empty means "all available". */
        @Nullable
        private Collection<String> criteria;
        /** If true, evaluate matches but do not persist any PreboardedExperiment rows. */
        private boolean dryRun;

        @Nullable
        public Date getSince() {
            return since;
        }

        public void setSince( @Nullable Date since ) {
            this.since = since;
        }

        @Nullable
        public Integer getMaxRecords() {
            return maxRecords;
        }

        public void setMaxRecords( @Nullable Integer maxRecords ) {
            this.maxRecords = maxRecords;
        }

        @Nullable
        public Collection<String> getCriteria() {
            return criteria;
        }

        public void setCriteria( @Nullable Collection<String> criteria ) {
            this.criteria = criteria;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun( boolean dryRun ) {
            this.dryRun = dryRun;
        }
    }
}
