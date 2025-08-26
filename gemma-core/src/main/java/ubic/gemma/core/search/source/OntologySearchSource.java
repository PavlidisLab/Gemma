package ubic.gemma.core.search.source;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.*;
import ubic.gemma.core.search.lucene.LuceneParseSearchException;
import ubic.gemma.core.search.lucene.LuceneQueryUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.CharacteristicService;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ubic.gemma.core.search.SearchSettingsUtils.isFilled;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.extractTermsDnf;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.prepareTermUriQuery;

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
     * Special indicator for exact matches. Those are stripped out when computing summary statistics and then assigned
     * the value of exactly 1.0.
     */
    private static final double EXACT_MATCH_SCORE = -1.0;

    /**
     * Amount of time to dedicate to searching and inferring terms.
     */
    private static final long ONTOLOGY_SEARCH_AND_INFERENCE_TIMEOUT_MILLIS = 30000L;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    @Override
    public boolean accepts( SearchSettings settings ) {
        return settings.isUseCharacteristics();
    }

    /**
     * Search via characteristics i.e. ontology terms.
     * <p>
     * This is an important type of search but also a point of performance issues. Searches for "specific" terms are
     * generally not a big problem (yielding less than 100 results); searches for "broad" terms can return numerous
     * (thousands)
     * results.
     */
    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( final SearchSettings settings, SearchContext context ) throws SearchException {
        Collection<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>( settings );

        StopWatch watch = StopWatch.createStarted();

        log.debug( "Starting EE search for " + settings );
        /*
         * Note that the AND is applied only within one entity type. The fix would be to apply AND at this
         * level.
         *
         * The tricky part here is if the user has entered a boolean query. If they put in Parkinson's disease AND
         * neuron, then we want to eventually return entities that are associated with both. We don't expect to find
         * single characteristics that match both.
         *
         * But if they put in Parkinson's disease we don't want to do two queries.
         */
        Set<Set<String>> subclauses = extractTermsDnf( settings, true, context.getIssueReporter() );
        for ( Set<String> subclause : subclauses ) {
            Collection<SearchResult<ExpressionExperiment>> classResults = this.searchExpressionExperiments( settings, context, subclause, Math.max( ONTOLOGY_SEARCH_AND_INFERENCE_TIMEOUT_MILLIS - watch.getTime(), 0 ) );
            if ( !classResults.isEmpty() ) {
                log.debug( String.format( "Found %d EEs matching %s", classResults.size(), String.join( " AND ", subclause ) ) );
            }
            results.addAll( classResults );
            // this is an OR query, so we can stop as soon as we've retrieved enough results
            if ( isFilled( results, settings ) ) {
                break;
            }
        }

        OntologySearchSource.log.debug( String.format( "ExpressionExperiment search: %s -> %d characteristic-based hits %d ms",
                settings, results.size(), watch.getTime() ) );

        return results;
    }

    /**
     * Search for the Experiment query in ontologies, including items that are associated with children of matching
     * query terms. That is, 'brain' should return entities tagged as 'hippocampus'. It can handle AND in searches, so
     * Parkinson's
     * AND neuron finds items tagged with both of those terms. The use of OR is handled by the caller.
     *
     * @param settings      search settings
     * @param clause        a conjunctive clause
     * @return SearchResults of Experiments
     */
    private SearchResultSet<ExpressionExperiment> searchExpressionExperiments( SearchSettings settings, SearchContext context, Set<String> clause, long timeoutMs ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        // we would have to first deal with the separate queries, and then apply the logic.
        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );

        OntologySearchSource.log.debug( "Starting characteristic search for: " + settings + " matching " + String.join( " AND ", clause ) );
        for ( String subClause : clause ) {
            // at this point, subclauses have already been parsed, so if they contain special characters, those must be
            // escaped, spaces must be quoted
            String subClauseQuery = LuceneQueryUtils.quote( subClause );
            SearchResultSet<ExpressionExperiment> subqueryResults = doSearchExpressionExperiment( settings.withQuery( subClauseQuery ), context, timeoutMs );
            if ( results.isEmpty() ) {
                results.addAll( subqueryResults );
            } else {
                // this is our Intersection operation.
                results.retainAll( subqueryResults );
            }
            if ( watch.getTime() > 1000 ) {
                OntologySearchSource.log.warn( String.format( "Characteristic EE search for '%s': %d hits retained so far; %dms",
                        subClauseQuery, results.size(), watch.getTime() ) );
                watch.reset();
                watch.start();
            }
            // if one subquery has no results, the intersection will be empty and we can return early
            if ( results.isEmpty() ) {
                return results;
            }
        }

        return results;
    }

    /**
     * Perform a Experiment search based on annotations (anchored in ontology terms) - it does not have to be one word,
     * it could be "parkinson's disease"; it can also be a URI.
     *
     * @return collection of SearchResults (Experiments)
     */
    private SearchResultSet<ExpressionExperiment> doSearchExpressionExperiment( SearchSettings settings, SearchContext context, long timeoutMs ) throws SearchException {
        // overall timer
        StopWatch watch = StopWatch.createStarted();
        long searchMs, childrenMs, retrievedMs;

        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );

        Collection<OntologyResult> ontologyResults = new HashSet<>();

        Collection<OntologySearchResult<OntologyTerm>> matchingTerms;

        // if the query is a term, find it directly
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
                // attempt to guess a label from othe database
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
            // Search ontology classes matches to the full-text query
            try {
                matchingTerms = ontologyService.findTerms( settings.getQuery(), 5000,
                        Math.max( timeoutMs - watch.getTime(), 0L ), TimeUnit.MILLISECONDS );
            } catch ( LuceneParseSearchException e ) {
                log.debug( String.format( "Failed to parse '%s': %s.", settings.getQuery(), ExceptionUtils.getRootCauseMessage( e ) ), e );
                // reattempt it without escaped
                matchingTerms = ontologyService.findTerms( LuceneQueryUtils.escape( settings.getQuery() ), 5000,
                        Math.max( timeoutMs - watch.getTime(), 0L ), TimeUnit.MILLISECONDS );
            }
            matchingTerms.stream()
                    // ignore bnodes
                    .filter( t -> t.getResult().getUri() != null )
                    // the only possibility for being no score is that the query is an URI and the search didn't go through
                    // the search index
                    .map( t -> new OntologyResult( t.getResult(), t.getScore() ) )
                    .forEach( ontologyResults::add );
        }
        searchMs = watch.getTime() - searchMs;

        // Search for child terms.
        childrenMs = watch.getTime();
        if ( !matchingTerms.isEmpty() && timeoutMs > 0 ) {
            // TODO: move this logic in baseCode, this can be done far more efficiently with Jena API
            // we don't know parent/child relation, so the best we can do is assigne the average full-text score
            double avgScore = matchingTerms.stream()
                    .mapToDouble( OntologySearchResult::getScore )
                    .filter( s -> s != EXACT_MATCH_SCORE )
                    .average()
                    .orElse( 0 );
            try {
                ontologyService.getChildren( matchingTerms.stream().map( OntologySearchResult::getResult ).collect( Collectors.toSet() ), false, true, Math.max( timeoutMs - watch.getTime(), 1L ), TimeUnit.MILLISECONDS )
                        .stream()
                        // ignore bnodes
                        .filter( c -> c.getUri() != null )
                        // small penalty for being indirectly matched
                        .map( c -> new OntologyResult( c, INDIRECT_HIT_PENALTY * avgScore ) )
                        // if a children was already in terms, it will not be added again and thus its original score will
                        // be reflected in the results
                        .forEach( ontologyResults::add );
            } catch ( TimeoutException e ) {
                if ( settings.getMode().equals( SearchSettings.SearchMode.FAST ) ) {
                    // in fast mode, we don't really care if the inference is incomplete
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
        // URIs are case-insensitive in the database, so should be the mapping to labels
        Collection<String> uris = new HashSet<>();
        Map<String, String> uri2value = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        Map<String, Double> uri2score = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

        // rescale the scores in a [0, 1] range
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

        findExpressionExperimentsByUris( uris, uri2value, uri2score, settings, context, results );
    }

    private void findExpressionExperimentsByUris( Collection<String> uris, Map<String, String> uri2value, Map<String, Double> uri2score, SearchSettings settings, SearchContext context, SearchResultSet<ExpressionExperiment> results ) {
        if ( isFilled( results, settings ) )
            return;

        // ranking results by level is costly
        boolean rankByLevel = settings.getMode().equals( SearchSettings.SearchMode.ACCURATE );

        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = characteristicService.findExperimentsByUris( uris, settings.getTaxonConstraint(), getLimit( results, settings ), settings.isFillResults(), rankByLevel );

        // collect all direct tags
        if ( hits.containsKey( ExpressionExperiment.class ) ) {
            addExperimentsByUrisHits( hits.get( ExpressionExperiment.class ), "characteristics.valueUri", 1.0, uri2value, uri2score, context.getHighlighter(), results );
        }

        // collect experimental design-related terms
        if ( hits.containsKey( ExperimentalDesign.class ) ) {
            addExperimentsByUrisHits( hits.get( ExperimentalDesign.class ), "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri", 0.9, uri2value, uri2score, context.getHighlighter(), results );
        }

        // collect samples-related terms
        if ( hits.containsKey( BioMaterial.class ) ) {
            addExperimentsByUrisHits( hits.get( BioMaterial.class ), "bioAssays.sampleUsed.characteristics.valueUri", 0.9, uri2value, uri2score, context.getHighlighter(), results );
        }
    }

    private void addExperimentsByUrisHits( Map<String, Set<ExpressionExperiment>> hits, String field, double scoreMultiplier, Map<String, String> uri2value, Map<String, Double> uri2score, @Nullable Highlighter highlighter, SearchResultSet<ExpressionExperiment> results ) {
        for ( Map.Entry<String, Set<ExpressionExperiment>> entry : hits.entrySet() ) {
            String uri = entry.getKey();
            String value = uri2value.get( uri );
            for ( ExpressionExperiment ee : entry.getValue() ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, scoreMultiplier * uri2score.getOrDefault( uri, 0.0 ),
                        highlightTerm( highlighter, uri, value, field ),
                        String.format( "CharacteristicService.findExperimentsByUris with term [%s](%s)", value, uri ) ) );
            }
        }
    }

    /**
     * Obtain a limit suitable for the given search results and settings.
     *
     * @return the difference between the maximum results and the collection size or -1 if the settings are for
     * unlimited results
     * @throws IllegalArgumentException if the search results are already fully filled as per {@link SearchSettingsUtils#isFilled(Collection, SearchSettings)}
     */
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
        // use the fragment
        if ( !StringUtils.isEmpty( termUri.getFragment() ) ) {
            return partToTerm( termUri.getFragment() );
        }
        // pick the last non-empty segment
        for ( int i = segments.length - 1; i >= 0; i-- ) {
            if ( !StringUtils.isEmpty( segments[i] ) ) {
                return partToTerm( segments[i] );
            }
        }
        // as a last resort, return the parsed URI
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

    /**
     * Highlight a given ontology term.
     * @return a highlight, or null if no provider is set or the provider returns null
     */
    @Nullable
    public Map<String, String> highlightTerm( @Nullable Highlighter highlighter, String termUri, String termLabel, String field ) {
        if ( highlighter instanceof OntologyHighlighter ) {
            return ( ( OntologyHighlighter ) highlighter ).highlightTerm( termUri, termLabel, field );
        } else {
            return null;
        }
    }
}
