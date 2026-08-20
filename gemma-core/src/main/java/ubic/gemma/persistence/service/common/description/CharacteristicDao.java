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
     * Count the distinct experiments referencing each of the given URIs.
     * <p>
     * Same matching rules as {@link #findExperimentsByUris(Collection, boolean, boolean, boolean, Taxon, int, boolean)}
     * — the same per-column URI lookups, the same ACL restriction, the same taxon restriction — but
     * the tally is formed by the database rather than by returning the matching experiments and
     * counting them here.
     * <p>
     * Use this whenever the answer wanted is the number and not the experiments. The row-returning
     * form emits one row per matching EE2C row, so its cost follows the size of the corpus the
     * candidates cover rather than the size of the answer: measured on gemma2 2026-08-16,
     * {@code /annotations/search?rank=composite} spent 3.7s of a 4.1s response counting a
     * 1000-URI candidate set whose answer is 1000 numbers.
     *
     * @param excludedExperimentIds experiments to leave out of the tally, for leave-one-out
     *                              evaluation. Applied in the query: an aggregate never returns the
     *                              experiment ids, so there is nothing left to filter afterwards.
     * @return map of URI to the number of distinct experiments referencing it, restricted to what
     * the current user may read. A URI nothing references is absent rather than zero.
     */
    Map<String, Long> countExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, Collection<Long> excludedExperimentIds );

    /**
     * As {@link #countExperimentsByUris(Collection, boolean, boolean, boolean, Taxon, Collection)}, but able to
     * count usage in the CATEGORY slot as well.
     * <p>
     * Separate overload rather than a changed signature because every existing caller counts term usage, where the
     * category is a different question; only a sweep looking at every slot a URI can occupy wants both.
     */
    Map<String, Long> countExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, boolean includeCategories, @Nullable Taxon taxon, Collection<Long> excludedExperimentIds );

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

    /**
     * Find representative {@link Characteristic#getCategory()} labels grouped by
     * {@link Characteristic#getCategoryUri()}.
     * <p>
     * The category slot holds ontology terms just as the value slot does, and it goes stale the same way — the
     * "disease" category is {@code EFO_0000408}, which EFO obsoleted. A sweep that reads only value/predicate/object
     * URIs cannot see that, and reports the term as unused rather than as a problem.
     *
     * @param parentClasses    restrict the parents to these classes, all parents are returned if null
     * @param includeNoParents include characteristics that have no parents
     * @param maxResults       maximum number of results to return, or -1 for no limit
     */
    Map<String, String> findCategoryGroupedByCategoryUri( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

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
