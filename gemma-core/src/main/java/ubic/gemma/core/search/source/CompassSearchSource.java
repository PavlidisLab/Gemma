package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.compass.core.*;
import org.compass.core.engine.SearchEngineQueryParseException;
import org.compass.core.mapping.CompassMapping;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.mapping.osem.ClassMapping;
import org.compass.core.mapping.osem.ComponentMapping;
import org.compass.core.spi.InternalCompassSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.*;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Compass-based search source.
 *
 * @author klc
 * @author paul
 * @author keshav
 * @author poirigui
 */
@Component
@CommonsLog
public class CompassSearchSource implements SearchSource {

    /**
     * Penalty applied to score on hits for entities that derive from an association. For example, if a hit to an EE
     * came from text associated with one of its biomaterials,
     * the score is penalized by this amount (or, this is just the actual score used)
     */
    private static final double INDIRECT_DB_HIT_PENALTY = 0.8;

    /**
     * Penalty applied to all 'index' hits.
     */
    private static final double COMPASS_HIT_SCORE_PENALTY_FACTOR = 0.5;

    /**
     * Setting this too high is unnecessary as characteristic searches are more accurate (for experiments)
     */
    private static final int MAX_LUCENE_HITS = 300;

    private static final int MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH = 2;

    @Autowired
    @Qualifier("compassArray")
    private Compass compassArray;
    @Autowired
    @Qualifier("compassBibliographic")
    private Compass compassBibliographic;
    @Autowired
    @Qualifier("compassBiosequence")
    private Compass compassBiosequence;
    @Autowired
    @Qualifier("compassExperimentSet")
    private Compass compassExperimentSet;
    @Autowired
    @Qualifier("compassExpression")
    private Compass compassExpression;
    @Autowired
    @Qualifier("compassGene")
    private Compass compassGene;
    @Autowired
    @Qualifier("compassGeneSet")
    private Compass compassGeneSet;
    @Autowired
    @Qualifier("compassProbe")
    private Compass compassProbe;
    @Autowired
    private BioSequenceService bioSequenceService;
    @Autowired
    @Qualifier("valueObjectConversionService")
    private ConversionService valueObjectConversionService;

