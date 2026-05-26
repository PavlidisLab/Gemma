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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cached snapshot of public-home-page statistics. Recomputed daily by
 * {@link HomeStatsRefresher} from an anonymous-user perspective and served as-is by
 * {@code GET /stats/home} on the REST API.
 * <p>
 * Computing these on every request is too expensive — see legacy {@code WhatsNew}
 * for the precedent (file-cached weekly report read at every page load).
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeStats {

    /** When the snapshot was last refreshed (ISO-8601 in JSON via default Jackson). */
    private Date generatedAt;

    /** Total public expression experiments. */
    private long datasetCount;

    /** Total public platforms (array designs). */
    private long platformCount;

    /** Total distinct biomaterials across all public experiments. */
    private long sampleCount;

    /** Number of distinct genes in the database. Computed once per refresh; cheap. */
    private long geneCount;

    /** Per-taxon dataset counts, sorted descending by count. */
    private List<TaxonStat> byTaxon = new ArrayList<>();

    /**
     * Per-platform-technology-type dataset counts. Keys are {@code TechnologyType}
     * enum names (ONECOLOR / TWOCOLOR / DUALMODE / SEQUENCING / GENELIST / OTHER).
     * Frontend rolls these up into microarray (ONECOLOR+TWOCOLOR+DUALMODE) and
     * RNA-seq (SEQUENCING+GENELIST) buckets.
     */
    private Map<String, Long> byPlatformType = new LinkedHashMap<>();

    /** Number of public single-cell experiments (orthogonal to platform-technology type). */
    private long singleCellCount;

    /**
     * Total distinct ontology-backed annotation terms in use across all public datasets.
     * Free-text characteristics (no {@code valueUri}) are excluded — same semantics as
     * {@code GET /datasets/annotations/count?excludeFreeText=true}.
     */
    private long ontologyTermCount;

    /**
     * Per-category distinct-term counts. Keys are stable lowercase-snake-case strings:
     * {@code disease}, {@code organism_part}, {@code cell_type}, {@code treatment},
     * {@code strain}, {@code cell_line}. Each value is the count of distinct
     * ontology-backed terms in that category (free-text excluded). The {@code treatment}
     * bucket carries the drug-annotation count.
     */
    private Map<String, Long> byAnnotationCategory = new LinkedHashMap<>();

    /**
     * Distribution of annotation categories observed across public datasets — the top-N
     * categories Gemma actually uses, with the number of experiments each appears on.
     * Reflects the range of experimental conditions / annotation dimensions represented
     * in the corpus. Sorted descending by {@code numberOfExpressionExperiments}.
     */
    private List<CategoryStat> categoryDistribution = new ArrayList<>();

    /** Most-recently curated public experiments, for the scrolling-names widget. */
    private List<RecentExperiment> recentExperiments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class TaxonStat {
        private Long id;
        private String commonName;
        private String scientificName;
        private long count;

        public TaxonStat( Long id, String commonName, String scientificName, long count ) {
            this.id = id;
            this.commonName = commonName;
            this.scientificName = scientificName;
            this.count = count;
        }
    }

    @Data
    @NoArgsConstructor
    public static class CategoryStat {
        /** Stable lowercase-snake-case label used in {@code byAnnotationCategory} (e.g. {@code organism_part}),
         *  or {@code null} if this category isn't one we surface as a tile. */
        private String key;
        /** Gemma's canonical category label (e.g. {@code organism part}). */
        private String category;
        /** Ontology URI of the category, if any. */
        private String categoryUri;
        /** Number of public experiments carrying at least one annotation in this category. */
        private long numberOfExpressionExperiments;

        public CategoryStat( String key, String category, String categoryUri, long numberOfExpressionExperiments ) {
            this.key = key;
            this.category = category;
            this.categoryUri = categoryUri;
            this.numberOfExpressionExperiments = numberOfExpressionExperiments;
        }
    }

    @Data
    @NoArgsConstructor
    public static class RecentExperiment {
        private Long id;
        private String shortName;
        private String name;
        private String taxon;
        private Date lastUpdated;

        public RecentExperiment( Long id, String shortName, String name, String taxon, Date lastUpdated ) {
            this.id = id;
            this.shortName = shortName;
            this.name = name;
            this.taxon = taxon;
            this.lastUpdated = lastUpdated;
        }
    }
}
