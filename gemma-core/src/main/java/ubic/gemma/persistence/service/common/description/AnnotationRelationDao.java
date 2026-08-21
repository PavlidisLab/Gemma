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
package ubic.gemma.persistence.service.common.description;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reads over {@link AnnotationRelation}.
 *
 * <p>Two read shapes, because there are two questions and conflating them is a design error:</p>
 *
 * <ul>
 * <li>{@link #findRelations} - the ranked, evidenced list. For showing a curator, and for anything
 * that has to <b>choose</b> a term. Ambiguity is fatal here: SURF1 carries three germline disease
 * axioms in MONDO, so a producer asked to emit one annotation has to pick, and picking wrong ships a
 * wrong annotation. That is why the curation rule forbids generating the tag.</li>
 * <li>{@link #findRelatedTerms} - set membership. For query expansion, and for asking "is this
 * proposed tag already implied by what the experiment carries?". Ambiguity is <b>harmless</b> here:
 * all three of SURF1's diseases go into the set, and testing membership against the set gives the
 * right answer whichever one is meant.</li>
 * </ul>
 *
 * <p>Many-to-many kills generation and is irrelevant to suppression. A caller that needs the second
 * and reaches for the first inherits a problem it does not have.</p>
 */
public interface AnnotationRelationDao extends BaseDao<AnnotationRelation> {

    /**
     * Which end of the relation a caller is seeding from.
     */
    enum Direction {
        /** Seed terms are subjects; return objects. "What does this genotype stand for?" */
        SUBJECT_TO_OBJECT,
        /** Seed terms are objects; return subjects. "What stands for this disease?" */
        OBJECT_TO_SUBJECT
    }

    /**
     * A relation aggregated over its attesting experiments, for one basis.
     *
     * <p>One summary per {@code (triple, basis)}, and deliberately <b>not</b> merged across bases.
     * Merging would require deciding that two bases naming different terms mean the same thing, and
     * they routinely do while naming different terms: MONDO's germline axiom for SURF1 points at
     * {@code MONDO:0700250} (mitochondrial complex IV deficiency, nuclear type 1) where the curator
     * wrote {@code MONDO:0009723} (Leigh syndrome). Those two share no xref, sit in different
     * branches, and neither subsumes the other - the ontology is modelling the molecular diagnosis and
     * the curator the clinical syndrome. Both are correct. A merge keyed on term identity would call
     * that a disagreement; a merge keyed on anything looser would invent an equivalence nobody
     * asserted. So the rows come back side by side and the caller sees both framings.</p>
     */
    class RelationSummary {

        private final String subjectValue;
        @Nullable
        private final String subjectValueUri;
        @Nullable
        private final String subjectCategory;
        @Nullable
        private final String subjectCategoryUri;
        @Nullable
        private final String predicate;
        @Nullable
        private final String predicateUri;
        private final String objectValue;
        @Nullable
        private final String objectValueUri;
        @Nullable
        private final String objectCategory;
        @Nullable
        private final String objectCategoryUri;
        @Nullable
        private final Long taxonId;
        @Nullable
        private final String taxonCommonName;
        @Nullable
        private final Integer taxonNcbiId;
        private final AnnotationRelationBasis basis;
        /**
         * Whether the source states this relation holds or states that it does not.
         *
         * <p>Sits beside {@link #basis} in the constructor deliberately: both are enums, so getting
         * the two the wrong way round is a compile error rather than a silent swap in a constructor
         * this long.</p>
         *
         * @see ubic.gemma.model.common.description.AnnotationRelationStatus
         */
        private final ubic.gemma.model.common.description.AnnotationRelationStatus status;
        @Nullable
        private final String source;
        @Nullable
        private final String sourceVersion;

        /**
         * Experiments attesting this relation that the caller may see. Zero for asserted bases
         * ({@link AnnotationRelationBasis#ONTOLOGY}, {@link AnnotationRelationBasis#EXTERNAL}), which
         * hold independently of anything Gemma stores - zero support there means "not counted", not
         * "no evidence", and a client that sorts on it without checking the basis buries the strongest
         * rows.
         */
        private final long numberOfExperiments;
        private final long numberOfExperimentsAtFactorValue;
        private final long numberOfExperimentsAtTag;
        private final long numberOfExperimentsAtBioMaterial;
        @Nullable
        private final Long exampleExperimentId;

        /**
         * Experiments carrying the subject value at all, within the taxon - the denominator behind
         * {@link #getSpecificity()}. Zero when not computed, which is the normal state for every basis
         * except {@link AnnotationRelationBasis#CORPUS}, where alone it means anything.
         */
        private final long numberOfExperimentsWithSubject;

        /**
         * How many distinct subjects this object stands in relation to, across the whole table.
         *
         * <p>🛑 <b>The discriminator for whether an object identifies anything.</b> Measured on the
         * corpus: {@code Homozygous negative} relates to 2,898 subjects, {@code Overexpression} to
         * 1,839, {@code Knockdown} to 1,346, {@code Heterozygous} to 473, {@code 10 uM} to 451,
         * {@code 24 h} to 448, and {@code induced pluripotent stem cell line cell} to 81 — while
         * {@code MPTP}, {@code 5xFAD} and {@code APP/PS1} sit in the low single digits. A gate seeded
         * with a high-breadth object implies every one of those subjects.</p>
         *
         * <p>This is <b>not</b> a quality judgement. A dose and a duration are perfectly good curated
         * statements; they are simply not identifying, and one number covers zygosity, perturbation
         * direction, dose, duration and generic ontology classes without anyone maintaining a list of
         * them. Reported so a client sets its own bar rather than inheriting ours.</p>
         */
        private final long objectBreadth;

        /**
         * Distinct objects this relation's SUBJECT relates to under the same predicate — the mirror of
         * {@link #objectBreadth}, and it answers the question at the other end of the arrow.
         *
         * <p>A generic class enumerates its members exactly the way a disease enumerates its models:
         * {@code induced pluripotent stem cell line cell} carries 17 rows of
         * {@code derives from cell line cell} naming 201B7, 585A1, Detroit 551, WT33 and the rest. Nothing
         * there is about the term a curator is looking at; the term is a heading for a list.</p>
         *
         * <p>Reported rather than filtered on, for the reason {@link #objectBreadth} is: uib was
         * inferring it from a page of results — more than three objects under one predicate, drop the
         * group — which is a guess about a global fact from a local sample, and wrong exactly when the
         * page happens to be short.</p>
         */
        private final long subjectBreadth;

        /**
         * The one-line basis the source gives, meant to be shown — {@code PMID:11242117} where MGI
         * cites a paper. Null for the many rows that legitimately have none: an ontology axiom cites
         * nothing, and a corpus co-occurrence is evidence rather than having any.
         */
        @Nullable
        private final String evidence;

        /**
         * 🛑 <b>Build one with {@link #builder()}, not with this.</b> Twenty-six positional arguments,
         * a dozen of them {@code long} and most of the rest {@code String}, is a constructor where a
         * transposition compiles and passes — and adding a field to it broke a stale test class three
         * times in one day, because Maven does not recompile a test whose source has not changed and
         * the mismatch therefore arrives at runtime as {@code NoSuchMethodError} rather than at
         * compile time.
         *
         * <p>With a builder an added field is additive: existing callers keep compiling AND keep
         * meaning what they meant, and the new field simply defaults.</p>
         */
        @lombok.Builder
        public RelationSummary( String subjectValue, @Nullable String subjectValueUri, @Nullable String subjectCategory,
                @Nullable String subjectCategoryUri, @Nullable String predicate, @Nullable String predicateUri,
                String objectValue, @Nullable String objectValueUri, @Nullable String objectCategory,
                @Nullable String objectCategoryUri, @Nullable Long taxonId, @Nullable String taxonCommonName,
                @Nullable Integer taxonNcbiId, AnnotationRelationBasis basis,
                ubic.gemma.model.common.description.AnnotationRelationStatus status, @Nullable String source,
                @Nullable String sourceVersion, long numberOfExperiments, long numberOfExperimentsAtFactorValue,
                long numberOfExperimentsAtTag, long numberOfExperimentsAtBioMaterial,
                @Nullable Long exampleExperimentId, long numberOfExperimentsWithSubject, long objectBreadth,
                long subjectBreadth, @Nullable String evidence ) {
            this.subjectValue = subjectValue;
            this.subjectValueUri = subjectValueUri;
            this.subjectCategory = subjectCategory;
            this.subjectCategoryUri = subjectCategoryUri;
            this.predicate = predicate;
            this.predicateUri = predicateUri;
            this.objectValue = objectValue;
            this.objectValueUri = objectValueUri;
            this.objectCategory = objectCategory;
            this.objectCategoryUri = objectCategoryUri;
            this.taxonId = taxonId;
            this.taxonCommonName = taxonCommonName;
            this.taxonNcbiId = taxonNcbiId;
            this.basis = basis;
            this.status = status;
            this.source = source;
            this.sourceVersion = sourceVersion;
            this.numberOfExperiments = numberOfExperiments;
            this.numberOfExperimentsAtFactorValue = numberOfExperimentsAtFactorValue;
            this.numberOfExperimentsAtTag = numberOfExperimentsAtTag;
            this.numberOfExperimentsAtBioMaterial = numberOfExperimentsAtBioMaterial;
            this.exampleExperimentId = exampleExperimentId;
            this.numberOfExperimentsWithSubject = numberOfExperimentsWithSubject;
            this.objectBreadth = objectBreadth;
            this.subjectBreadth = subjectBreadth;
            this.evidence = evidence;
        }

        /**
         * The fraction of the subject's experiments this object accounts for, or {@code 0} when there
         * is no denominator to divide by.
         *
         * <p>This is what separates a real relation from a background co-occurrence, and support on its
         * own cannot. {@code Abca4} null is annotated retinal degeneration nearly every time it
         * appears; {@code C57BL/6J} appears against obesity a handful of times out of many hundreds of
         * experiments, and against hundreds of other diseases besides. Ranking on raw support puts the
         * background strain on top of every disease in the corpus - the obesity is diet-induced, the
         * stroke surgical, the Burkitt lymphoma belongs to the cell line. The same measure demotes the
         * drug-versus-model confusion with no special case, since a drug tested against everything has
         * low specificity against any one thing.</p>
         */
        public double getSpecificity() {
            return numberOfExperimentsWithSubject > 0
                    ? ( double ) numberOfExperiments / ( double ) numberOfExperimentsWithSubject
                    : 0d;
        }

        /**
         * Ranking score, highest first: the basis rank dominates, and support x specificity orders
         * within a basis.
         *
         * <p>The basis dominating is the point - an assertion outranks any amount of co-occurrence. A
         * score alone is never enough to act on, which is why the basis, the counts and the denominator
         * all stay on the row for a client to apply its own bar to.</p>
         */
        public double getScore() {
            // Within a basis, support orders. This used to be a CONSTANT for any self-sufficient
            // basis, which made the score identical for every CURATED row and turned the sort into a
            // no-op -- results then fell through to the alphabetical tiebreakers, so a 2-dataset
            // relation could be served above a 10-dataset one and the strongest row arrived tenth.
            double within = basis.isSelfSufficient()
                    ? numberOfExperiments
                    : getSpecificity() * numberOfExperiments;
            // Bounded so it can NEVER cross a basis-rank gap. An assertion outranking an attestation
            // is the contract, and no amount of co-occurrence may overtake a curator's statement --
            // which a raw count could do, since support is bounded only by the size of the corpus.
            return basis.getRank() * 1000d + Math.min( within, 999d );
        }

        public String getSubjectValue() {
            return subjectValue;
        }

        @Nullable
        public String getSubjectValueUri() {
            return subjectValueUri;
        }

        @Nullable
        public String getSubjectCategory() {
            return subjectCategory;
        }

        @Nullable
        public String getSubjectCategoryUri() {
            return subjectCategoryUri;
        }

        @Nullable
        public String getPredicate() {
            return predicate;
        }

        @Nullable
        public String getPredicateUri() {
            return predicateUri;
        }

        public String getObjectValue() {
            return objectValue;
        }

        @Nullable
        public String getObjectValueUri() {
            return objectValueUri;
        }

        @Nullable
        public String getObjectCategory() {
            return objectCategory;
        }

        @Nullable
        public String getObjectCategoryUri() {
            return objectCategoryUri;
        }

        @Nullable
        public Long getTaxonId() {
            return taxonId;
        }

        @Nullable
        public String getTaxonCommonName() {
            return taxonCommonName;
        }

        @Nullable
        public Integer getTaxonNcbiId() {
            return taxonNcbiId;
        }

        public AnnotationRelationBasis getBasis() {
            return basis;
        }

        public ubic.gemma.model.common.description.AnnotationRelationStatus getStatus() {
            return status;
        }

        @Nullable
        public String getSource() {
            return source;
        }

        @Nullable
        public String getSourceVersion() {
            return sourceVersion;
        }

        public long getNumberOfExperiments() {
            return numberOfExperiments;
        }

        public long getNumberOfExperimentsAtFactorValue() {
            return numberOfExperimentsAtFactorValue;
        }

        public long getNumberOfExperimentsAtTag() {
            return numberOfExperimentsAtTag;
        }

        public long getNumberOfExperimentsAtBioMaterial() {
            return numberOfExperimentsAtBioMaterial;
        }

        @Nullable
        public Long getExampleExperimentId() {
            return exampleExperimentId;
        }

        public long getNumberOfExperimentsWithSubject() {
            return numberOfExperimentsWithSubject;
        }

        public long getObjectBreadth() {
            return objectBreadth;
        }

        /**
         * @see #subjectBreadth
         */
        public long getSubjectBreadth() {
            return subjectBreadth;
        }

        /**
         * @see #evidence
         */
        @Nullable
        public String getEvidence() {
            return evidence;
        }

        /**
         * Key identifying the triple irrespective of basis, so a caller can group the side-by-side rows
         * and see for itself where two bases do land on the same term.
         */
        /**
         * Whether this relation says something about the subject TERM, or records a parameter of one
         * experiment. See {@link ubic.gemma.model.common.description.RelationTopicality}.
         */
        public ubic.gemma.model.common.description.RelationTopicality getTopicality() {
            return ubic.gemma.model.common.description.RelationTopicality
                    .of( predicateUri, subjectCategoryUri, subjectValueUri );
        }

        /**
         * Which end of this relation licenses an inference about the other. A relation is readable
         * from both ends and inferable from only one — see
         * {@link ubic.gemma.model.common.description.RelationInferenceDirection}.
         */
        public ubic.gemma.model.common.description.RelationInferenceDirection getInferenceDirection() {
            return ubic.gemma.model.common.description.RelationInferenceDirection
                    .of( predicateUri, subjectCategoryUri, subjectValueUri, objectCategoryUri );
        }

        /**
         * NCBI taxon id for human, which decides whether an inferred disease claim reads as
         * {@code has disease} or as {@code has role in modeling}.
         */
        private static final int HUMAN_NCBI_TAXON_ID = 9606;

        /**
         * RO_0003301. The constant name keeps the colloquial "is model of" because that is what
         * everyone here calls the relation; the VALUE is RO's own {@code rdfs:label}, which is what
         * {@code Relation.terms.txt} sanctions and therefore what the commit validator compares
         * against. Do not "fix" the value back to the readable spelling — that is a 400.
         */
        private static final String IS_MODEL_OF_URI = "http://purl.obolibrary.org/obo/RO_0003301";
        private static final String HAS_DISEASE_URI = "http://purl.obolibrary.org/obo/RO_0016002";

        /**
         * The claim this relation licenses, as a triple in its own right — <b>not</b> the stored row
         * read backwards.
         *
         * <p>Gemma stores {@code Alzheimer disease --has_genotype--> APP/PS1}, but the fact that
         * follows from it is "APP/PS1 is a disease model of Alzheimer's": different ends, and a
         * different verb. A consumer handed only the stored row has to invert it and choose a verb
         * itself, and three consumers will choose three.</p>
         *
         * <p><b>Taxon picks the verb</b>, which is the rule the design settled on and the reason
         * {@code RO_0003301 has role in modeling} was added to the vocabulary. A mouse carrying {@code APP/PS1}
         * <i>models</i> Alzheimer disease; a human line carrying {@code LRRK2 G2019S} is not modelling
         * Parkinson disease, it <i>has</i> it. Unknown taxon falls to {@code has role in modeling}, the weaker
         * of the two claims, rather than being guessed at.</p>
         *
         * <p>Null when {@link #getInferenceDirection()} is
         * {@link ubic.gemma.model.common.description.RelationInferenceDirection#NEITHER} — nothing is
         * implied, so there is no claim to phrase.</p>
         */
        @Nullable
        public String getImpliedSubject() {
            switch ( getInferenceDirection() ) {
                case OBJECT_IMPLIES_SUBJECT:
                    return objectValue;
                case SUBJECT_IMPLIES_OBJECT:
                    return subjectValue;
                default:
                    return null;
            }
        }

        @Nullable
        public String getImpliedSubjectUri() {
            switch ( getInferenceDirection() ) {
                case OBJECT_IMPLIES_SUBJECT:
                    return objectValueUri;
                case SUBJECT_IMPLIES_OBJECT:
                    return subjectValueUri;
                default:
                    return null;
            }
        }

        @Nullable
        public String getImpliedObject() {
            switch ( getInferenceDirection() ) {
                case OBJECT_IMPLIES_SUBJECT:
                    return subjectValue;
                case SUBJECT_IMPLIES_OBJECT:
                    return objectValue;
                default:
                    return null;
            }
        }

        @Nullable
        public String getImpliedObjectUri() {
            switch ( getInferenceDirection() ) {
                case OBJECT_IMPLIES_SUBJECT:
                    return subjectValueUri;
                case SUBJECT_IMPLIES_OBJECT:
                    return objectValueUri;
                default:
                    return null;
            }
        }

        /**
         * @see #getImpliedSubject()
         */
        @Nullable
        public String getImpliedPredicateUri() {
            switch ( getInferenceDirection() ) {
                case OBJECT_IMPLIES_SUBJECT:
                    // the genotype end implies a disease claim and taxon says which one -- but only
                    // where the implied subject is the sort of thing that could bear the disease. An
                    // inducer never is: see RelationInferenceDirection.impliesAnInducer.
                    return !ubic.gemma.model.common.description.RelationInferenceDirection
                            .impliesAnInducer( predicateUri )
                            && taxonNcbiId != null && taxonNcbiId == HUMAN_NCBI_TAXON_ID
                            ? HAS_DISEASE_URI
                            : IS_MODEL_OF_URI;
                case SUBJECT_IMPLIES_OBJECT:
                    // already oriented specific-to-general, and the curator's own verb reads correctly:
                    // "MCF7 derives from a patient having adenocarcinoma" needs no rewriting
                    return predicateUri;
                default:
                    return null;
            }
        }

        @Nullable
        public String getImpliedPredicate() {
            switch ( getInferenceDirection() ) {
                case OBJECT_IMPLIES_SUBJECT:
                    return !ubic.gemma.model.common.description.RelationInferenceDirection
                            .impliesAnInducer( predicateUri )
                            && taxonNcbiId != null && taxonNcbiId == HUMAN_NCBI_TAXON_ID
                            ? "has disease"
                            : "has role in modeling";
                case SUBJECT_IMPLIES_OBJECT:
                    return predicate;
                default:
                    return null;
            }
        }

        /**
         * Identity of the CLAIM this row licenses, as opposed to {@link #getTripleKey()}, which is the
         * identity of the row as stored. Null when the row licenses nothing.
         *
         * <p>🛑 <b>Two different stored relations routinely derive one claim, and {@code tripleKey}
         * cannot see it.</b> uib measured it on the {@code BRCA1} card, 2026-08-18:</p>
         *
         * <pre>
         * BRCA1         --has disease-->  breast cancer   tripleKey  …/672 RO_0016002 …/MONDO_0007254
         * breast cancer --has_genotype--> BRCA1           tripleKey  …/MONDO_0007254 GENO_0000222 …/672
         * </pre>
         *
         * <p>Two keys, one claim — {@code BRCA1 — has disease → breast cancer} — rendered twice, word
         * for word. {@code tripleKey} groups the side-by-side rows ONE relation produces across bases,
         * which is a different shape from two relations converging on one claim, so a consumer reaching
         * for it to deduplicate a card gets a duplicate anyway. Dedup belongs at the level the reader
         * actually sees, which is the claim.</p>
         *
         * <p>It correctly keeps apart claims whose verb differs: the same genotype relation on a mouse
         * implies {@code has role in modeling} and on a human line {@code has disease}, and those are two claims
         * rather than one rendered twice.</p>
         */
        @Nullable
        public String getImpliedTripleKey() {
            String subject = getImpliedSubjectUri() != null ? getImpliedSubjectUri() : getImpliedSubject();
            String object = getImpliedObjectUri() != null ? getImpliedObjectUri() : getImpliedObject();
            if ( subject == null || object == null ) {
                return null;
            }
            return subject + " " + ( getImpliedPredicateUri() != null ? getImpliedPredicateUri() : "" )
                    + " " + object;
        }

        public String getTripleKey() {
            return ( subjectValueUri != null ? subjectValueUri : subjectValue )
                    + " " + ( predicateUri != null ? predicateUri : "" )
                    + " " + ( objectValueUri != null ? objectValueUri : objectValue );
        }
    }

    /**
     * What to look for. Every field narrows; an empty collection means "do not constrain on this".
     *
     * <p>At least one of the subject or object legs must be populated. Enumerating the whole relation
     * table is not a question anyone is asking, and would be an expensive way to find that out.</p>
     */
    class RelationQuery {

        private Collection<String> subjectValueUris = Collections.emptySet();
        private Collection<String> subjectValues = Collections.emptySet();
        private Collection<String> objectValueUris = Collections.emptySet();
        private Collection<String> objectValues = Collections.emptySet();
        private Collection<String> predicateUris = Collections.emptySet();
        private Collection<String> subjectCategoryUris = Collections.emptySet();
        private Collection<String> objectCategoryUris = Collections.emptySet();
        private Set<AnnotationRelationBasis> bases = EnumSet.allOf( AnnotationRelationBasis.class );
        private Collection<Long> excludedExperimentIds = Collections.emptySet();
        @Nullable
        private Long taxonId;
        @Nullable
        private Long seedFromExperimentId;
        private Direction seedDirection = Direction.SUBJECT_TO_OBJECT;
        private int minimumSupport = 0;
        private int maximumObjectBreadth = 0;
        private boolean termLevelOnly = true;
        private boolean includeRefuted = false;
        private double minimumSpecificity = 0d;
        private int maxResults = 50;

        public Collection<String> getSubjectValueUris() {
            return subjectValueUris;
        }

        public RelationQuery subjectValueUris( Collection<String> v ) {
            this.subjectValueUris = v;
            return this;
        }

        public Collection<String> getSubjectValues() {
            return subjectValues;
        }

        public RelationQuery subjectValues( Collection<String> v ) {
            this.subjectValues = v;
            return this;
        }

        public Collection<String> getObjectValueUris() {
            return objectValueUris;
        }

        public RelationQuery objectValueUris( Collection<String> v ) {
            this.objectValueUris = v;
            return this;
        }

        public Collection<String> getObjectValues() {
            return objectValues;
        }

        public RelationQuery objectValues( Collection<String> v ) {
            this.objectValues = v;
            return this;
        }

        public Collection<String> getPredicateUris() {
            return predicateUris;
        }

        public RelationQuery predicateUris( Collection<String> v ) {
            this.predicateUris = v;
            return this;
        }

        public Collection<String> getSubjectCategoryUris() {
            return subjectCategoryUris;
        }

        public RelationQuery subjectCategoryUris( Collection<String> v ) {
            this.subjectCategoryUris = v;
            return this;
        }

        public Collection<String> getObjectCategoryUris() {
            return objectCategoryUris;
        }

        public RelationQuery objectCategoryUris( Collection<String> v ) {
            this.objectCategoryUris = v;
            return this;
        }

        public Set<AnnotationRelationBasis> getBases() {
            return bases;
        }

        public RelationQuery bases( Set<AnnotationRelationBasis> v ) {
            this.bases = v;
            return this;
        }

        public Collection<Long> getExcludedExperimentIds() {
            return excludedExperimentIds;
        }

        /**
         * Hold experiments out of the evidence.
         *
         * <p>This is what makes "the tag on this dataset is inferable, so it can be dropped" an honest
         * claim rather than a circular one: exclude the dataset and ask whether the <i>rest</i> of the
         * corpus still recovers the relation. Without it, a dataset is shown its own annotation as
         * independent support for itself.</p>
         *
         * <p><b>Expected to be long, and it has to be done here.</b> A scored evaluation holds out its
         * whole panel, not one dataset, because precedent drawn from the other panel experiments is
         * leakage into the same benchmark. A caller cannot do this filtering for itself afterwards:
         * what comes back is an aggregate count, not the list of datasets behind it, so a client
         * holding a relation with support 5 has no way to know which five contributed and no way to
         * subtract its own. The list is sorted and deduped before binding so a long hold-out reuses one
         * prepared statement rather than minting a plan per distinct length.</p>
         */
        public RelationQuery excludedExperimentIds( Collection<Long> v ) {
            this.excludedExperimentIds = v;
            return this;
        }

        @Nullable
        public Long getTaxonId() {
            return taxonId;
        }

        public RelationQuery taxonId( @Nullable Long v ) {
            this.taxonId = v;
            return this;
        }

        @Nullable
        public Long getSeedFromExperimentId() {
            return seedFromExperimentId;
        }

        /**
         * Seed from every annotation an experiment carries, instead of from a list of terms.
         *
         * <p>Matched inside the query as an {@code exists} against EE2C rather than by fetching the
         * experiment's annotations into Java first. A round trip per experiment page is exactly the
         * kind of cost that makes an endpoint slower the more it is used, and the seed set is already
         * indexed where it sits.</p>
         *
         * <p>Pass the same id to {@link #excludedExperimentIds(Collection)} unless you specifically
         * want the experiment counted as evidence for itself.</p>
         */
        public RelationQuery seedFromExperimentId( @Nullable Long v ) {
            this.seedFromExperimentId = v;
            return this;
        }

        public Direction getSeedDirection() {
            return seedDirection;
        }

        /**
         * Which side of the relation an experiment seed is matched against.
         *
         * <p>Both directions are real and which one a caller wants depends on what it holds. A curated
         * statement puts the <b>disease in the subject</b> and the gene in the object
         * ({@code disease model: autism spectrum disorder - has_genotype -> Mef2c}), so an experiment
         * carrying a genotype matches on the object side and the disease is what comes back;
         * an experiment carrying the disease matches on the subject side and the genotype comes
         * back.</p>
         */
        public RelationQuery seedDirection( Direction v ) {
            this.seedDirection = v;
            return this;
        }

        public int getMinimumSupport() {
            return minimumSupport;
        }

        public RelationQuery minimumSupport( int v ) {
            this.minimumSupport = v;
            return this;
        }

        public double getMinimumSpecificity() {
            return minimumSpecificity;
        }

        /**
         * Off by default, deliberately: no threshold has been tuned against curator judgement, and the
         * shape of the distribution is worth seeing before one is fixed in the API.
         */
        public RelationQuery minimumSpecificity( double v ) {
            this.minimumSpecificity = v;
            return this;
        }

        public int getMaximumObjectBreadth() {
            return maximumObjectBreadth;
        }

        /**
         * Drop relations whose object relates to more than this many distinct subjects. Zero, the
         * default, does not filter.
         *
         * <p>No default is imposed because the right bar depends on the question. A suppression gate
         * wants something small — an object shared by hundreds of diseases implies all of them and is
         * useless for deciding whether one of them is redundant. A curator browsing what a dataset's
         * annotations relate to may well want the dose and the duration.</p>
         *
         * @see RelationSummary#getObjectBreadth()
         */
        public RelationQuery maximumObjectBreadth( int v ) {
            this.maximumObjectBreadth = v;
            return this;
        }

        public boolean isIncludeRefuted() {
            return includeRefuted;
        }

        /**
         * Also return relations a source states do NOT hold. <b>Off by default.</b>
         *
         * <p>🛑 A refuted row is not a weak assertion, it is the opposite of one, so it must never
         * arrive where a caller expects support. Asking for it is asking a different question — "does
         * anything actively deny this?" — and only a caller posing that question should see one.</p>
         */
        public RelationQuery includeRefuted( boolean v ) {
            this.includeRefuted = v;
            return this;
        }

        public boolean isTermLevelOnly() {
            return termLevelOnly;
        }

        /**
         * Keep only relations that say something about the subject term, dropping per-experiment
         * parameters. <b>On by default</b>, which is a change of behaviour made deliberately: the
         * bookkeeping is roughly four rows in five, none of it is what a reader of a term wants, and
         * every consumer was about to write its own list of predicates to exclude.
         *
         * <p>Set false to get everything the harvest stored. Nothing is dropped from the table.</p>
         */
        public RelationQuery termLevelOnly( boolean v ) {
            this.termLevelOnly = v;
            return this;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public RelationQuery maxResults( int v ) {
            this.maxResults = v;
            return this;
        }

        public boolean isSeeded() {
            return seedFromExperimentId != null
                    || !subjectValueUris.isEmpty() || !subjectValues.isEmpty()
                    || !objectValueUris.isEmpty() || !objectValues.isEmpty();
        }
    }

    /**
     * The ranked, evidenced read. Empty when the query names neither end of the relation.
     *
     * <p>Support is counted here, never stored, so the counts are exact for the calling user's ACL.</p>
     */
    List<RelationSummary> findRelations( RelationQuery query );

    /**
     * Set membership: every term related to any of the seeds, with no counting, no ranking and no
     * ambiguity to resolve.
     *
     * <p>This is the primitive behind query expansion and behind "is this tag already implied?". It is
     * one indexed lookup and has to stay cheap enough to sit on an interactive path.</p>
     *
     * @param seedValueUris grounded seeds; may be empty if {@code seedValues} is not
     * @param seedValues    ungrounded seeds, matched on the value string - ordinary here, since plenty
     *                      of curated statement objects are free text ({@code aortic banding} has none)
     * @return the related terms, each as {@code [value, valueUri]}, the URI possibly null
     */
    List<String[]> findRelatedTerms( Collection<String> seedValueUris, Collection<String> seedValues,
            Direction direction, Set<AnnotationRelationBasis> bases, Collection<String> predicateUris,
            @Nullable Long taxonId, Collection<Long> excludedExperimentIds, int maximumObjectBreadth,
            int maxResults );

    /**
     * Remove every derived row for a basis, optionally for one experiment.
     *
     * <p>Rebuild, not upsert - see {@link AnnotationRelation}. An upsert can only correct rows the new
     * query still produces, so a row whose source annotation was deleted would outlive it.</p>
     */
    int removeByBasis( AnnotationRelationBasis basis, @Nullable Long experimentId );

    /**
     * The same rebuild delete, narrowed to one {@link AnnotationRelation#getSource() source}.
     *
     * <p>{@link AnnotationRelationBasis#ONTOLOGY} has more than one producer — CLO states which disease
     * a cell line came from, CHEBI which roles a chemical bears — and they are loaded and rebuilt
     * independently. Rebuilding one with the basis-wide delete would silently drop the other's rows and
     * leave the table looking like the other producer had never run.</p>
     *
     * @param source the source to remove, or null for every source under the basis
     */
    int removeByBasis( AnnotationRelationBasis basis, @Nullable Long experimentId, @Nullable String source );
}