    /**
     * A Compass search on array designs.
     *
     * @return {@link Collection}
     */
    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassArray, settings, ArrayDesign.class );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassBibliographic, settings, BibliographicReference.class );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassExperimentSet, settings, ExpressionExperimentSet.class );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassBiosequence, settings, BioSequence.class );
    }

    /**
     * A compass backed search that finds {@link BioSequence} that match the search string. Searches the gene and probe
     * indexes for matches then converts those results to {@link BioSequence}
     *
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The {@link BioSequence}
     *                                  for the genes are added to the final results.
     */
    @Override
    public Collection<SearchResult<?>> searchBioSequenceAndGene( SearchSettings settings,
            @Nullable Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        Collection<SearchResult<?>> results = new HashSet<>( this.compassSearch( compassBiosequence, settings, BioSequence.class ) );

        // FIXME: incorporate the genes in the biosequence results (breaks generics)
        Collection<SearchResult<Gene>> geneResults;
        if ( previousGeneSearchResults == null ) {
            CompassSearchSource.log.debug( "Biosequence Search:  running gene search with " + settings.getQuery() );
            geneResults = this.searchGene( settings );
        } else {
            CompassSearchSource.log.debug( "Biosequence Search:  using previous results" );
            geneResults = previousGeneSearchResults;
        }

        Map<Gene, SearchResult<Gene>> genes = geneResults.stream()
                .filter( sr -> sr.getResultObject() != null )
                .collect( Collectors.toMap( SearchResult::getResultObject, identity() ) );

        Map<Gene, Collection<BioSequence>> seqsFromDb = bioSequenceService.findByGenes( genes.keySet() );
        for ( Gene gene : seqsFromDb.keySet() ) {
            SearchResult<?> compassHitDerivedFrom = genes.get( gene );
            results.addAll( seqsFromDb.get( gene ).stream()
                    .filter( Objects::nonNull )
                    .filter( entity -> entity.getId() != null )
                    .map( entity -> SearchResult.from( BioSequence.class, entity, compassHitDerivedFrom.getScore() * CompassSearchSource.INDIRECT_DB_HIT_PENALTY, compassHitDerivedFrom.getHighlightedText(), compassHitDerivedFrom.getSource() ) )
                    .collect( Collectors.toList() ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassProbe, settings, CompositeSequence.class );
    }

    @Override
    public Collection<SearchResult<?>> searchCompositeSequenceAndGene( final SearchSettings settings ) throws SearchException {
        return new ArrayList<>( this.searchBioSequence( settings ) );
    }

    /**
     * A compass search on expressionExperiments. The results are filtered by taxon so that our limits are meaningfully
     * applied to next stages of the querying.
     *
     * @return {@link Collection}
     */
    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException {
        Collection<SearchResult<ExpressionExperiment>> unfilteredResults = this.compassSearch( compassExpression, settings, ExpressionExperiment.class );
        Taxon t = settings.getTaxon();
        if ( t == null || unfilteredResults.isEmpty() )
            return unfilteredResults;

        Collection<SearchResult<ExpressionExperiment>> filteredResults = new SearchResultSet<>();
        Collection<Long> eeIds = unfilteredResults.stream().map( SearchResult::getResultId ).collect( Collectors.toSet() );
        for ( SearchResult<ExpressionExperiment> sr : unfilteredResults ) {
            if ( eeIds.contains( sr.getResultId() ) ) {
                filteredResults.add( sr );
            }
        }
        if ( filteredResults.size() < unfilteredResults.size() ) {
            log.debug( "Filtered for taxon = " + t.getCommonName() + ", removed " + ( unfilteredResults.size()
                    - filteredResults.size() ) + " results" );
        }
        return filteredResults;
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( final SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassGene, settings, Gene.class );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassGeneSet, settings, GeneSet.class );
    }

    /**
     * Generic method for searching Lucene indices for entities (excluding ontology terms, which use the OntologySearch)
     */
    private <T extends Identifiable> Set<SearchResult<T>> compassSearch( Compass compass, final SearchSettings settings, Class<T> clazz ) throws SearchException {
        if ( !settings.isUseIndices() )
            return Collections.emptySet();
        try {
            StopWatch timer = StopWatch.createStarted();
            Set<SearchResult<T>> searchResults = new CompassTemplate( compass )
                    .execute( session -> performSearch( settings, session, clazz ) );
            log.debug( String.format( "Compass search via %s with %s yielded %d hits in %d ms.",
                    compass.getSettings().getSetting( "compass.name" ),
                    settings,
                    searchResults.size(),
                    timer.getTime() ) );
            return searchResults;
        } catch ( SearchEngineQueryParseException e ) {
            throw new CompassSearchException( "Compass failed to parse the search query.", e );
        } catch ( CompassException e ) {
            // FIXME: there's nothing we can do here and bubbling the error would abort the search altogether
            log.warn( String.format( "Compass search via %s failed due to a cause unrelated to the query syntax. No results will be returned.",
                    compass.getSettings().getSetting( "compass.name" ) ), e );
            return Collections.emptySet();
        }
    }

    /**
     * Runs inside Compass transaction
     */
    private <T extends Identifiable> Set<SearchResult<T>> performSearch( SearchSettings settings, CompassSession session, Class<T> clazz ) {
        StopWatch watch = new StopWatch();
        watch.start();

        // some strings of size 1 cause lucene to barf, and they were slipping through in multi-term queries, get rid of them
        String enhancedQuery = Arrays.stream( settings.getQuery().split( "\\s+" ) )
                .filter( t -> t.length() > 1 )
                .collect( Collectors.joining( " " ) );

        // exclude non-word characters when measuring query length
        if ( enhancedQuery.replaceAll( "\\W", "" ).length() < CompassSearchSource.MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH ) {
            log.debug( String.format( "Query %s does not contain enough word-like character for free-text search.", settings ) );
            return Collections.emptySet();
        }

        CompassQuery compassQuery = session.queryBuilder().queryString( enhancedQuery ).toQuery();
        CompassSearchSource.log.debug( "Parsed query: " + compassQuery );

        CompassHits hits = compassQuery.hits();

        // Note that hits come in decreasing score order, so it makes sense to limit ourselves to a few first results
        int maxHits = Math.min( CompassSearchSource.MAX_LUCENE_HITS, hits.getLength() );

        if ( settings.getMaxResults() > 0 ) {
            maxHits = Math.min( maxHits, settings.getMaxResults() );
        }

        // highlighting, if desired & supported by Compass (always!)
        if ( settings.isDoHighlighting() ) {
            if ( session instanceof InternalCompassSession ) {
                // always ...
                CompassMapping mapping = ( ( InternalCompassSession ) session ).getMapping();
                ResourceMapping[] rootMappings = mapping.getRootMappings();
                // should only be one rootMapping.
                processHits( hits, rootMappings, maxHits );
            }
        }

        String source = String.format( "%s with '%s'", session.getSettings().getSetting( "compass.name" ), compassQuery );

        Set<SearchResult<T>> results = new SearchResultSet<>( maxHits );
        for ( int i = 0; i < maxHits; i++ ) {
            Object resultObject = hits.data( i );

            // check if result object is of expected type
            if ( !clazz.isAssignableFrom( resultObject.getClass() ) ) {
                log.warn( String.format( "Incompatible Compass result with type %s (expected %s) from %s with %s.",
                        resultObject.getClass().getName(), clazz.getName(), source, compassQuery ) );
                continue;
            }

            double score = Double.isNaN( hits.score( i ) ) ? 1.0 : hits.score( i );
            String ht = null;
            if ( settings.isDoHighlighting() && hits.highlightedText( i ) != null ) {
                ht = hits.highlightedText( i ).getHighlightedText();
            }

            //noinspection unchecked
            results.add( SearchResult.from( clazz, ( ( T ) resultObject ).getId(), score * CompassSearchSource.COMPASS_HIT_SCORE_PENALTY_FACTOR, ht, source ) );
        }

        if ( settings.isFillResults() ) {
            fillSearchResults( results, clazz );
        }

        watch.stop();

        String message = String.format( "Getting %d Lucene hits for %s (parsed as %s) from %s took %d ms",
                hits.getLength(), enhancedQuery, compassQuery, session.getSettings().getSetting( "compass.name" ), watch.getTime() );
        if ( watch.getTime() > 5000 ) {
            CompassSearchSource.log.warn( "***** Slow Lucene Index Search!  " + message );
        } else {
            CompassSearchSource.log.debug( message );
        }

        return results;
    }

    /**
     * Recursively cache the highlighted text. This must be done during the search transaction.
     *
     * @param hits          hits to cache
     * @param givenMappings on first call, the root mapping(s)
     * @param maxHits       maximum hits to cache
     */
    private static void processHits( CompassHits hits, ResourceMapping[] givenMappings, int maxHits ) {
        for ( ResourceMapping resourceMapping : givenMappings ) {
            Iterator<Mapping> mappings = resourceMapping.mappingsIt(); // one for each property.
            while ( mappings.hasNext() ) {
                Mapping m = mappings.next();
                if ( m instanceof ComponentMapping ) {
                    ClassMapping[] refClassMappings = ( ( ComponentMapping ) m ).getRefClassMappings();
                    processHits( hits, refClassMappings, maxHits );
                } else { // should be a ClassPropertyMapping
                    String name = m.getName();
                    for ( int i = 0; i < maxHits; i++ ) {
                        try {
                            String frag = hits.highlighter( i ).fragment( name );
                            if ( log.isDebugEnabled() )
                                log.debug( "Highlighted fragment: " + frag + " for " + hits.hit( i ) );
                        } catch ( Exception e ) {
                            break; // skip this property entirely for all hits ...
                        }
                    }
                }
            }
        }
    }

    private <T extends Identifiable> void fillSearchResults( Collection<SearchResult<T>> results, Class<T> clazz ) {
        StopWatch timer = StopWatch.createStarted();

        Set<Long> ids = results.stream()
                .map( SearchResult::getResultId )
                .collect( Collectors.toSet() );

        //noinspection unchecked
        List<T> entities = ( List<T> ) valueObjectConversionService.convert( ids,
                TypeDescriptor.collection( Set.class, TypeDescriptor.valueOf( Long.class ) ),
                TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( clazz ) ) );

        Map<Long, T> entitiesById = EntityUtils.getIdMap( entities );

        Iterator<SearchResult<T>> it = results.iterator();
        while ( it.hasNext() ) {
            SearchResult<T> sr = it.next();
            if ( entitiesById.containsKey( sr.getResultId() ) ) {
                sr.setResultObject( entitiesById.get( sr.getResultId() ) );
            } else {
                it.remove();
            }
        }

        if ( timer.getTime() > 100 ) {
            log.warn( String.format( "Filling %d %s results took %d ms", ids.size(), clazz.getSimpleName(), timer.getTime() ) );
        }
    }
}
