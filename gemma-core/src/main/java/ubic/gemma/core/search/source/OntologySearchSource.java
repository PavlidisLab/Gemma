package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ubic.basecode.ontology.model.*;
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

        Collection<OntologyTerm> terms = new HashSet<>();

        // f the query is a term, find it
        if ( settings.isTermQuery() ) {
            String termUri = settings.getQuery();
            OntologyTerm resource;
            OntologyTerm r2 = ontologyService.getTerm( termUri );
            if ( r2 != null ) {
                resource = new SimpleOntologyTermWithScore( r2, 1.0 );
            } else {
                // attempt to guess a label from othe database
                Characteristic c = characteristicService.findBestByUri( settings.getQuery() );
                if ( c != null ) {
                    assert c.getValueUri() != null;
                    resource = new SimpleOntologyTermWithScore( c.getValueUri(), c.getValue(), 1.0 );
                } else {
                    resource = new SimpleOntologyTermWithScore( termUri, getLabelFromTermUri( termUri ), 1.0 );
                }
            }
            terms.add( resource );
        }

        // Search ontology classes matches to the query
        timer.reset();
        timer.start();
        Collection<OntologyTerm> matchingTerms = ontologyService.findTerms( settings.getQuery() );
        terms.addAll( matchingTerms );
        timer.stop();

        if ( timer.getTime() > 100 ) {
            log.warn( String.format( "Found %d ontology classes matching '%s' in %d ms",
                    matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
        }

        // Search for child terms.
        if ( !matchingTerms.isEmpty() ) {
            // TODO: move this logic in baseCode, this can be done far more efficiently with Jena API
            timer.reset();
            timer.start();
            terms.addAll( ontologyService.getChildren( matchingTerms, false, true ) );
            timer.stop();

            if ( timer.getTime() > 200 ) {
                log.warn( String.format( "Found %d ontology subclasses or related terms for %d terms matching '%s' in %d ms",
                        terms.size() - matchingTerms.size(), matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
            }
        }

        timer.reset();
        timer.start();
        findExperimentsByTerms( terms, settings, results );
        timer.stop();

        if ( timer.getTime() > 100 ) {
            log.warn( String.format( "Retrieved %d datasets via %d characteristics in %d ms",
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

    private void findExperimentsByTerms( Collection<OntologyTerm> terms, SearchSettings settings, Set<SearchResult<ExpressionExperiment>> results ) {
        // URIs are case-insensitive in the database, so should be the mapping to labels
        Collection<String> uris = new HashSet<>();
        Map<String, String> uri2value = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        Map<String, Double> uri2score = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );

        // renormalize the scores in a [0, 1] range
        DoubleSummaryStatistics summaryStatistics = terms.stream()
                .map( OntologyTerm::getScore )
                .filter( Objects::nonNull )
                .mapToDouble( s -> s )
                .summaryStatistics();

        for ( OntologyTerm term : terms ) {
            // bnodes can have null URIs, how annoying...
            if ( term.getUri() != null ) {
                uris.add( term.getUri() );
                uri2value.put( term.getUri(), term.getLabel() );
                uri2score.put( term.getUri(), term.getScore() != null ? term.getScore() / summaryStatistics.getMax() : summaryStatistics.getAverage() / summaryStatistics.getMax() );
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
            addExperimentsByUrisHits( hits.get( ExpressionExperiment.class ), "characteristics.valueUri", 0.9, uri2value, uri2score, settings, results );
        }

        // collect experimental design-related terms
        if ( hits.containsKey( ExperimentalDesign.class ) ) {
            addExperimentsByUrisHits( hits.get( ExperimentalDesign.class ), "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri", 0.9 * 0.9, uri2value, uri2score, settings, results );
        }

        // collect samples-related terms
        if ( hits.containsKey( BioMaterial.class ) ) {
            addExperimentsByUrisHits( hits.get( BioMaterial.class ), "bioAssays.sampleUsed.characteristics.valueUri", 0.9 * 0.9, uri2value, uri2score, settings, results );
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

    /**
     * Simple ontology resource with a score.
     */
    private static class SimpleOntologyTermWithScore implements OntologyTerm {

        private static final Comparator<OntologyResource> COMPARATOR = Comparator
                .comparing( OntologyResource::getScore, Comparator.nullsLast( Comparator.reverseOrder() ) )
                .thenComparing( OntologyResource::getUri, Comparator.nullsLast( Comparator.naturalOrder() ) );

        private final String uri;
        private final String label;
        private final double score;

        private SimpleOntologyTermWithScore( String uri, String label, double score ) {
            this.uri = uri;
            this.label = label;
            this.score = score;
        }

        public SimpleOntologyTermWithScore( OntologyTerm resource, double score ) {
            this.uri = resource.getUri();
            this.label = resource.getLabel();
            this.score = score;
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public boolean isObsolete() {
            return false;
        }

        @Override
        public Double getScore() {
            return score;
        }

        @Override
        public int compareTo( OntologyResource ontologyResource ) {
            return Objects.compare( this, ontologyResource, COMPARATOR );
        }

        @Override
        public Collection<String> getAlternativeIds() {
            return null;
        }

        @Override
        public Collection<AnnotationProperty> getAnnotations() {
            return null;
        }

        @Override
        public Collection<OntologyTerm> getChildren( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
            return null;
        }

        @Override
        public String getComment() {
            return null;
        }

        @Override
        public Collection<OntologyIndividual> getIndividuals( boolean direct ) {
            return null;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public Object getModel() {
            return null;
        }

        @Override
        public Collection<OntologyTerm> getParents( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
            return null;
        }

        @Override
        public Collection<OntologyRestriction> getRestrictions() {
            return null;
        }

        @Override
        public String getTerm() {
            return null;
        }

        @Override
        public boolean isRoot() {
            return false;
        }

        @Override
        public boolean isTermObsolete() {
            return false;
        }
    }
}
