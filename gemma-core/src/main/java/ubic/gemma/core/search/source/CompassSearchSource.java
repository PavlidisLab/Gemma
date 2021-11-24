package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.compass.core.*;
import org.compass.core.mapping.CompassMapping;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.mapping.osem.ClassMapping;
import org.compass.core.mapping.osem.ComponentMapping;
import org.compass.core.spi.InternalCompassSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

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
     * Penalty applied to scores on hits for entities that derive from an association. For example, if a hit to an EE
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
    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * A Compass search on array designs.
     *
     * @return {@link Collection}
     */
    @Override
    public Collection<SearchResult> searchArrayDesign( SearchSettings settings ) {
        return this.compassSearch( compassArray, settings );
    }

    @Override
    public Collection<SearchResult> searchBibliographicReference( SearchSettings settings ) {
        return this.compassSearch( compassBibliographic, settings );
    }

    @Override
    public Collection<SearchResult> searchExperimentSet( SearchSettings settings ) {
        return this.compassSearch( compassExperimentSet, settings );
    }

    /**
     * A compass backed search that finds biosequences that match the search string. Searches the gene and probe indexes
     * for matches then converts those results to biosequences
     *
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *                                  for the genes are added to the final results.
     */
    @Override
    public Collection<SearchResult> searchBioSequence( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults ) {

        Collection<SearchResult> results = this.compassSearch( compassBiosequence, settings );

        Collection<SearchResult> geneResults;
        if ( previousGeneSearchResults == null ) {
            CompassSearchSource.log.info( "Biosequence Search:  running gene search with " + settings.getQuery() );
            geneResults = this.searchGene( settings );
        } else {
            CompassSearchSource.log.info( "Biosequence Search:  using previous results" );
            geneResults = previousGeneSearchResults;
        }

        Map<Gene, SearchResult> genes = new HashMap<>();
        for ( SearchResult sr : geneResults ) {
            Object resultObject = sr.getResultObject();
            if ( Gene.class.isAssignableFrom( resultObject.getClass() ) ) {
                genes.put( ( Gene ) resultObject, sr );
            } else {
                // see bug 1774 -- may not be happening any more.
                CompassSearchSource.log
                        .warn( "Expected a Gene, got a " + resultObject.getClass() + " on query=" + settings
                                .getQuery() );
            }
        }

        Map<Gene, Collection<BioSequence>> seqsFromDb = bioSequenceService.findByGenes( genes.keySet() );
        for ( Gene gene : seqsFromDb.keySet() ) {
            List<BioSequence> bs = new ArrayList<>( seqsFromDb.get( gene ) );
            // bioSequenceService.thawRawAndProcessed( bs );
            results.addAll( this.dbHitsToSearchResult( bs, genes.get( gene ), null ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult> searchCompositeSequence( final SearchSettings settings ) {
        return this.compassSearch( compassProbe, settings );
    }

    /**
     * A compass search on expressionExperiments. The reults are filtered by taxon so that our limits are meaningfully
     * applied to next stages of the querying.
     *
     * @return {@link Collection}
     */
    @Override
    public Collection<SearchResult> searchExpressionExperiment( SearchSettings settings ) {
        Collection<SearchResult> unfilteredResults = this.compassSearch( compassExpression, settings );
        return filterExperimentHitsByTaxon( unfilteredResults, settings.getTaxon() );
    }

    @Override
    public Collection<SearchResult> searchGene( final SearchSettings settings ) {
        return this.compassSearch( compassGene, settings );
    }

    @Override
    public Collection<SearchResult> searchGeneSet( SearchSettings settings ) {
        return this.compassSearch( compassGeneSet, settings );
    }

    @Override
    public Collection<SearchResult> searchPhenotype( SearchSettings settings ) {
        throw new NotImplementedException( "Searching phenotypes is not supported for the Compass source." );
    }

    /**
     * Generic method for searching Lucene indices for entities (excluding ontology terms, which use the OntologySearch)
     */
    private Collection<SearchResult> compassSearch( Compass bean, final SearchSettings settings ) {

        if ( !settings.getUseIndices() )
            return new HashSet<>();

        CompassTemplate template = new CompassTemplate( bean );
        Collection<SearchResult> searchResults = template.execute( new CompassCallback<Collection<SearchResult>>() {
            @Override
            public Collection<SearchResult> doInCompass( CompassSession session ) throws CompassException {
                return CompassSearchSource.this.performSearch( settings, session );
            }
        } );
        if ( CompassSearchSource.log.isDebugEnabled() ) {
            CompassSearchSource.log
                    .debug( "Compass search via " + bean.getSettings().getSetting( "compass.name" ) + " : " + settings
                            + " -> " + searchResults.size() + " hits" );
        }
        return searchResults;
    }

    /**
     * Runs inside Compass transaction
     */
    private Collection<SearchResult> performSearch( SearchSettings settings, CompassSession session ) {
        StopWatch watch = new StopWatch();
        watch.start();
        String enhancedQuery = settings.getQuery().trim();

        if ( StringUtils.isBlank( enhancedQuery )
                || enhancedQuery.length() < CompassSearchSource.MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH
                || enhancedQuery.equals( "*" ) )
            return new ArrayList<>();

        CompassQuery compassQuery = session.queryBuilder().queryString( enhancedQuery ).toQuery();
        CompassSearchSource.log.debug( "Parsed query: " + compassQuery );

        CompassHits hits = compassQuery.hits();

        // highlighting.
        if ( settings.isDoHighlighting() ) {
            if ( session instanceof InternalCompassSession ) { // always ...
                CompassMapping mapping = ( ( InternalCompassSession ) session ).getMapping();
                ResourceMapping[] rootMappings = mapping.getRootMappings();
                // should only be one rootMapping.
                this.process( rootMappings, hits );
            } else {
                //?
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

        return this.getSearchResults( hits );
    }

    /**
     * Recursively cache the highlighted text. This must be done during the search transaction.
     *
     * @param givenMappings on first call, the root mapping(s)
     */
    private void process( ResourceMapping[] givenMappings, CompassHits hits ) {
        for ( ResourceMapping resourceMapping : givenMappings ) {
            Iterator<Mapping> mappings = resourceMapping.mappingsIt(); // one for each property.
            for ( ; mappings.hasNext(); ) {
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
    private Collection<SearchResult> getSearchResults( CompassHits hits ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<SearchResult> results = new HashSet<>();
        /*
         * Note that hits come in decreasing score order.
         */
        for ( int i = 0, len = Math.min( CompassSearchSource.MAX_LUCENE_HITS, hits.getLength() ); i < len; i++ ) {

            SearchResult r = new SearchResult( hits.data( i ) );

            // FIXME: score is generally (always?) NaN
            double score = hits.score( i );
            if ( Double.isNaN( score ) ) {
                score = 1.0;
            }

            /*
             * Always give compass hits a lower score so they can be differentiated from exact database hits.
             */
            r.setScore( score * CompassSearchSource.COMPASS_HIT_SCORE_PENALTY_FACTOR );

            this.getHighlightedText( hits, i, r );

            results.add( r );
        }

        if ( timer.getTime() > 100 ) {
            CompassSearchSource.log.info( results.size() + " hits retrieved (out of " + Math
                    .min( CompassSearchSource.MAX_LUCENE_HITS, hits.getLength() ) + " raw hits tested) in "
                    + timer
                    .getTime()
                    + "ms" );
        }
        if ( timer.getTime() > 5000 ) {
            CompassSearchSource.log
                    .info( "****Extremely long Lucene Search processing! " + results.size() + " hits retrieved (out of "
                            + Math.min( CompassSearchSource.MAX_LUCENE_HITS, hits.getLength() ) + " raw hits tested) in "
                            + timer.getTime() + "ms" );
        }

        return results;
    }

    private void getHighlightedText( CompassHits hits, int i, SearchResult r ) {
        CompassHighlightedText highlightedText = hits.highlightedText( i );
        if ( highlightedText != null && highlightedText.getHighlightedText() != null ) {
            r.setHighlightedText( highlightedText.getHighlightedText() );
        } else {
            r.setHighlightedText( HIGHLIGHT_TEXT_NOT_AVAILABLE_MESSAGE );
        }
    }

    private Collection<SearchResult> filterExperimentHitsByTaxon( Collection<SearchResult> unfilteredResults,
            Taxon t ) {
        if ( t == null || unfilteredResults.isEmpty() )
            return unfilteredResults;

        Collection<SearchResult> filteredResults = new HashSet<>();
        Collection<Long> eeIds = this.expressionExperimentService
                .filterByTaxon( EntityUtils.getIds( unfilteredResults ), t );
        for ( SearchResult sr : unfilteredResults ) {
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

    private List<SearchResult> dbHitsToSearchResult( Collection<?> entities, SearchResult compassHitDerivedFrom,
            String matchText ) {
        StopWatch watch = new StopWatch();
        watch.start();
        StopWatch timer = watch;
        List<SearchResult> results = new ArrayList<>();
        for ( Object e : entities ) {
            if ( e == null ) {
                if ( CompassSearchSource.log.isDebugEnabled() )
                    CompassSearchSource.log.debug( "Null search result object" );
                continue;
            }
            SearchResult esr = this.dbHitToSearchResult( compassHitDerivedFrom, e, matchText );
            results.add( esr );
        }
        if ( timer.getTime() > 1000 ) {
            CompassSearchSource.log.info( "Unpack " + results.size() + " search resultsS: " + timer.getTime() + "ms" );
        }
        return results;
    }

    /**
     * @param text that matched the query (for highlighting)
     */
    private SearchResult dbHitToSearchResult( SearchResult compassHitDerivedFrom, Object e, String text ) {
        SearchResult esr;
        if ( compassHitDerivedFrom != null && text == null ) {
            esr = new SearchResult( e, compassHitDerivedFrom.getScore() * CompassSearchSource.INDIRECT_DB_HIT_PENALTY );
            esr.setHighlightedText( compassHitDerivedFrom.getHighlightedText() );
        } else {
            esr = new SearchResult( e, 1.0, text );
        }
        log.debug( esr );
        return esr;
    }
}
