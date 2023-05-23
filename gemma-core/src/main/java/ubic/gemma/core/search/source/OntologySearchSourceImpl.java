package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.core.search.BaseCodeOntologySearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.CharacteristicService;

import java.util.*;

@Component
@CommonsLog
public class OntologySearchSourceImpl implements OntologySearchSource {

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
    @Cacheable("OntologySearchSource.searchExpressionExperiment")
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException {
        // overall timer
        StopWatch watch = StopWatch.createStarted();
        // per-step timer
        StopWatch timer = StopWatch.create();

        Set<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>();

        Collection<OntologyResource> terms = new HashSet<>();

        // Phase 1: We first search for individuals.
        Collection<OntologyIndividual> individuals;
        try {
            timer.start();
            individuals = ontologyService.findIndividuals( settings.getQuery() );
            terms.addAll( individuals );
        } catch ( OntologySearchException e ) {
            throw new BaseCodeOntologySearchException( e );
        } finally {
            timer.stop();
        }

        if ( timer.getTime() > 100 ) {
            log.warn( String.format( "Found %d terms (individual) matching '%s' in %d ms",
                    individuals.size(), settings.getQuery(), timer.getTime() ) );
        }

        // Phase 2: Search ontology classes matches to the query
        timer.reset();
        timer.start();
        Collection<OntologyTerm> matchingTerms;
        try {
            matchingTerms = ontologyService.findTerms( settings.getQuery() );
            terms.addAll( matchingTerms );
            timer.stop();
        } catch ( OntologySearchException e ) {
            throw new BaseCodeOntologySearchException( "Failed to find terms via ontology search.", e );
        }

        if ( timer.getTime() > 100 ) {
            log
                    .warn( String.format( "Found %d ontology classes matching '%s' in %d ms",
                            matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
        }

        /*
         * Search for child terms.
         */
        if ( !matchingTerms.isEmpty() ) {
            // TODO: move this logic in baseCode, this can be done far more efficiently with Jena API
            timer.reset();
            timer.start();
            terms.addAll( ontologyService.getChildren( matchingTerms, false, true ) );
            timer.stop();

            if ( timer.getTime() > 200 ) {
                log.warn(
                        String.format( "Found %d ontology subclasses or related terms for %d terms matching '%s' in %d ms",
                                terms.size() - matchingTerms.size(), matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
            }
        }

        timer.reset();
        timer.start();
        findExperimentsByTerms( terms, results, settings );
        timer.stop();

        if ( timer.getTime() > 100 ) {
            log
                    .warn( String.format( "Retrieved %d datasets via %d characteristics in %d ms",
                            results.size(), terms.size(), timer.getTime() ) );
        }

        String message = String.format( "Found %d datasets by %d characteristic URIs for '%s' in %d ms",
                results.size(), terms.size(), settings.getQuery(), watch.getTime() );
        if ( watch.getTime() > 300 ) {
            log.warn( message );
        } else {
            log.debug( message );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperimentByUris( SearchSettings settings, Collection<String> uris, Map<String, String> uri2value ) {
        Set<SearchResult<ExpressionExperiment>> results = new HashSet<>();
        findExpressionExperimentsByUris( uris, results, 1.0, uri2value, settings );
        return results;
    }

    private void findExperimentsByTerms( Collection<? extends OntologyResource> individuals, Set<SearchResult<ExpressionExperiment>> results, SearchSettings settings ) {
        // URIs are case-insensitive in the database, so should be the mapping to labels
        Collection<String> uris = new HashSet<>();
        Map<String, String> uri2value = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

        if ( settings.isTermQuery() ) {
            String query = settings.getQuery();
            uris.add( query );
            // this will be replaced with a proper label if found in the ontology
            uri2value.put( query, OntologyUtils.getLabelFromTermUri( query ) );
        }

        for ( OntologyResource individual : individuals ) {
            // bnodes can have null URIs, how annoying...
            if ( individual.getUri() != null ) {
                uris.add( individual.getUri() );
                uri2value.put( individual.getUri(), individual.getLabel() );
            }
        }

        findExpressionExperimentsByUris( uris, results, 0.9, uri2value, settings );
    }

    private void findExpressionExperimentsByUris( Collection<String> uris, Set<SearchResult<ExpressionExperiment>> results, double score, Map<String, String> uri2value, SearchSettings settings ) {
        if ( isFilled( results, settings ) )
            return;

        // ranking results by level is costly
        boolean rankByLevel = settings.getMode().equals( SearchSettings.SearchMode.ACCURATE );

        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = characteristicService.findExperimentsByUris( uris, settings.getTaxon(), getLimit( results, settings ), settings.isFillResults(), rankByLevel );

        // collect all direct tags
        addExperimentsByUrisHits( hits, results, ExpressionExperiment.class, score, uri2value, settings );

        // collect experimental design-related terms
        addExperimentsByUrisHits( hits, results, ExperimentalDesign.class, 0.9 * score, uri2value, settings );

        // collect samples-related terms
        addExperimentsByUrisHits( hits, results, BioMaterial.class, 0.9 * score, uri2value, settings );
    }

    private void addExperimentsByUrisHits( Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits, Set<SearchResult<ExpressionExperiment>> results, Class<? extends Identifiable> clazz, double score, Map<String, String> uri2value, SearchSettings settings ) {
        Map<String, Set<ExpressionExperiment>> specificHits = hits.get( clazz );
        if ( specificHits == null )
            return;
        for ( Map.Entry<String, Set<ExpressionExperiment>> entry : specificHits.entrySet() ) {
            String uri = entry.getKey();
            String value = uri2value.get( uri );
            for ( ExpressionExperiment ee : entry.getValue() ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, score,
                        settings.highlightTerm( uri, value, clazz ),
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
}
