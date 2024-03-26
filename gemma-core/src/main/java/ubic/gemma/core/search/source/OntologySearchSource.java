package ubic.gemma.core.search.source;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchResultSet;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.CharacteristicService;

import java.net.URI;
import java.util.*;

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

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    /**
     * Perform a Experiment search based on annotations (anchored in ontology terms) - it does not have to be one word,
     * it could be "parkinson's disease"; it can also be a URI.
     *
     * @return collection of SearchResults (Experiments)
     */
    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException {
        // overall timer
        StopWatch watch = StopWatch.createStarted();
        // per-step timer
        StopWatch timer = StopWatch.create();

        Set<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>();

        Collection<OntologyResult> ontologyResults = new HashSet<>();

        // if the query is a term, find it directly
        Collection<OntologyTerm> matchingTerms;
        if ( settings.isTermQuery() ) {
            String termUri = settings.getQuery();
            OntologyResult resource;
            OntologyTerm r2 = ontologyService.getTerm( termUri );
            if ( r2 != null ) {
                assert r2.getUri() != null;
                resource = new OntologyResult( r2, EXACT_MATCH_SCORE );
                matchingTerms = Collections.singleton( r2 );
            } else {
                // attempt to guess a label from othe database
                Characteristic c = characteristicService.findBestByUri( termUri );
                if ( c != null ) {
                    assert c.getValueUri() != null;
                    resource = new OntologyResult( c.getValueUri(), c.getValue(), EXACT_MATCH_SCORE );
                } else {
                    resource = new OntologyResult( termUri, getLabelFromTermUri( termUri ), EXACT_MATCH_SCORE );
                }
                matchingTerms = Collections.emptySet();
            }
            ontologyResults.add( resource );
        } else {
            // Search ontology classes matches to the full-text query
            timer.reset();
            timer.start();
            matchingTerms = ontologyService.findTerms( settings.getQuery() );
            matchingTerms.stream()
                    // ignore bnodes
                    .filter( t -> t.getUri() != null )
                    // the only possibility for being no score is that the query is an URI and the search didn't go through
                    // the search index
                    .map( t -> new OntologyResult( t, t.getScore() != null ? t.getScore() : EXACT_MATCH_SCORE ) )
                    .forEach( ontologyResults::add );
            timer.stop();
            if ( timer.getTime() > 100 ) {
                log.warn( String.format( "Found %d ontology classes matching '%s' in %d ms",
                        matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
            }
        }

        // Search for child terms.
        if ( !matchingTerms.isEmpty() ) {
            // TODO: move this logic in baseCode, this can be done far more efficiently with Jena API
            timer.reset();
            timer.start();
            // we don't know parent/child relation, so the best we can do is assigne the average full-text score
            double avgScore = matchingTerms.stream()
                    .mapToDouble( t -> t.getScore() != null ? t.getScore() : 0 )
                    .filter( s -> s != EXACT_MATCH_SCORE )
                    .average()
                    .orElse( 0 );
            ontologyService.getChildren( matchingTerms, false, true )
                    .stream()
                    // ignore bnodes
                    .filter( c -> c.getUri() != null )
                    // small penalty for being indirectly matched
                    .map( c -> new OntologyResult( c, INDIRECT_HIT_PENALTY * avgScore ) )
                    // if a children was already in terms, it will not be added again and thus its original score will
                    // be reflected in the results
                    .forEach( ontologyResults::add );
            timer.stop();

            if ( timer.getTime() > 200 ) {
                log.warn( String.format( "Found %d ontology subclasses or related terms for %d terms matching '%s' in %d ms",
                        ontologyResults.size() - matchingTerms.size(), matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
            }
        }

        timer.reset();
        timer.start();
        findExperimentsByOntologyResults( ontologyResults, settings, results );
        timer.stop();

        if ( timer.getTime() > 100 ) {
            log.warn( String.format( "Retrieved %d datasets via %d characteristics in %d ms",
                    results.size(), ontologyResults.size(), timer.getTime() ) );
        }

        String message = String.format( "Found %d datasets by %d characteristic URIs for '%s' in %d ms",
                results.size(), ontologyResults.size(), settings.getQuery(), watch.getTime() );
        if ( watch.getTime() > 300 ) {
            log.warn( message );
        } else {
            log.debug( message );
        }

        return results;
    }

    private void findExperimentsByOntologyResults( Collection<OntologyResult> terms, SearchSettings settings, Set<SearchResult<ExpressionExperiment>> results ) {
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

        for ( OntologyResult term : terms ) {
            uris.add( term.getUri() );
            uri2value.put( term.getUri(), term.getLabel() );
            if ( term.getScore() == EXACT_MATCH_SCORE ) {
                uri2score.put( term.getUri(), 1.0 );
            } else if ( summaryStatistics.getMax() == summaryStatistics.getMin() ) {
                uri2score.put( term.getUri(), FULL_TEXT_SCORE_PENALTY );
            } else {
                uri2score.put( term.getUri(), FULL_TEXT_SCORE_PENALTY * ( term.getScore() - summaryStatistics.getMin() ) / ( summaryStatistics.getMax() - summaryStatistics.getMin() ) );
            }
        }

        findExpressionExperimentsByUris( uris, uri2value, uri2score, settings, results );
    }

    private void findExpressionExperimentsByUris( Collection<String> uris, Map<String, String> uri2value, Map<String, Double> uri2score, SearchSettings settings, Set<SearchResult<ExpressionExperiment>> results ) {
        if ( isFilled( results, settings ) )
            return;

        // ranking results by level is costly
        boolean rankByLevel = settings.getMode().equals( SearchSettings.SearchMode.ACCURATE );

        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = characteristicService.findExperimentsByUris( uris, settings.getTaxon(), getLimit( results, settings ), settings.isFillResults(), rankByLevel );

        // collect all direct tags
        if ( hits.containsKey( ExpressionExperiment.class ) ) {
            addExperimentsByUrisHits( hits.get( ExpressionExperiment.class ), "characteristics.valueUri", 1.0, uri2value, uri2score, settings, results );
        }

        // collect experimental design-related terms
        if ( hits.containsKey( ExperimentalDesign.class ) ) {
            addExperimentsByUrisHits( hits.get( ExperimentalDesign.class ), "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri", 0.9, uri2value, uri2score, settings, results );
        }

        // collect samples-related terms
        if ( hits.containsKey( BioMaterial.class ) ) {
            addExperimentsByUrisHits( hits.get( BioMaterial.class ), "bioAssays.sampleUsed.characteristics.valueUri", 0.9, uri2value, uri2score, settings, results );
        }
    }

    private void addExperimentsByUrisHits( Map<String, Set<ExpressionExperiment>> hits, String field, double scoreMultiplier, Map<String, String> uri2value, Map<String, Double> uri2score, SearchSettings settings, Set<SearchResult<ExpressionExperiment>> results ) {
        for ( Map.Entry<String, Set<ExpressionExperiment>> entry : hits.entrySet() ) {
            String uri = entry.getKey();
            String value = uri2value.get( uri );
            for ( ExpressionExperiment ee : entry.getValue() ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, scoreMultiplier * uri2score.getOrDefault( uri, 0.0 ),
                        settings.highlightTerm( uri, value, field ),
                        String.format( "CharacteristicService.findExperimentsByUris with term [%s](%s)", value, uri ) ) );
            }
        }
    }

    /**
     * Check if a collection of search results is already filled.
     *
     * @return true if the search results are filled and cannot accept more results, false otherwise
     */
    private static <T extends Identifiable> boolean isFilled( Collection<SearchResult<T>> results, SearchSettings settings ) {
        return settings.getMaxResults() > 0 && results.size() >= settings.getMaxResults();
    }

    /**
     * Obtain a limit suitable for the given search results and settings.
     *
     * @return the difference between the maximum results and the collection size or -1 if the settings are for
     * unlimited results
     * @throws IllegalArgumentException if the search results are already fully filled as per {@link #isFilled(Collection, SearchSettings)}
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
    static String getLabelFromTermUri( String termUri ) {
        URI components = URI.create( termUri );
        String[] segments = components.getPath().split( "/" );
        // use the fragment
        if ( !StringUtils.isEmpty( components.getFragment() ) ) {
            return partToTerm( components.getFragment() );
        }
        // pick the last non-empty segment
        for ( int i = segments.length - 1; i >= 0; i-- ) {
            if ( !StringUtils.isEmpty( segments[i] ) ) {
                return partToTerm( segments[i] );
            }
        }
        // as a last resort, return the parsed URI
        return components.toString();
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
}
