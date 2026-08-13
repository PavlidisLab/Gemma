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
    DryRunResult scrapeDryRun( ScrapeRequest req );

    /**
     * Outcome of a dry run: the candidates, plus the two things a batching caller cannot work out
     * from the candidate list alone.
     */
    class DryRunResult {
        private final List<GeoScrapeDryRunCandidate> candidates;
        @Nullable
        private final String lastScannedAccession;
        @Nullable
        private final Date lastScannedDate;
        private final List<String> incompleteRecords;
        @Nullable
        private final Integer nextOffset;

        public DryRunResult( List<GeoScrapeDryRunCandidate> candidates, @Nullable String lastScannedAccession,
                @Nullable Date lastScannedDate, List<String> incompleteRecords, @Nullable Integer nextOffset ) {
            this.candidates = candidates;
            this.lastScannedAccession = lastScannedAccession;
            this.lastScannedDate = lastScannedDate;
            this.incompleteRecords = incompleteRecords;
            this.nextOffset = nextOffset;
        }

        public List<GeoScrapeDryRunCandidate> getCandidates() {
            return candidates;
        }

        /**
         * The last record the scan actually LOOKED at, matched or not.
         * <p>
         * A caller can only cursor on the oldest candidate, but {@code maxRecords} caps records
         * SCANNED and most scanned records match nothing — so when a batch's matches all sit near
         * the head, the next request re-scans the same span and returns nothing new. Measured by
         * the agents side over a 2026-06-01..2026-08-12 walk: 38 of 101 requests bought nothing,
         * each one a full synchronous scan against the 60-second proxy budget. Cursoring on this
         * instead of on the oldest candidate collapses those.
         * <p>
         * Null when the scan examined no records at all.
         */
        @Nullable
        public String getLastScannedAccession() {
            return lastScannedAccession;
        }

        /** Release date of {@link #getLastScannedAccession()}, so a caller can step `until` without a lookup. */
        @Nullable
        public Date getLastScannedDate() {
            return lastScannedDate;
        }

        /**
         * Accessions examined on degraded information: GEO served invalid MINiML, so the record was
         * kept with whatever the summary gave rather than failing the batch. Matchers that depend on
         * sample details may therefore have under-matched on these, so a caller can report its list
         * as incomplete and name the records — and retry them later, since the condition is usually
         * transient.
         */
        public List<String> getIncompleteRecords() {
            return incompleteRecords;
        }

        /**
         * Absolute offset into the resolved window where this scan stopped — feed straight back as
         * {@code skip} alongside the same {@code startAt} to continue at record level rather than
         * restarting that day. Null when nothing was scanned.
         */
        @Nullable
        public Integer getNextOffset() {
            return nextOffset;
        }
    }

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
        /** Upper bound of the scrape window (publication date). Null means "today". */
        @Nullable
        private Date until;
        /** Hard cap on the number of GEO records examined; null means use the service default. */
        @Nullable
        private Integer maxRecords;
        /** Subset of matcher names to apply; null/empty means "all available". */
        @Nullable
        private Collection<String> criteria;
        /** If true, evaluate matches but do not persist any PreboardedExperiment rows. */
        private boolean dryRun;
        /**
         * GEO series accession to resume from, e.g. {@code "GSE342847"} — the last record the
         * caller processed. Its release date becomes the upper bound of the window, so the scan
         * picks up where the previous batch stopped and walks backwards from there.
         * <p>
         * An accession, not an offset, on purpose: GEO returns newest-first, so a numeric offset
         * shifts every time a new series is published and a client paging by offset would skip
         * records. Matching on the accession's date is stable under new publications. Records
         * released the same day reappear — the overlap is intentional and cheaper than a gap.
         * <p>
         * An explicit {@link #until} wins over this. Unresolvable accessions are rejected rather
         * than ignored, since silently dropping the cursor rescans from the top.
         */
        @Nullable
        private String startAt;

        /**
         * Records to skip at the START of the resolved window, for record-level resumption.
         * <p>
         * {@link #startAt} resolves an accession to its RELEASE DATE and GEO's date filter is
         * day-granular, so resuming at X re-scans X's whole day. When that day holds more records
         * than {@code maxRecords}, the scan returns the same last-scanned record every call and the
         * walk spins; the only escape was stepping {@code until} back a day, which DISCARDS
         * whatever the scan never reached in that day. Measured by the agents side 2026-08-12: the
         * same window yielded 19 candidates at maxRecords=100 and 16 at maxRecords=10, a strict
         * subset — three records lost purely to the day-step.
         * <p>
         * Pair this with {@code startAt} — "resume at GSE-X, skipping the first N of its day" —
         * using the {@code nextOffset} the previous response returned. An offset alone would be
         * unstable, since GEO returns newest-first and every new publication shifts it; anchored to
         * the cursor's day it is stable, because the window is pinned by date first.
         */
        @Nullable
        private Integer skip;

        @Nullable
        public Integer getSkip() {
            return skip;
        }

        public void setSkip( @Nullable Integer skip ) {
            this.skip = skip;
        }

        @Nullable
        public String getStartAt() {
            return startAt;
        }

        public void setStartAt( @Nullable String startAt ) {
            this.startAt = startAt;
        }

        @Nullable
        public Date getSince() {
            return since;
        }

        public void setSince( @Nullable Date since ) {
            this.since = since;
        }

        @Nullable
        public Date getUntil() {
            return until;
        }

        public void setUntil( @Nullable Date until ) {
            this.until = until;
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
