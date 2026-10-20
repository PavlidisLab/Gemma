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
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;

/**
 * Pluggable predicate over a {@link GeoRecord} for the GEO scrape &amp; preboard
 * pipeline. Each matcher decides whether the scrape should flag the record
 * for preboarding under its criterion.
 *
 * <p>v1 ships three cheesy keyword-based matchers ({@code brain},
 * {@code scbrain}, {@code tfperturb}); v2 will delegate to a curation-agent
 * pass + gene-set-fetch skill output instead of hand-rolled keyword lists.</p>
 *
 * @author phase 3 geo-scrape pipeline
 */
public interface GeoRecordMatcher {

    /**
     * Short, stable identifier — surfaces in REST request bodies (the
     * {@code criteria} selector), the watermark's {@code criteriaApplied}
     * column, and the preboarded's {@code matchedCriteria} JSON. Lowercase,
     * no whitespace (e.g. {@code "brain"}, {@code "tfperturb"}).
     */
    String name();

    /**
     * Decide whether this matcher flags the given record.
     */
    MatchResult evaluate( GeoRecord r );

    /**
     * Outcome of a single matcher's evaluation.
     */
    class MatchResult {
        private final boolean matched;
        @Nullable
        private final String reason;
        private final double confidence;

        public MatchResult( boolean matched, @Nullable String reason, double confidence ) {
            this.matched = matched;
            this.reason = reason;
            this.confidence = confidence;
        }

        public static MatchResult miss() {
            return new MatchResult( false, null, 0.0 );
        }

        public static MatchResult hit( String reason ) {
            return new MatchResult( true, reason, 1.0 );
        }

        public boolean isMatched() {
            return matched;
        }

        @Nullable
        public String getReason() {
            return reason;
        }

        /** Confidence in [0,1]; v1 keyword matchers return 1.0 on a hit. */
        public double getConfidence() {
            return confidence;
        }
    }
}
