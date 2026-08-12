/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.common.description;

import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BrowsingDao;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import org.springframework.lang.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 */
public interface CharacteristicDao
        extends BrowsingDao<Characteristic>, FilteringVoEnabledDao<Characteristic, CharacteristicValueObject> {

    /**
     * NCBI taxonomy id for human, which decides whether a disease inference reads as
     * {@code disease} or {@code disease model}.
     *
     * @see DiseaseModelInference#getInferredCategory()
     */
    int HUMAN_NCBI_TAXON_ID = 9606;

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param start How far into the list to start
     * @param limit Maximum records to retrieve (might be subject to security filtering)
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( int start, int limit );

    /**
     * Browse through the characteristics, excluding GO annotations, with sorting.
     *
     * @param start      query offset
     * @param limit      maximum amount of entries
     * @param descending order direction
     * @param sortField  order field
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( int start, int limit, String sortField, boolean descending );

    Collection<Characteristic> findByParentClasses( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, @Nullable String category, int maxResults );

    Collection<Characteristic> findByCategory( String value );

    Collection<Characteristic> findByCategoryLike( String query, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    Collection<Characteristic> findByCategoryUri( String uri, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * This search looks at direct annotations, factor values and biomaterials in that order.
     * <p>
     * Resulting EEs are filtered by ACLs.
     * <p>
     * The returned collection of EEs is effectively a {@link Set}, but since we cannot use since this should be
     * interchangable with {@link #findExperimentReferencesByUris(Collection, boolean, boolean, boolean, Taxon, int, boolean)}.
     * <p>
     * Ranking results by level guarantees correctness if a limit is used as datasets matched by direct annotation will
     * be considered before those matched by factor values or biomaterials. It is however expensive.
     *
     * @param uris              collection of URIs used for matching characteristics (via {@link Characteristic#getValueUri()})
     * @param includeSubjects   lookup subjects (or values for regular characteristics)
     * @param includePredicates lookup predicates (only applicable to {@link Statement}s)
     * @param includeObjects    lookup objects (only applicable to {@link Statement}s)
     * @param taxon             taxon to restrict EEs to, or null to ignore
     * @param limit             limit how many results to return. Set to -1 for no limit.
     * @param rankByLevel       rank results by level before limiting, has no effect if limit is -1
     * @return map of classes ({@link ExpressionExperiment}, {@link ubic.gemma.model.expression.experiment.FactorValue},
     * {@link ubic.gemma.model.expression.biomaterial.BioMaterial}) to the matching URI to EEs which have an associated
     * characteristic using the given URI. The class lets us track where the annotation was.
     */
    Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean rankByLevel );

    /**
     * Similar to {@link #findExperimentsByUris(Collection, boolean, boolean, boolean, Taxon, int, boolean)}, but returns proxies with instead of
     * initializing all the EEs in bulk.
     *
     * @see org.hibernate.Session#load(Object, Serializable)
     */
    Map<Class<? extends Identifiable>, Map<String, Set<@MayBeUninitialized ExpressionExperiment>>> findExperimentReferencesByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean rankByLevel );

    /**
     * Find characteristics with the given URI.
     *
     * @param category         restrict the category of the characteristic, or null to ignore
     * @param parentClasses    only return characteristics that have parents of these classes, or null to ignore
     * @param includeNoParents include characteristics that have no parents
     * @param maxResults       maximum number of results to return, or -1 for no limit
     */
    Collection<Characteristic> findByUri( String uri, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Return the characteristic with the most frequently used non-null value by URI.
     */
    Characteristic findBestByUri( String uri );

    /**
     * One representative, ACL-visible usage of a term (keyed by its value URI), for showing a search hit in
     * the context it has actually been applied (e.g. a curator picking a rare term sees "wild type" under
     * the "genotype" factor of some accessible dataset). Unlike {@link #countByCategory}, this exposes a
     * specific dataset + statement, so it MUST be ACL-restricted — sourced from the denormalised {@code EE2C}
     * view with the same ACL clause the usage-frequency queries use, so a private dataset never leaks.
     */
    class UsageExample {
        @Nullable
        public final Class<? extends Identifiable> level;
        @Nullable
        public final String category, categoryUri, value, valueUri;
        @Nullable
        public final String predicate, predicateUri, object, objectUri;
        @Nullable
        public final String secondPredicate, secondPredicateUri, secondObject, secondObjectUri;
        public final long sourceExperimentId;

        public UsageExample( @Nullable Class<? extends Identifiable> level, @Nullable String category, @Nullable String categoryUri,
                @Nullable String value, @Nullable String valueUri, @Nullable String predicate, @Nullable String predicateUri,
                @Nullable String object, @Nullable String objectUri, @Nullable String secondPredicate, @Nullable String secondPredicateUri,
                @Nullable String secondObject, @Nullable String secondObjectUri, long sourceExperimentId ) {
            this.level = level;
            this.category = category;
            this.categoryUri = categoryUri;
            this.value = value;
            this.valueUri = valueUri;
            this.predicate = predicate;
            this.predicateUri = predicateUri;
            this.object = object;
            this.objectUri = objectUri;
            this.secondPredicate = secondPredicate;
            this.secondPredicateUri = secondPredicateUri;
            this.secondObject = secondObject;
            this.secondObjectUri = secondObjectUri;
            this.sourceExperimentId = sourceExperimentId;
        }
    }

    /**
     * For each supplied value URI, return one representative ACL-visible {@link UsageExample}, or omit the URI
     * when no accessible usage exists. Batched (one query for the whole set), sourced from {@code EE2C} keyed
     * on the indexed {@code VALUE_URI} column.
     */
    Map<String, UsageExample> findRepresentativeUsageByValueUris( Collection<String> valueUris );

    /**
     * An annotation value inferred to stand for a disease, recovered from curation Gemma already holds.
     * <p>
     * Curation policy records a mutant/wild-type contrast as a {@code genotype} factor, not a
     * {@code disease model} one — the mutation is what varies across samples, and that is what a factor is
     * for. The disease then lives nowhere on studies that annotate only the genotype, and a user picking
     * "autism" in the disease selector stops finding the {@code Chd8} mutant studies. The relation is
     * recoverable rather than lost: an experiment annotated BOTH {@code genotype = Homozygous negative
     * Mecp2} and {@code disease = Rett syndrome} attests that the genotype stands for the disease, and 16
     * such experiments attest it 16 times over. Nothing here asserts anything — the inference is only as
     * good as the curation behind it, which is why {@link #numberOfExperiments} and
     * {@link #exampleExperimentId} travel with every row and no annotation is ever written.
     * <p>
     * <b>What the inferred annotation would say depends on the taxon</b>, which is why {@link #taxonId} is
     * part of the grain. A mouse carrying the {@code Mecp2} null is a MODEL of Rett syndrome; a human
     * iPSC line carrying {@code LRRK2 G2019S} is not modelling Parkinson disease, it HAS it. So the same
     * derivation yields {@code disease model = D} for a non-human experiment and {@code disease = D} for a
     * human one — see {@link #getInferredCategory()}.
     * <p>
     * The key is the whole annotation VALUE, not a gene or any parse of one: {@code Myc} overexpression and
     * {@code Myc} knockdown accompany different diseases, and {@code APP/PS1}, {@code 5xFAD} or
     * {@code trisomy 21} name no gene at all. {@link #valueUri} is null for values never grounded in an
     * ontology, so both it and {@link #value} are carried and either can drive a query.
     * <p>
     * 🛑 <b>Support alone is the wrong rank, and confidently so.</b> {@code C57BL/6J} co-occurs with every
     * disease in the corpus and models none of them; the disease in those experiments is induced by diet,
     * by surgery, by noise, or belongs to the cell line rather than to the strain. What separates
     * {@code Mecp2} null (attested against Rett syndrome and little else) from {@code C57BL/6J} (attested
     * against hundreds of diseases) is not how often the pair appears but what FRACTION of the value's
     * experiments it accounts for — see {@link #getSpecificity()}, which is why
     * {@link #numberOfExperimentsWithValue} and {@link #numberOfDiseasesAttested} are part of every row and
     * not an optional extra. A ranking on raw count puts the strains on top.
     */
    class DiseaseModelInference {
        @Nullable
        public final String value, valueUri, category, categoryUri;
        /**
         * The disease term this value was inferred to stand for — one of the URIs passed in, so a caller
         * that expanded a term to its sub-classes can still say which one did the matching.
         */
        public final String diseaseValueUri;
        @Nullable
        public final String diseaseValue;
        @Nullable
        public final Long taxonId;
        @Nullable
        public final String taxonCommonName;
        @Nullable
        public final Integer taxonNcbiId;
        /**
         * Distinct accessible experiments attesting the inference. The confidence weight.
         */
        public final long numberOfExperiments;
        /**
         * The same count split by where the annotation sits, since a factor value (the property varies
         * across samples) and an experiment tag (it holds of the whole experiment) are different evidence.
         */
        public final long numberOfExperimentsAsFactorValue, numberOfExperimentsAsExperimentTag, numberOfExperimentsAsSampleCharacteristic;
        /**
         * One accessible experiment attesting it, so a client can link straight to the evidence.
         */
        public final long exampleExperimentId;
        /**
         * Every experiment carrying this value in this category, whatever disease it was about — the
         * denominator that tells {@code Mecp2} null apart from {@code C57BL/6J}.
         */
        public final long numberOfExperimentsWithValue;
        /**
         * Distinct diseases this value has been attested against anywhere in the corpus. One or two is a
         * model; hundreds is a background strain, or a drug tested against everything.
         */
        public final long numberOfDiseasesAttested;

        public DiseaseModelInference( @Nullable String value, @Nullable String valueUri, @Nullable String category,
                @Nullable String categoryUri, String diseaseValueUri, @Nullable String diseaseValue,
                @Nullable Long taxonId, @Nullable String taxonCommonName, @Nullable Integer taxonNcbiId,
                long numberOfExperiments, long numberOfExperimentsAsFactorValue, long numberOfExperimentsAsExperimentTag,
                long numberOfExperimentsAsSampleCharacteristic, long exampleExperimentId,
                long numberOfExperimentsWithValue, long numberOfDiseasesAttested ) {
            this.value = value;
            this.valueUri = valueUri;
            this.category = category;
            this.categoryUri = categoryUri;
            this.diseaseValueUri = diseaseValueUri;
            this.diseaseValue = diseaseValue;
            this.taxonId = taxonId;
            this.taxonCommonName = taxonCommonName;
            this.taxonNcbiId = taxonNcbiId;
            this.numberOfExperiments = numberOfExperiments;
            this.numberOfExperimentsAsFactorValue = numberOfExperimentsAsFactorValue;
            this.numberOfExperimentsAsExperimentTag = numberOfExperimentsAsExperimentTag;
            this.numberOfExperimentsAsSampleCharacteristic = numberOfExperimentsAsSampleCharacteristic;
            this.exampleExperimentId = exampleExperimentId;
            this.numberOfExperimentsWithValue = numberOfExperimentsWithValue;
            this.numberOfDiseasesAttested = numberOfDiseasesAttested;
        }

        /**
         * The category the inferred annotation would carry: {@code disease} when the experiment is human —
         * the subject has the disease — and {@code disease model} otherwise.
         * <p>
         * Taxon-unknown experiments (a null {@code TAXON_FK}) fall to {@code disease model}, the weaker of
         * the two claims.
         */
        public Category getInferredCategory() {
            return taxonNcbiId != null && taxonNcbiId == HUMAN_NCBI_TAXON_ID
                    ? Categories.DISEASE
                    : Categories.DISEASE_MODEL;
        }

        /**
         * The fraction of this value's experiments that are about this disease, in {@code [0, 1]}.
         * <p>
         * This is what makes the inference safe to act on. {@code Abca4} null is annotated retinal
         * degeneration nearly every time it appears — a high fraction, and the disease-model tag on such a
         * study is recoverable without it. {@code C57BL/6J} appears against obesity in a handful of the many
         * hundreds of experiments that use the strain, and obesity there is diet-induced: a low fraction,
         * and the tag has to stay because nothing else carries the disease.
         */
        public double getSpecificity() {
            return numberOfExperimentsWithValue > 0 ? ( double ) numberOfExperiments / numberOfExperimentsWithValue : 0;
        }

        /**
         * Rank key: support weighted by specificity, so a pair needs BOTH to come out on top. Equivalent to
         * {@code support² / experiments-with-value}.
         */
        public double getScore() {
            return numberOfExperiments * getSpecificity();
        }
    }

    /**
     * Infer which annotation values stand for which diseases, from curation the corpus already carries.
     * <p>
     * Seed EITHER side. Constrain the disease side ({@code diseaseValueUris}) to ask "what models Alzheimer
     * disease?"; constrain the model side ({@code modelValueUris} / {@code modelValues}) to ask "what does
     * this genotype model?", which is the question an experiment page asks about its own annotations and the
     * question behind dropping a {@code disease model} tag as redundant. Constrain both to test one specific
     * inference. At least one constraint is required — this does not enumerate the corpus.
     * <p>
     * The disease side is always identified by CATEGORY ({@code disease} or {@code disease model}), never by
     * which side was seeded, so a row means the same thing whichever way it was asked.
     * <p>
     * ACL-restricted, like {@link #findRepresentativeUsageByValueUris(Collection)}: the result names
     * specific datasets, so a private dataset must not contribute a count or an example.
     * <p>
     * Baseline values are dropped — a control arm models nothing — using the same recognition
     * {@code BaselineSelection} applies when picking a DEA baseline.
     *
     * @param diseaseValueUris       diseases of interest, or empty for any; pass a term together with its
     *                               inferred sub-terms to have those count as well
     * @param modelValueUris         restrict the model side to these value URIs, or empty for any
     * @param modelValues            restrict the model side to these literal values, for the many genotypes
     *                               and strains that were never grounded in an ontology ({@code APP/PS1},
     *                               {@code Tp53/Rb1 DKO}). OR'd with {@code modelValueUris}
     * @param modelCategories        categories the model side must be under, as labels ({@code genotype},
     *                               {@code strain}) or category URIs; empty means any category. This is what
     *                               generalises the derivation past genotypes — an exposure can model a
     *                               disease too, though most exposures are interventions and will show it in
     *                               their specificity
     * @param excludedExperimentIds  experiments that must not contribute evidence. Holding out the dataset
     *                               being examined is what makes "this tag is inferable, so it can be
     *                               dropped" an honest claim rather than a restatement of the tag
     * @param minimumSupport         drop inferences attested by fewer than this many experiments
     * @param maxResults             cap on returned rows, or -1 for no cap. Applied after ranking by
     *                               {@link DiseaseModelInference#getScore()}
     */
    List<DiseaseModelInference> findDiseaseModelInferences( Collection<String> diseaseValueUris,
            Collection<String> modelValueUris, Collection<String> modelValues, Collection<String> modelCategories,
            Collection<Long> excludedExperimentIds, int minimumSupport, int maxResults );

    /**
     * Find characteristics by URI.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link CharacteristicUtils#getNormalizedValue(Characteristic)}.
     */
    Map<String, Characteristic> findByValueUriGroupedByNormalizedValue( String valueUri, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    /**
     * Find characteristics by value matching the provided LIKE pattern.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link CharacteristicUtils#getNormalizedValue(Characteristic)}.
     */
    Map<String, Characteristic> findByValueLikeGroupedByNormalizedValue( String valueLike, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    /**
     * Count characteristics matching the provided value URIs.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link CharacteristicUtils#getNormalizedValue(Characteristic)}.
     */
    Map<String, Long> countByValueUriGroupedByNormalizedValue( Collection<String> uris, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    /**
     * For each supplied value URI, return the distinct-experiment count grouped by the category
     * the curator applied when tagging the URI on an experiment. E.g. a single URI
     * {@code http://purl.obolibrary.org/obo/CLO_0037182} might map to
     * {@code {"cell line" → 14, "cell type" → 1}} when prior curators have used it 14 times under
     * category "cell line" and once under "cell type".
     * <p>
     * Sourced from the denormalised {@code EE2C} view, so the result reflects ACL-unrestricted
     * corpus-wide curation history (matches the spirit of usage-count metadata: aggregate
     * signal about the URI, not per-experiment information). Rows with a {@code null} category
     * are dropped from the result — they don't carry actionable signal for resolvers.
     */
    Map<String, Map<String, Long>> findEeCountsByUriGroupedByCategory( Collection<String> uris );

    /**
     * For each supplied value URI, count the distinct experiments on which a prior curator wrote
     * {@code originalValue} as the submitter-facing string for that URI.
     * <p>
     * This answers a different question from the usage count. A usage count is about the TERM —
     * "which compound is meant" — whereas this is about the STRING: of everyone who actually
     * wrote the words you are searching for, how many meant each candidate? On the production
     * corpus {@code "dmso"} resolves to {@code {CHEBI_28262 → 508, OBI_0000025 → 16}}, which
     * separates the compound from the role even though both are legitimate hits.
     * <p>
     * Matching is case-insensitive and tolerates GEO's field prefix, so {@code "treatment: DMSO"}
     * and {@code "agent: DMSO"} both count towards {@code "dmso"}. Strings that merely CONTAIN the
     * value do not count — {@code "0.3% DMSO"} is a different string, and treating it as this one
     * would inflate the evidence with annotations nobody wrote this way.
     * <p>
     * Sourced from the denormalised {@code EE2C} view and counted per distinct experiment, so a
     * 500-sample study cannot weight the tally 500× on one submitter's naming choice. Rows with a
     * {@code null} original value are excluded: they record that the value was never edited, not
     * that anybody chose to write it.
     */
    Map<String, Long> findEeCountsByUriForOriginalValue( Collection<String> uris, String originalValue );

    /**
     * As {@link #findEeCountsByUriForOriginalValue(Collection, String)}, ignoring the given
     * experiments.
     * <p>
     * This exists for leave-one-out evaluation. A tally taken over the whole corpus includes the
     * very experiments a held-out gold set was drawn from, so it is partly counting the answer key
     * and cannot be used to score a resolver. Excluding those experiments makes the count
     * independent evidence about the string.
     *
     * @param excludedExperimentIds experiments to leave out of the tally; empty for the corpus-wide
     *                              count
     */
    Map<String, Long> findEeCountsByUriForOriginalValue( Collection<String> uris, String originalValue,
            Collection<Long> excludedExperimentIds );

    /**
     * For a value string, return the terms prior curators actually chose when they met it, most
     * used first.
     * <p>
     * This is a retrieval question, not a ranking one, and it is not answerable lexically. Curators
     * annotate {@code vehicle}, {@code untreated} and {@code sham} with
     * {@code reference substance role} / {@code reference subject role} — terms that share no word
     * with the string, so no label or synonym search will ever return them however it is ranked.
     * The corpus is the only place that mapping is written down. Likewise {@code EAE} resolves
     * lexically to {@code episodic angioedema with eosinophilia} and in the corpus to
     * {@code experimental autoimmune encephalomyelitis}, which is what it means.
     * <p>
     * Matching follows {@link #findEeCountsByUriForOriginalValue(Collection, String)}: case
     * insensitive, tolerant of GEO's field prefix, exact on the remainder, and refused outright
     * for values with no letters in them. The label reported for each URI is the one most often
     * stored beside it, so the result stays readable without loading the owning ontology.
     * <p>
     * ⚠️ These counts are curation history, which means they carry its mistakes: a string
     * mis-tagged for years comes back with a large and authoritative-looking count. Treat an entry
     * as evidence with a denominator, never as a verdict.
     *
     * @param maxResults cap on the number of distinct terms returned, or -1 for no cap
     */
    List<PriorCurationUsage> findPriorCurationByOriginalValue( String originalValue, int maxResults );

    /**
     * As {@link #findPriorCurationByOriginalValue(String, int)}, ignoring the given experiments.
     *
     * @see #findEeCountsByUriForOriginalValue(Collection, String, Collection) for why leaving
     *      experiments out matters
     */
    List<PriorCurationUsage> findPriorCurationByOriginalValue( String originalValue, int maxResults,
            Collection<Long> excludedExperimentIds );

    /**
     * A term prior curators chose for some value string, and how many distinct experiments they
     * chose it on.
     */
    class PriorCurationUsage {
        public final String valueUri;
        @Nullable
        public final String value;
        public final long experimentCount;
        /**
         * This term's share of every annotation made from this string, in {@code [0, 1]}.
         * <p>
         * The count alone cannot be read safely, because it does not say whether curators agreed.
         * {@code wild type} goes to {@code wild type genotype} 1421 times against 2 for anything
         * else — settled convention. {@code sham} goes to {@code reference subject role} 187 times
         * and to {@code reference substance role} 28 — the same corpus, genuinely contested, and a
         * consumer should treat the two very differently. Computed over all terms for the string,
         * before any truncation, so it does not drift with the result cap.
         */
        public final double agreement;

        public PriorCurationUsage( String valueUri, @Nullable String value, long experimentCount, double agreement ) {
            this.valueUri = valueUri;
            this.value = value;
            this.experimentCount = experimentCount;
            this.agreement = agreement;
        }
    }

    /**
     * Find characteristics {@link Characteristic#getValue()} grouped by {@link Characteristic#getValueUri()}.
     * <p>
     * The results are grouped by value URIs, so free-text terms will not be returned. If you need a way to get both
     * free-text and URI annotations, use {@link #findByValueUriGroupedByNormalizedValue(String, Collection, boolean)}
     * instead.
     *
     * @param parentClasses     restrict the parents to these classes, all parents are returned if null. If supplied, at
     *                          least one parent must be provided unless includeNoParents is true.
     * @param includeNoParents  include characteristics that have no parents, those will be mapped explicitly to
     *                          {@code null}.
     * @param includePredicates if true, include {@link Statement#getPredicateUri()} and {@link Statement#getPredicate()} pairs
     * @param includeObjects    if true, include {@link Statement#getObjectUri()} and {@link Statement#getObject()} pairs
     * @param maxResults        maximum number of results to return, or -1 for no limit
     */
    Map<String, String> findValueGroupedByValueUri( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, boolean includePredicates, boolean includeObjects, int maxResults );

    Collection<Characteristic> findByValue( String search );

    /**
     * Finds all Characteristics whose value match the given search term
     *
     * @param category constraint the category of the characteristic, or null to ignore
     */
    Collection<Characteristic> findByValueLike( String search, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Obtain the classes of entities can can own a {@link Characteristic}.
     */
    Collection<Class<? extends Identifiable>> getParentClasses();

    /**
     * Obtain the parents (i.e. owners) of the given characteristics.
     *
     * @param characteristics  characteristics to find parents for
     * @param parentClasses    restrict the parents to these classes, all parents are returned if null. If supplied, at
     *                         least one parent must be provided unless includeNoParents is true.
     * @param includeNoParents include characteristics that have no parents, those will be mapped explicitly to
     *                         {@code null}.
     * @return the supplied characteristics mapped to their parents, or {@code null} if the characteristic has no parent
     * and includeNoParents is true. A characteristic may not have multiple parents.
     */
    Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );
}
