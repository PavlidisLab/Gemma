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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Externalised bucket spec for {@code /stats/home treatmentSubcategories}.
 * <p>
 * Loaded from the classpath default at
 * {@code ubic/gemma/core/analysis/report/treatment-buckets.json}, optionally
 * overridden by {@code ${gemma.appdata.home}/HomeStats/treatment-buckets.json}
 * if present. Editing the spec without recompiling lets curators iterate on
 * bucket definitions (adding pharmacology classes, splitting CHEBI subtrees,
 * folding ncbi-gene biologics into the biologic bucket, etc.) without
 * touching Java.
 *
 * @see HomeStatsServiceImpl#computeTreatmentSubcategories
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TreatmentBucketsConfig {

    /** Ordered list of explicit buckets. First-match-wins when a term matches multiple. */
    private List<Bucket> buckets = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        /** Stable lowercase-snake-case identifier the frontend matches in code. */
        private String key;
        /** Visitor-facing label (canonical phrasing). */
        private String label;
        /** Coarse UI grouping. Lets the frontend cluster bars (and dim or visually
         *  separate the "control-like" buckets — control + vehicle — which are
         *  accurate annotations but not pharmacology of interest). Free string,
         *  recommended values: {@code control}, {@code pharmacology},
         *  {@code biological}, {@code unclassified}. Null = ungrouped. */
        private String group;
        /** Ontology parent URIs; expanded via {@code OntologyService.getChildren} into a
         *  full subClassOf descendant set. Empty / null if no subtree matching is desired. */
        private List<String> parentSubtreeUris = Collections.emptyList();
        /** {@code valueUri.startsWith(prefix)} matchers. Empty / null if no prefix matching. */
        private List<String> uriPrefixes = Collections.emptyList();
        /** Full-URI equality matchers. Empty / null if no exact matching. */
        private List<String> uriExactMatches = Collections.emptyList();
        /** Optional nested partition. When this bucket matches a term, the term is
         *  ALSO matched against the sub-buckets (first-match-wins among them) and
         *  attributed to exactly one sub-bucket — or to an implicit {@code <key>_other}
         *  sub-bucket if none match. Parent count = sum of sub-bucket counts. */
        private List<Bucket> subBuckets = Collections.emptyList();
    }
}
