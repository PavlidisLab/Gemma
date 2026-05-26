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

    /**
     * Distinct external accessions across all public EEs. {@code <= datasetCount} — the
     * difference is the EEs split off a parent submission ("1 GSE → 2 Gemma EEs"). Drives
     * the Datasets-tile sub-line "from N distinct accessions".
     */
    private long distinctAccessionCount;

    /** Total distinct biomaterials across all public experiments. */
    private long sampleCount;

    /** Number of distinct genes in the database. Computed once per refresh; cheap. */
    private long geneCount;

    /** Per-taxon dataset counts, sorted descending by count. */
    private List<TaxonStat> byTaxon = new ArrayList<>();

    /**
     * Per-external-database dataset counts — GEO, ArrayExpress, CELLxGENE, etc.,
     * plus a {@code "none"} bucket for datasets without an external accession
     * (direct lab submissions / Gemma-native). Sorted descending by count.
     */
    private List<AccessionSourceStat> datasetsByAccessionSource = new ArrayList<>();

    /**
     * Per-platform-technology-type dataset counts. Keys are {@code TechnologyType}
     * enum names (ONECOLOR / TWOCOLOR / DUALMODE / SEQUENCING / GENELIST / OTHER).
     * Frontend rolls these up into microarray (ONECOLOR+TWOCOLOR+DUALMODE) and
     * RNA-seq (SEQUENCING+GENELIST) buckets.
     */
    private Map<String, Long> byPlatformType = new LinkedHashMap<>();

    /** Number of public experiments that have at least one single-cell dimension recorded
     *  (orthogonal to platform-technology type — a single-cell experiment usually rides on
     *  a GENELIST platform). */
    private long singleCellCount;

    /** Total distinct differential-expression analysis result sets across public datasets.
     *  A DEA result set is the per-contrast unit of analysis output (e.g. "diseased vs
     *  control on factor 'disease state'"), so this number reflects the size of Gemma's
     *  DEA library — the corpus of comparisons callers can query / re-use. */
    private long deaResultSetCount;

    /** Distinct CHEBI-anchored drug / chemical annotations in use (characteristics whose
     *  {@code valueUri} starts with {@code http://purl.obolibrary.org/obo/CHEBI_}). Narrower
     *  than {@link #byAnnotationCategory}.{@code treatment} — that bucket includes non-drug
     *  treatments like radiation exposure or behavioural interventions. */
    private long drugCount;

    /** Distinct genes annotated as manipulation targets across the corpus (characteristics
     *  whose {@code valueUri} starts with {@link ubic.gemma.model.genome.Gene#NCBI_URI_PREFIX},
     *  typically under a genotype / genetic-perturbation category). Reflects how many distinct
     *  genes Gemma has perturbation data for (knockouts, knockdowns, overexpression, etc.). */
    private long geneManipulatedCount;

    /** Companion to {@link #geneManipulatedCount}: number of distinct experiments that carry
     *  at least one gene-URI annotation. {@link #geneManipulatedCount} counts the genes;
     *  this counts how many experiments have any gene perturbation at all. */
    private long geneManipulatedExperimentCount;

    /** Total individual cells measured across all single-cell experiments — sum of
     *  {@code BioAssay.numberOfCells} for assays attached to EEs with a SingleCellDimension.
     *  Typically reported in millions on the home page. */
    private long totalCells;

    /**
     * Sample (biomaterial) counts broken down by technology bucket — companion to
     * {@link #sampleCount}. Keys are stable lowercase-snake-case labels: {@code single_cell},
     * {@code rna_seq} (bulk RNA-seq only, single-cell excluded), {@code microarray}. Counts
     * distinct {@code ba.sampleUsed} biomaterials per bucket, not cells / sub-biomaterials.
     */
    private Map<String, Long> samplesByTech = new LinkedHashMap<>();

    /** Distinct factor-value count per ExperimentalFactor category. Reflects the range of
     *  experimental conditions Gemma has measured along each axis (e.g. how many distinct
     *  disease-state factor values exist across the corpus, how many genotypes, how many
     *  treatments). Keys are the canonical category labels carried on {@code ExperimentalFactor.category};
     *  sorted descending by value. */
    private List<FactorValueCategoryStat> factorValuesByCategory = new ArrayList<>();

    /**
     * Treatment-category terms broken down by bucket (see {@code treatment-buckets.json}):
     * {@code approved_drug}, {@code hormone}, {@code vitamin}, {@code toxin}, {@code vehicle},
     * {@code biologic} (PR / NCBI Gene), {@code pathogen} (NCBITaxon), {@code control},
     * plus the {@code other_chemical} catchall (unbucketed CHEBI) and {@code other} catchall
     * (non-CHEBI). Counts are EE-mention sums (Σ {@code numberOfExpressionExperiments} over
     * each bucket's matched terms), not distinct-URI counts — a single popular drug used in
     * 40 datasets contributes 40, not 1. Sorted descending by count.
     */
    private List<TreatmentBucketStat> treatmentSubcategories = new ArrayList<>();

    /**
     * Per-gene ranking of how many public EEs carry that gene as a perturbation-target
     * annotation. Top 25, sorted descending by {@code numberOfExpressionExperiments}.
     * Drives the home-page middle-column bar chart of most-studied perturbed genes.
     */
    private List<PerturbedGeneStat> topPerturbedGenes = new ArrayList<>();

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
    public static class AccessionSourceStat {
        /** External-database name (e.g. {@code GEO}, {@code ArrayExpress},
         *  {@code CELLxGENE}) or the literal {@code "none"} for datasets without
         *  an external accession. */
        private String source;
        /** Number of public expression experiments with this accession source. */
        private long count;

        public AccessionSourceStat( String source, long count ) {
            this.source = source;
            this.count = count;
        }
    }

    @Data
    @NoArgsConstructor
    public static class PerturbedGeneStat {
        /** Canonical Gemma gene symbol (e.g. {@code TP53}, {@code Trp53}, {@code MYC}). */
        private String geneSymbol;
        /** Lowercase common name of the gene's taxon, matching {@link TaxonStat#commonName}.
         *  May be {@code null} if the gene has no taxon recorded. Human and mouse orthologs
         *  surface as separate rows ({@code TP53 / human} vs {@code Trp53 / mouse}). */
        private String taxon;
        /** Distinct public experiments carrying this gene as a perturbation target. */
        private long numberOfExpressionExperiments;

        public PerturbedGeneStat( String geneSymbol, String taxon, long numberOfExpressionExperiments ) {
            this.geneSymbol = geneSymbol;
            this.taxon = taxon;
            this.numberOfExpressionExperiments = numberOfExpressionExperiments;
        }
    }

    @Data
    @NoArgsConstructor
    public static class TreatmentBucketStat {
        /** Stable lowercase-snake-case identifier the frontend matches in code — {@code drug},
         *  {@code pathogen}, {@code biologic}, {@code other}. */
        private String key;
        /** Visitor-facing string with the canonical phrasing — "Drugs / chemicals",
         *  "Pathogens", "Biologics", "Other". */
        private String label;
        /** Distinct ontology-backed terms in this bucket. */
        private long count;

        public TreatmentBucketStat( String key, String label, long count ) {
            this.key = key;
            this.label = label;
            this.count = count;
        }
    }

    @Data
    @NoArgsConstructor
    public static class FactorValueCategoryStat {
        /** Gemma's canonical category label carried on {@code ExperimentalFactor.category}
         *  (e.g. {@code disease state}, {@code treatment}, {@code genotype}).
         *  May be {@code null} for factors with no category set. */
        private String category;
        /** Ontology URI of the category, if any. */
        private String categoryUri;
        /** Distinct factor values existing under this category across the corpus. */
        private long numberOfDistinctFactorValues;

        public FactorValueCategoryStat( String category, String categoryUri, long numberOfDistinctFactorValues ) {
            this.category = category;
            this.categoryUri = categoryUri;
            this.numberOfDistinctFactorValues = numberOfDistinctFactorValues;
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
