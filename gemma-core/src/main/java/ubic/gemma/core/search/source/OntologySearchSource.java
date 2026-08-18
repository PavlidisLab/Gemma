package ubic.gemma.core.search.source;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.search.OntologySearchResult;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.Highlighter;
import ubic.gemma.core.search.OntologyHighlighter;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.core.search.SearchTimeoutException;
import ubic.gemma.core.search.lucene.LuceneParseSearchException;
import ubic.gemma.core.search.lucene.LuceneQueryUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchResultSet;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.CharacteristicService;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ubic.gemma.core.search.lucene.LuceneQueryUtils.extractTermsDnf;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.prepareTermUriQuery;
import static ubic.gemma.core.search.source.SearchSourceUtils.isFilled;

/**
 * Ontology-driven {@link SearchSource} for {@link ExpressionExperiment} discovery
 * via {@link Characteristic} URIs.
 *
 * <p><b>Phase 3 restoration.</b> This class was deleted wholesale in the Phase 2
 * "stub/delete search subsystem cascade" commit (ed93c2f023) and is restored here
 * per {@code SEARCH_RECCE.md} Section 6.3 Step 5. The body is the pre-strip
 * implementation (416 LoC) with one adjustment: the {@code findExperimentsByUris}
 * call uses the current public {@code CharacteristicService} signature
 * (which carries an explicit {@code loadEEs} parameter).
 *
 * <p><b>Known runtime gap (2026-05-19).</b> This source consumes
 * {@link OntologyService#findTerms(String, int, long, TimeUnit)}, which in turn
 * routes through baseCode's pre-renovations {@code findTerm} machinery.
 * baseCode 1.1.34-RENOVATIONS-SNAPSHOT stubbed its Lucene-3 indexer and exposes
 * no public hook for the in-Gemma {@code OntologySearchService}
 * ({@code jena-text} / Lucene 9) to slot under it. Until either baseCode gains
 * such a hook, or Gemma's {@code OntologyServiceImpl.findTerms} is rewired to
 * also consult {@link ubic.gemma.core.ontology.search.OntologySearchService},
 * full-text ontology search will return empty results and only the term-URI
 * exact-match path through {@link CharacteristicService#findBestByUri(String)}
 * will populate. See {@code SEARCH_RECCE.md} Section 6 for the architectural
 * gap recce and the path to closing it.
 */
@Component
@CommonsLog
public class OntologySearchSource implements SearchSource {

    /**
     * Penalty applied on a full-text result.
     */
    private static final double FULL_TEXT_SCORE_PENALTY = 0.9;

    /**
     * Penalty for indirect hits.
     */
    private static final double INDIRECT_HIT_PENALTY = 0.9;

    /**
     * Penalty for a hit reached through an inferred relation rather than through the annotation
     * itself.
     *
     * <p>Steeper than {@link #INDIRECT_HIT_PENALTY} on purpose. An indirect hit is still the term the
     * user asked for, sitting at a different level of the same dataset; an inferred hit is a
     * <i>different term</i> that Gemma believes stands for the one asked about. That is a weaker claim
     * and it should never outrank a dataset that is actually annotated with the query.</p>
     */
    private static final double INFERRED_RELATION_SCORE_PENALTY = 0.5;

    /**
     * Ceiling on how many terms one query may be widened by, across all seeds.
     *
     * <p>A runaway guard, not a working limit: a disease with a large sub-term fan-out could otherwise
     * turn one search into a query over thousands of URIs. When it bites, it is logged rather than
     * applied silently -- a search that quietly stopped widening looks identical to one that found
     * nothing to widen into.</p>
     */
    private static final int MAX_INFERRED_EXPANSION_TERMS = 200;

