package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.CompassSearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchSource;
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
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Compass-based search source.
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

    /**
     * Text displayed when we fail to retrieve the information on why a hit was retrieved. This was "[Matching text not
     * available" but we decided that was confusing.
     */
    private static final String HIGHLIGHT_TEXT_NOT_AVAILABLE_MESSAGE = "";

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
    public Collection<SearchResult> searchBioSequenceAndGene( SearchSettings settings,
            Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        Collection<SearchResult> results = new HashSet<>( this.compassSearch( compassBiosequence, settings, BioSequence.class ) );

        // FIXME: incorporate the genes in the biosequence results (breaks generics)
        Collection<SearchResult<Gene>> geneResults;
        if ( previousGeneSearchResults == null ) {
            CompassSearchSource.log.info( "Biosequence Search:  running gene search with " + settings.getQuery() );
            geneResults = this.searchGene( settings );
        } else {
            CompassSearchSource.log.info( "Biosequence Search:  using previous results" );
            geneResults = previousGeneSearchResults;
        }

        Map<Gene, SearchResult<Gene>> genes = geneResults.stream()
                .filter( sr -> sr.getResultObject() != null )
                .collect( Collectors.toMap( SearchResult::getResultObject, identity() ) );

        Map<Gene, Collection<BioSequence>> seqsFromDb = bioSequenceService.findByGenes( genes.keySet() );
        for ( Gene gene : seqsFromDb.keySet() ) {
            List<BioSequence> bs = new ArrayList<>( seqsFromDb.get( gene ) );
            // bioSequenceService.thawRawAndProcessed( bs );
            results.addAll( this.indirectDbHitsToSearchResults( bs, genes.get( gene ) ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassProbe, settings, CompositeSequence.class );
    }

    @Override
    public Collection<SearchResult> searchCompositeSequenceAndGene( final SearchSettings settings ) throws SearchException {
        return this.searchBioSequence( settings ).stream()
                .map( o -> ( SearchResult ) o )
                .collect( Collectors.toList() );
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
        return filterExperimentHitsByTaxon( unfilteredResults, settings.getTaxon() );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( final SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassGene, settings, Gene.class );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        return this.compassSearch( compassGeneSet, settings, GeneSet.class );
    }

    @Override
    public Collection<SearchResult<CharacteristicValueObject>> searchPhenotype( SearchSettings settings ) {
        throw new NotImplementedException( "Searching phenotypes is not supported for the Compass source." );
    }

    /**
     * Generic method for searching Lucene indices for entities (excluding ontology terms, which use the OntologySearch)
     */
    private <T extends Identifiable> Set<SearchResult<T>> compassSearch( Compass bean, final SearchSettings settings, Class<T> clazz ) throws SearchException {

        if ( !settings.getUseIndices() )
            return new HashSet<>();

        Object source = bean.getSettings().getSetting( "compass.name" );
        CompassTemplate template = new CompassTemplate( bean );
        Set<SearchResult<T>> searchResults;
        try {
            searchResults = template.execute( session -> CompassSearchSource.this.performSearch( settings, session, clazz, source ) );
        } catch ( SearchEngineQueryParseException e ) {
            throw new CompassSearchException( "Compass failed to parse the search query.", e );
        } catch ( CompassException e ) {
            // FIXME: there's nothing we can do here and bubbling the error would abort the search altogether
            log.warn( String.format( "Compass search via %s failed due to a cause unrelated to the query syntax. No results will be returned.",
                    bean.getSettings().getSetting( "compass.name" ) ) );
            searchResults = new HashSet<>();
        }

        if ( CompassSearchSource.log.isDebugEnabled() ) {
            CompassSearchSource.log
                    .debug( "Compass search via " + source + " : " + settings
                            + " -> " + searchResults.size() + " hits" );
        }

        return searchResults;
    }

    /**
     * Runs inside Compass transaction
     */
    private <T extends Identifiable> Set<SearchResult<T>> performSearch( SearchSettings settings, CompassSession session, Class<T> clazz, Object source ) {
        StopWatch watch = new StopWatch();
        watch.start();
        String enhancedQuery = settings.getQuery().trim();

        //noinspection ConstantConditions
        if ( StringUtils.isBlank( enhancedQuery )
                || enhancedQuery.length() < CompassSearchSource.MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH
                // FIXME: this is ignored because of the minimum string length
                || enhancedQuery.equals( "*" ) )
            return new HashSet<>();

        CompassQuery compassQuery = session.queryBuilder().queryString( enhancedQuery ).toQuery();
        CompassSearchSource.log.debug( "Parsed query: " + compassQuery );

        CompassHits hits = compassQuery.hits();

        // highlighting, if desired & supported by Compass (always!)
        if ( settings.isDoHighlighting() ) {
            if ( session instanceof InternalCompassSession ) {
                // always ...
                CompassMapping mapping = ( ( InternalCompassSession ) session ).getMapping();
                ResourceMapping[] rootMappings = mapping.getRootMappings();
                // should only be one rootMapping.
                this.process( rootMappings, hits );
            }
        }

        watch.stop();
        if ( watch.getTime() > 100 ) {
            CompassSearchSource.log
                    .info( "Getting " + hits.getLength() + " lucene hits for " + enhancedQuery + " took " + watch
                            .getTime() + " ms" );
        }
        if ( watch.getTime() > 5000 ) {
            CompassSearchSource.log
                    .info( "***** Slow Lucene Index Search!  " + hits.getLength() + " lucene hits for " + enhancedQuery
                            + " took " + watch.getTime() + " ms" );
        }

        return this.getSearchResults( hits, clazz, source );
    }

    /**
     * Recursively cache the highlighted text. This must be done during the search transaction.
     *
     * @param givenMappings on first call, the root mapping(s)
     */
    private void process( ResourceMapping[] givenMappings, CompassHits hits ) {
        for ( ResourceMapping resourceMapping : givenMappings ) {
            Iterator<Mapping> mappings = resourceMapping.mappingsIt(); // one for each property.
            while ( mappings.hasNext() ) {
                Mapping m = mappings.next();

                if ( m instanceof ComponentMapping ) {
                    ClassMapping[] refClassMappings = ( ( ComponentMapping ) m ).getRefClassMappings();
                    this.process( refClassMappings, hits );
                } else { // should be a ClassPropertyMapping
                    String name = m.getName();
                    for ( int i = 0; i < hits.getLength(); i++ ) {
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

    /**
     *
     * @param  hits CompassHits object
     * @return collection of SearchResult. These *do not* contain the actual entities, just their IDs and class.
     */
    private <T extends Identifiable> Set<SearchResult<T>> getSearchResults( CompassHits hits, Class<T> hitsClass, Object source ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Set<SearchResult<T>> results = new HashSet<>();
        /*
         * Note that hits come in decreasing score order.
         */
        int maxHits = Math.min( CompassSearchSource.MAX_LUCENE_HITS, hits.getLength() );
        for ( int i = 0; i < maxHits; i++ ) {
            Object resultObject = hits.data( i );

            // check if result object is of expected type
            if ( !hitsClass.isAssignableFrom( resultObject.getClass() ) ) {
                log.warn( "Incompatible result from compass: " + resultObject );
                continue;
            }

            // FIXME: score is generally (always?) NaN
            double score = hits.score( i );
            if ( Double.isNaN( score ) ) {
                score = 1.0;
            }

            //noinspection unchecked
            SearchResult<T> sr = new SearchResult<>( ( T ) resultObject, source );
            sr.setScore( score * CompassSearchSource.COMPASS_HIT_SCORE_PENALTY_FACTOR );
            sr.setHighlightedText( this.getHighlightedText( hits, i ) );

            /*
             * Always give compass hits a lower score, so they can be differentiated from exact database hits.
             */
            results.add( sr );
        }

        if ( timer.getTime() > 100 ) {
            String message = results.size() + " hits retrieved (out of " + hits.getLength() + " raw hits tested) in "
                    + timer.getTime() + "ms for "
                    + hits.getQuery();
            if ( timer.getTime() > 5000 ) {
                CompassSearchSource.log.warn( message );
            } else {
                CompassSearchSource.log.info( message );
            }
        }

        return results;
    }

    private String getHighlightedText( CompassHits hits, int i ) {
        CompassHighlightedText highlightedText = hits.highlightedText( i );
        if ( highlightedText != null && highlightedText.getHighlightedText() != null ) {
            return highlightedText.getHighlightedText();
        } else {
            return HIGHLIGHT_TEXT_NOT_AVAILABLE_MESSAGE;
        }
    }

    private Collection<SearchResult<ExpressionExperiment>> filterExperimentHitsByTaxon
            ( Collection<SearchResult<ExpressionExperiment>> unfilteredResults,
                    Taxon t ) {
        if ( t == null || unfilteredResults.isEmpty() )
            return unfilteredResults;

        Collection<SearchResult<ExpressionExperiment>> filteredResults = new HashSet<>();
        Collection<Long> eeIds = unfilteredResults.stream().map( SearchResult::getResultId ).collect( Collectors.toSet() );
        for ( SearchResult<ExpressionExperiment> sr : unfilteredResults ) {
            if ( eeIds.contains( sr.getResultId() ) ) {
                filteredResults.add( sr );
            }
        }
        if ( filteredResults.size() < unfilteredResults.size() ) {
            log.info( "Filtered for taxon = " + t.getCommonName() + ", removed " + ( unfilteredResults.size()
                    - filteredResults.size() ) + " results" );
        }
        return filteredResults;
    }

    private <T extends Identifiable> List<SearchResult<T>> indirectDbHitsToSearchResults( Collection<T> entities, SearchResult<?> compassHitDerivedFrom ) {
        StopWatch timer = StopWatch.createStarted();
        List<SearchResult<T>> results = new ArrayList<>();
        for ( T e : entities ) {
            if ( e == null ) {
                if ( CompassSearchSource.log.isDebugEnabled() )
                    CompassSearchSource.log.debug( "Null search result object" );
                continue;
            }
            if ( e.getId() == null ) {
                log.warn( "Search result object with null ID." );
                continue;
            }
            SearchResult<T> esr = new SearchResult<>( e, compassHitDerivedFrom.getSource() );
            esr.setScore( compassHitDerivedFrom.getScore() * CompassSearchSource.INDIRECT_DB_HIT_PENALTY );
            esr.setHighlightedText( compassHitDerivedFrom.getHighlightedText() );
            results.add( esr );
        }
        if ( timer.getTime() > 1000 ) {
            CompassSearchSource.log.info( "Unpack " + results.size() + " search resultsS: " + timer.getTime() + "ms" );
        }
        return results;
    }

}
