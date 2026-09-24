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

    /** Distinct genes annotated as manipulation targets across the corpus — characteristics whose
     *  {@code valueUri} starts with {@link ubic.gemma.model.genome.Gene#NCBI_URI_PREFIX} AND whose
     *  own {@code categoryUri} is {@code genotype}. Reflects how many distinct genes Gemma has
     *  perturbation data for (knockouts, knockdowns, overexpression, etc.).
     *  <p>
     *  The category is enforced on the SAME characteristic, not on the experiment. Until
     *  2026-08-21 it was not enforced at all and the field counted a gene URI in any category,
     *  which swept in cytokines and growth factors administered under {@code treatment}. */
    private long geneManipulatedCount;

    /** Companion to {@link #geneManipulatedCount}: number of distinct experiments carrying at
     *  least one genotype-category gene-URI annotation. {@link #geneManipulatedCount} counts the
     *  genes; this counts how many experiments perturbed any gene at all. */
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
     * annotation — a gene URI whose own category is {@code genotype}. Top 25, sorted descending
     * by {@code numberOfExpressionExperiments}. Drives the home-page middle-column bar chart of
     * most-studied perturbed genes.
     * <p>
     * Scoped to genotype since 2026-08-21. Before that it counted the gene URI in any category,
     * so genes that are both perturbation targets and administered agents read high for the wrong
     * reason — TNF 72 against 39 real perturbations, TGFB1 62 against 31 — and four cytokines held
     * top-10 places that belong to Sox2, Mecp2, Pten and Apoe.
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

    /**
     * How many public experiments were added to Gemma over each of several trailing windows,
     * newest window first. "Added" is the {@code action='C'} audit row — when the dataset was
     * first loaded — NOT when it was made public; see {@link WhatsNewService} for why the
     * publication date is not reportable.
     * <p>
     * Several windows ship together because loading runs in bursts, so the useful window is not
     * fixed: on 2026-08-21 the trailing 7-, 30- and 90-day counts were all 0 (the most recent
     * load was 2026-05-12) while the 365-day count was 1,195. A caller that renders a
     * "recently added" figure should pick the shortest window whose count is non-zero and label
     * it with that window's {@link AddedInWindow#since}, rather than hard-coding "this week"
     * and rendering a permanent zero.
     */
    private List<AddedInWindow> datasetsAdded = new ArrayList<>();

    /** Count of public experiments created within one trailing window. */
    @Data
    @NoArgsConstructor
    public static class AddedInWindow {
        /** Width of the window in days (7, 30, 90, 365). */
        private int days;
        /** Resolved start of the window — {@code generatedAt - days}. Carried so a caller can
         *  label the figure ("1,195 added since 2025-08-21") without recomputing the date
         *  against a snapshot that may be up to a day old. */
        private Date since;
        /** Public experiments whose creation event falls on or after {@link #since}. */
        private long count;

        public AddedInWindow( int days, Date since, long count ) {
            this.days = days;
            this.since = since;
            this.count = count;
        }
    }

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
        /** Annotation-burden metric: number of public experiments carrying at least one
         *  annotation in this category. */
        private long numberOfExpressionExperiments;
        /** Annotation-diversity metric: number of distinct ontology-backed terms in use
         *  under this category (free-text excluded). Mirrors the value reported in the
         *  top-level {@code byAnnotationCategory} map; carried here too so callers don't
         *  have to cross-reference. {@code 0} if not pre-computed for this category. */
        private long numberOfDistinctTerms;

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
        /** Stable lowercase-snake-case identifier the frontend matches in code — e.g.
         *  {@code approved_drug}, {@code pathogen}, {@code biologic}, {@code control},
         *  {@code other_chemical}, {@code other}. */
        private String key;
        /** Visitor-facing label with the canonical phrasing. */
        private String label;
        /** Coarse UI grouping carried through from the bucket spec — {@code control},
         *  {@code pharmacology}, {@code biological}, {@code unclassified}. Lets the
         *  frontend cluster bars together (and visually separate the control-like
         *  buckets — control + vehicle — from pharmacology of interest). */
        private String group;
        /** Annotation-burden metric: Σ {@code numberOfExpressionExperiments} over each
         *  matched term — a popular drug used in 40 datasets contributes 40, not 1. */
        private long count;
        /** Annotation-diversity metric: distinct URIs that landed in this bucket. */
        private long termCount;
        /** Top representative terms in this bucket, sorted desc by EE-mention count.
         *  For the {@code other_chemical} / {@code other} catchalls this drives the
         *  iterative-bucketing loop: the curator looks at what's still drowning
         *  unclassified, adds those URIs to a more specific bucket, redeploys, and
         *  the head of the catchall list shrinks. */
        private List<TermStat> topTerms = new ArrayList<>();
        /** Nested partition. When this bucket has sub-buckets, every matched term is
         *  attributed to exactly one sub-bucket (or to an implicit {@code <key>_other}
         *  if no sub-bucket matches). Parent {@link #count} = Σ child {@code count}. */
        private List<TreatmentBucketStat> subBuckets = new ArrayList<>();

        public TreatmentBucketStat( String key, String label ) {
            this.key = key;
            this.label = label;
        }
    }

    /** A single annotation term with its EE-mention count — used inside
     *  {@link TreatmentBucketStat#topTerms} for the catchall-iteration loop. */
    @Data
    @NoArgsConstructor
    public static class TermStat {
        /** Ontology URI (CHEBI, OBI, EFO, …). */
        private String uri;
        /** Curator-visible label (the {@code Characteristic.value}). May fall back to
         *  the URI tail if no human label is attached. */
        private String label;
        /** Number of expression experiments carrying this term as a treatment annotation. */
        private long count;

        public TermStat( String uri, String label, long count ) {
            this.uri = uri;
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
        /** Diversity metric: distinct factor values existing under this category across the corpus. */
        private long numberOfDistinctFactorValues;
        /** Burden metric: distinct experiments carrying at least one factor under this
         *  category. A category used by 200 EEs each with 5 distinct genotype FVs would
         *  read 200 here and 1000 in {@link #numberOfDistinctFactorValues}. */
        private long numberOfExpressionExperiments;

        public FactorValueCategoryStat( String category, String categoryUri,
                                        long numberOfDistinctFactorValues,
                                        long numberOfExpressionExperiments ) {
            this.category = category;
            this.categoryUri = categoryUri;
            this.numberOfDistinctFactorValues = numberOfDistinctFactorValues;
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