    /**
     * How many related terms any ONE matched term may contribute.
     *
     * <p>🛑 This is the limit that matters, and it is the difference between the two uses of the same
     * relations. Asking "is the disease I am about to tag among those this gene is associated with?"
     * is set membership, and every candidate belongs in the set. Widening a <i>search</i> is not: some
     * genes carry a great many diseases -- {@code Trp53} pairs with 15 in our corpus, and a null in it
     * genuinely models lung adenocarcinoma, medulloblastoma and breast cancer among others -- so
     * expanding through all of them turns a search for one of those into a search for every dataset
     * that ever mutated {@code Trp53}. The user asked a specific question and would get a
     * gene-shaped answer.</p>
     *
     * <p>A per-seed cap on the RANKED list is used rather than a specificity threshold, deliberately.
     * A threshold is an absolute number nobody has tuned against curator judgement yet, and the wrong
     * one silently changes what search returns; a cap is relative, so a gene with fifteen diseases
     * contributes its best few and a gene with one contributes its one.</p>
     */
    @org.springframework.beans.factory.annotation.Value("${gemma.search.inferredRelations.maxTermsPerSeed:5}")
    private int maxInferredTermsPerSeed = 5;

    /**
     * Minimum specificity a {@link AnnotationRelationBasis#CORPUS} relation must clear to widen a
     * search. Ignored for asserted bases, which are not counted.
     *
     * <p>Zero by default: no threshold has been tuned against curator judgement, and the honest
     * default is the one that does not quietly filter. The per-seed cap above is what keeps the
     * widening bounded in the meantime; this is here so the bar can be raised once the distribution
     * has been looked at.</p>
     */
    @org.springframework.beans.factory.annotation.Value("${gemma.search.inferredRelations.minSpecificity:0}")
    private double minInferredSpecificity = 0d;

    /**
     * Special indicator for exact matches. Those are stripped out when computing summary statistics and then assigned
     * the value of exactly 1.0.
     */
    private static final double EXACT_MATCH_SCORE = -1.0;

    /**
     * Amount of time to dedicate to searching and inferring terms. Bounded and tunable: because the
     * search runs in a read-only transaction, this is also the ceiling on how long one request can
     * hold its pooled DB connection during the ontology leg. The old 30s constant let a single slow
     * search pin a connection for 3× the Hikari acquire timeout, cascading into pool exhaustion under
     * load; default is now 10s (matching the pool's connectionTimeout).
     */
    @org.springframework.beans.factory.annotation.Value("${gemma.search.ontology.timeoutMs:10000}")
    private long ontologySearchAndInferenceTimeoutMillis;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    /**
     * Optional so that the several test contexts wiring this source by hand keep booting without it;
     * when absent, {@link SearchSettings#isUseInferredRelations()} simply has nothing to widen with.
     */
    @Autowired(required = false)
    private ubic.gemma.persistence.service.common.description.AnnotationRelationService annotationRelationService;

    @Override
    public boolean accepts( SearchSettings settings ) {
        return settings.isUseOntology()
                && settings.hasResultType( ExpressionExperiment.class )
                && settings.getMode().isAtLeast( SearchSettings.SearchMode.FAST );
    }

    /**
     * Search via characteristics i.e. ontology terms.
     */
    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( final SearchSettings settings, SearchContext context ) throws SearchException {
        Collection<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>( settings );

        StopWatch watch = StopWatch.createStarted();

        log.debug( "Starting EE search for " + settings );
        Set<Set<String>> subclauses = extractTermsDnf( settings, true, context.getIssueReporter() );
        for ( Set<String> subclause : subclauses ) {
            Collection<SearchResult<ExpressionExperiment>> classResults = this.searchExpressionExperiments( settings, context, subclause, Math.max( ontologySearchAndInferenceTimeoutMillis - watch.getTime(), 0 ) );
            if ( !classResults.isEmpty() ) {
                log.debug( String.format( "Found %d EEs matching %s", classResults.size(), String.join( " AND ", subclause ) ) );
            }
            results.addAll( classResults );
            if ( isFilled( results, settings ) ) {
                break;
            }
        }

        OntologySearchSource.log.debug( String.format( "ExpressionExperiment search: %s -> %d characteristic-based hits %d ms",
                settings, results.size(), watch.getTime() ) );

        return results;
    }

    private SearchResultSet<ExpressionExperiment> searchExpressionExperiments( SearchSettings settings, SearchContext context, Set<String> clause, long timeoutMs ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );

        OntologySearchSource.log.debug( "Starting characteristic search for: " + settings + " matching " + String.join( " AND ", clause ) );
        for ( String subClause : clause ) {
            String subClauseQuery = LuceneQueryUtils.quote( subClause );
            SearchResultSet<ExpressionExperiment> subqueryResults = doSearchExpressionExperiment( settings.withQuery( subClauseQuery ), context, timeoutMs );
            if ( results.isEmpty() ) {
                results.addAll( subqueryResults );
            } else {
                results.retainAll( subqueryResults );
            }
            if ( watch.getTime() > 1000 ) {
                OntologySearchSource.log.warn( String.format( "Characteristic EE search for '%s': %d hits retained so far; %dms",
                        subClauseQuery, results.size(), watch.getTime() ) );
                watch.reset();
                watch.start();
            }
            if ( results.isEmpty() ) {
                return results;
            }
        }

        return results;
    }

    private SearchResultSet<ExpressionExperiment> doSearchExpressionExperiment( SearchSettings settings, SearchContext context, long timeoutMs ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();
        long searchMs, childrenMs, retrievedMs;

        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );

        Collection<OntologyResult> ontologyResults = new HashSet<>();

        Collection<OntologySearchResult<OntologyTerm>> matchingTerms;

        searchMs = watch.getTime();
        URI termUri = prepareTermUriQuery( settings, context.getIssueReporter() );
        if ( termUri != null ) {
            OntologyResult resource;
            OntologyTerm r2;
            try {
                r2 = ontologyService.getTerm( termUri.toString(), Math.max( timeoutMs - watch.getTime(), 0L ), TimeUnit.MILLISECONDS );
            } catch ( TimeoutException e ) {
                throw new SearchTimeoutException( "Search timeout when attempting to retrieve " + termUri + ".", e );
            }
            if ( r2 != null ) {
                assert r2.getUri() != null;
                resource = new OntologyResult( r2, EXACT_MATCH_SCORE );
                matchingTerms = Collections.singleton( new OntologySearchResult<>( r2, EXACT_MATCH_SCORE ) );
            } else {
                Characteristic c = characteristicService.findBestByUri( termUri.toString() );
                if ( c != null ) {
                    assert c.getValueUri() != null;
                    resource = new OntologyResult( c.getValueUri(), c.getValue(), EXACT_MATCH_SCORE );
                } else {
                    resource = new OntologyResult( termUri.toString(), getLabelFromTermUri( termUri ), EXACT_MATCH_SCORE );
                }
                matchingTerms = Collections.emptySet();
            }
            ontologyResults.add( resource );
        } else {
            try {
                matchingTerms = ontologyService.findTerms( settings.getQuery(), 5000,
                        Math.max( timeoutMs - watch.getTime(), 0L ), TimeUnit.MILLISECONDS );
            } catch ( LuceneParseSearchException e ) {
                log.debug( String.format( "Failed to parse '%s': %s.", settings.getQuery(), ExceptionUtils.getRootCauseMessage( e ) ), e );
                matchingTerms = ontologyService.findTerms( LuceneQueryUtils.escape( settings.getQuery() ), 5000,
                        Math.max( timeoutMs - watch.getTime(), 0L ), TimeUnit.MILLISECONDS );
            }
            matchingTerms.stream()
                    .filter( t -> t.getResult().getUri() != null )
                    .map( t -> new OntologyResult( t.getResult(), t.getScore() ) )
                    .forEach( ontologyResults::add );
        }
        searchMs = watch.getTime() - searchMs;

        childrenMs = watch.getTime();
        if ( !matchingTerms.isEmpty() && timeoutMs > 0 ) {
            double avgScore = matchingTerms.stream()
                    .mapToDouble( OntologySearchResult::getScore )
                    .filter( s -> s != EXACT_MATCH_SCORE )
                    .average()
                    .orElse( 0 );
            try {
                ontologyService.getChildren( matchingTerms.stream().map( OntologySearchResult::getResult ).collect( Collectors.toSet() ), false, true, Math.max( timeoutMs - watch.getTime(), 1L ), TimeUnit.MILLISECONDS )
                        .stream()
                        .filter( c -> c.getUri() != null )
                        .map( c -> new OntologyResult( c, INDIRECT_HIT_PENALTY * avgScore ) )
                        .forEach( ontologyResults::add );
            } catch ( TimeoutException e ) {
                if ( settings.getMode().equals( SearchSettings.SearchMode.FAST ) ) {
                    log.warn( String.format( "Obtaining children for terms matching %s timed out, those will be ignored.", settings ), e );
                } else {
                    throw new SearchTimeoutException( String.format( "Obtaining children for terms matching '%s' timed out.", settings.getQuery() ), e );
                }
            }
        }
        childrenMs = watch.getTime() - childrenMs;

        retrievedMs = watch.getTime();
        findExperimentsByOntologyResults( ontologyResults, settings, context, results );
        retrievedMs = watch.getTime() - retrievedMs;

        String message = String.format( "Found %d datasets by %d characteristic URIs for '%s' in %d ms (ontology class search: %s ms, ontology inference: %s ms, retrieving matching datasets: %d ms)",
                results.size(), ontologyResults.size(), settings.getQuery(), watch.getTime(), searchMs, childrenMs, retrievedMs );
        if ( watch.getTime() > 1000 ) {
            log.warn( message );
        } else {
            log.debug( message );
        }

        return results;
    }

    private void findExperimentsByOntologyResults( Collection<OntologyResult> terms, SearchSettings settings, SearchContext context, SearchResultSet<ExpressionExperiment> results ) {
        Collection<String> uris = new HashSet<>();
        Map<String, String> uri2value = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        Map<String, Double> uri2score = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

        DoubleSummaryStatistics summaryStatistics = terms.stream()
                .map( OntologyResult::getScore )
                .mapToDouble( s -> s )
                .filter( s -> s != EXACT_MATCH_SCORE )
                .summaryStatistics();
        double m = summaryStatistics.getMin();
        double d = summaryStatistics.getMax() - summaryStatistics.getMin();

        for ( OntologyResult term : terms ) {
            uris.add( term.getUri() );
            uri2value.put( term.getUri(), term.getLabel() );
            if ( term.getScore() == EXACT_MATCH_SCORE ) {
                uri2score.put( term.getUri(), 1.0 );
            } else if ( d == 0 ) {
                uri2score.put( term.getUri(), FULL_TEXT_SCORE_PENALTY );
            } else {
                uri2score.put( term.getUri(), FULL_TEXT_SCORE_PENALTY * ( term.getScore() - m ) / d );
            }
        }

        Set<String> inferredUris = expandByInferredRelations( uris, uri2value, uri2score, settings );

        findExpressionExperimentsByUris( uris, uri2value, uri2score, inferredUris, settings, context, results );
    }

    /**
     * Widen the query into terms Gemma knows stand in some relation to the ones matched.
     *
     * <p>This is what makes a curation rule safe. If a redundant {@code disease} tag is dropped on the
     * grounds that the genotype beside it already implies the disease, the fact only stays findable if
     * something recovers the implication at search time. Storage without this is the failure case that
     * looks identical to success in any spot check of the database.</p>
     *
     * <p>Widened in <b>both</b> directions, because which end the user typed is not something to
     * assume: a curated statement puts the disease in the subject and the genotype in the object, so
     * searching the disease has to reach objects and searching the gene has to reach subjects.</p>
     *
     * <p>Ambiguity is kept rather than resolved. {@code SURF1} is associated with three diseases and
     * all three go into the set; a search matches if any of them does, which is the right answer
     * whichever one the dataset means. That is only true because this is set membership -- the same
     * ambiguity would be fatal to anything that had to pick one.</p>
     *
     * @return the URIs added, so a hit reached through one can say so
     */
    // package-private rather than private so the widening can be tested for what it does to the URI
    // set and the scores without standing up the whole ontology search path around it
    Set<String> expandByInferredRelations( Collection<String> uris, Map<String, String> uri2value,
            Map<String, Double> uri2score, SearchSettings settings ) {
        if ( !settings.isUseInferredRelations() || annotationRelationService == null || uris.isEmpty() ) {
            return Collections.emptySet();
        }
        StopWatch watch = StopWatch.createStarted();
        Set<String> seeds = new HashSet<>( uris );
        Set<String> added = new LinkedHashSet<>();
        Long taxonId = settings.getTaxonConstraint() != null ? settings.getTaxonConstraint().getId() : null;

        // Ranked, not raw. findRelatedTerms is the right primitive for a membership test and the wrong
        // one here: it returns the whole candidate set with nothing to order it by, which is exactly
        // what a search must not do when one seed has fifteen candidates and another has one.
        for ( AnnotationRelationDao.Direction direction : AnnotationRelationDao.Direction.values() ) {
            boolean seedIsSubject = direction == AnnotationRelationDao.Direction.SUBJECT_TO_OBJECT;
            AnnotationRelationDao.RelationQuery q = new AnnotationRelationDao.RelationQuery()
                    .taxonId( taxonId )
                    .minimumSpecificity( minInferredSpecificity )
                    // unlimited here, then capped per seed below: cutting the ranked list globally
                    // would let one prolific seed spend the whole budget
                    .maxResults( -1 );
            if ( seedIsSubject ) {
                q.subjectValueUris( seeds );
            } else {
                q.objectValueUris( seeds );
            }

            Map<String, Integer> perSeed = new HashMap<>();
            for ( AnnotationRelationDao.RelationSummary r : annotationRelationService.findRelations( q ) ) {
                String seedUri = seedIsSubject ? r.getSubjectValueUri() : r.getObjectValueUri();
                String relatedUri = seedIsSubject ? r.getObjectValueUri() : r.getSubjectValueUri();
                String relatedValue = seedIsSubject ? r.getObjectValue() : r.getSubjectValue();
                if ( relatedUri == null || seedUri == null || seeds.contains( relatedUri ) ) {
                    // an ungrounded related term cannot be matched against VALUE_URI, and a term the
                    // query already carries is a direct hit that must keep its undiminished score
                    continue;
                }
                if ( perSeed.merge( seedUri, 1, Integer::sum ) > maxInferredTermsPerSeed ) {
                    continue;
                }
                if ( added.size() >= MAX_INFERRED_EXPANSION_TERMS ) {
                    log.warn( String.format( "Inferred-relation widening for '%s' hit the %d-term ceiling; it is incomplete.",
                            settings.getQuery(), MAX_INFERRED_EXPANSION_TERMS ) );
                    break;
                }
                if ( !added.add( relatedUri ) ) {
                    continue;
                }
                uris.add( relatedUri );
                uri2value.put( relatedUri, relatedValue );
                uri2score.put( relatedUri, INFERRED_RELATION_SCORE_PENALTY );
            }
            int dropped = perSeed.values().stream().mapToInt( n -> Math.max( 0, n - maxInferredTermsPerSeed ) ).sum();
            if ( dropped > 0 ) {
                // said out loud rather than silently truncated: a seed whose tail was cut looks
                // identical to one that had nothing else to give
                log.debug( String.format( "Widening '%s' %s: %d related terms beyond the %d-per-term cap were not used.",
                        settings.getQuery(), direction, dropped, maxInferredTermsPerSeed ) );
            }
        }
        log.debug( String.format( "Widened '%s' by %d inferred terms in %d ms.",
                settings.getQuery(), added.size(), watch.getTime() ) );
        return added;
    }

    private void findExpressionExperimentsByUris( Collection<String> uris, Map<String, String> uri2value, Map<String, Double> uri2score, Set<String> inferredUris, SearchSettings settings, SearchContext context, SearchResultSet<ExpressionExperiment> results ) {
        if ( isFilled( results, settings ) )
            return;

        boolean rankByLevel = settings.getMode().equals( SearchSettings.SearchMode.ACCURATE );

        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = characteristicService.findExperimentsByUris(
                uris, true, true, true, settings.getTaxonConstraint(),
                getLimit( results, settings ), settings.isFillResults(), rankByLevel );

        if ( hits.containsKey( ExpressionExperiment.class ) ) {
            addExperimentsByUrisHits( hits.get( ExpressionExperiment.class ), "characteristics.valueUri", 1.0, uri2value, uri2score, inferredUris, context.getHighlighter(), results );
        }

        if ( hits.containsKey( ExperimentalDesign.class ) ) {
            addExperimentsByUrisHits( hits.get( ExperimentalDesign.class ), "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri", 0.9, uri2value, uri2score, inferredUris, context.getHighlighter(), results );
        }

        if ( hits.containsKey( BioMaterial.class ) ) {
            addExperimentsByUrisHits( hits.get( BioMaterial.class ), "bioAssays.sampleUsed.characteristics.valueUri", 0.9, uri2value, uri2score, inferredUris, context.getHighlighter(), results );
        }
    }

    private void addExperimentsByUrisHits( Map<String, Set<ExpressionExperiment>> hits, String field, double scoreMultiplier, Map<String, String> uri2value, Map<String, Double> uri2score, Set<String> inferredUris, @Nullable Highlighter highlighter, SearchResultSet<ExpressionExperiment> results ) {
        for ( Map.Entry<String, Set<ExpressionExperiment>> entry : hits.entrySet() ) {
            String uri = entry.getKey();
            String value = uri2value.get( uri );
            // The provenance string says which of the two it was. A dataset returned because it is
            // annotated with the query and one returned because Gemma thinks a term it carries stands
            // for the query are different claims, and a UI that cannot tell them apart will present
            // the second as though it were the first.
            String provenance = inferredUris.contains( uri )
                    ? String.format( "CharacteristicService.findExperimentsByUris with inferred term [%s](%s)", value, uri )
                    : String.format( "CharacteristicService.findExperimentsByUris with term [%s](%s)", value, uri );
            for ( ExpressionExperiment ee : entry.getValue() ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, scoreMultiplier * uri2score.getOrDefault( uri, 0.0 ),
                        highlightTerm( highlighter, uri, value, field ),
                        provenance ) );
            }
        }
    }

    private static <T extends Identifiable> int getLimit( Collection<SearchResult<T>> results, SearchSettings settings ) {
        if ( isFilled( results, settings ) ) {
            throw new IllegalArgumentException( "Search results are already fully filled, have to checked the collection with isFilled()?" );
        }
        return settings.getMaxResults() > 0 ? settings.getMaxResults() - results.size() : -1;
    }

    /**
     * Extract a label for a term URI as per {@link OntologyTerm#getLabel()}.
     */
    static String getLabelFromTermUri( URI termUri ) {
        String[] segments = termUri.getPath().split( "/" );
        if ( !StringUtils.isEmpty( termUri.getFragment() ) ) {
            return partToTerm( termUri.getFragment() );
        }
        for ( int i = segments.length - 1; i >= 0; i-- ) {
            if ( !StringUtils.isEmpty( segments[i] ) ) {
                return partToTerm( segments[i] );
            }
        }
        return termUri.toString();
    }

    private static String partToTerm( String part ) {
        return part.replaceFirst( "_", ":" ).toUpperCase();
    }

    @Value
    @EqualsAndHashCode(of = { "uri" })
    private static class OntologyResult {
        String uri;
        String label;
        double score;

        private OntologyResult( String uri, String label, double score ) {
            this.uri = uri;
            this.label = label;
            this.score = score;
        }

        public OntologyResult( OntologyTerm resource, double score ) {
            this.uri = resource.getUri();
            if ( resource.getLabel() != null ) {
                this.label = resource.getLabel();
            } else {
                this.label = resource.getLocalName();
            }
            this.score = score;
        }
    }

    @Nullable
    public Map<String, String> highlightTerm( @Nullable Highlighter highlighter, String termUri, String termLabel, String field ) {
        if ( highlighter instanceof OntologyHighlighter ) {
            return ( ( OntologyHighlighter ) highlighter ).highlightTerm( termUri, termLabel, field );
        } else {
            return null;
        }
    }
}
