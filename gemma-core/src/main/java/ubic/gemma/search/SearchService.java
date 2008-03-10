/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

package ubic.gemma.search;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.Compass;
import org.compass.core.CompassCallback;
import org.compass.core.CompassDetachedHits;
import org.compass.core.CompassException;
import org.compass.core.CompassHighlightedText;
import org.compass.core.CompassHighlighter;
import org.compass.core.CompassHit;
import org.compass.core.CompassHits;
import org.compass.core.CompassQuery;
import org.compass.core.CompassSession;
import org.compass.core.CompassTemplate;
import org.compass.core.CompassTransaction;
import org.compass.core.engine.SearchEngineException;
import org.springframework.beans.factory.InitializingBean;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.OntologyIndividual;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.ReflectionUtil;

/**
 * This service is used for performing searches using free text or exact matches to items in the database.
 * <h2>Implementation notes</h2>
 * <p>
 * Internally, there are generally two kinds of searches performed, percise database searches looking for exact matches
 * in the database and compass/lucene searches which look for matches in the stored index.
 * <p>
 * To add more dependencies to this Service edit the applicationContext-search.xml
 * 
 * @author klc
 * @author paul
 * @author keshav
 * @version $Id$
 */
public class SearchService implements InitializingBean {

    private static final double INDIRECT_DB_HIT_PENALTY = 0.8;

    private static final double COMPASS_HIT_SCORE_PENALTY_FACTOR = 0.9;

    /**
     * Global maximum number of search results that will be returned for index searches.
     */
    private static final int MAX_COMPASS_HITS_TO_DETACH = 1000;

    /**
     * How long an item in the cache lasts when it is not accessed.
     */
    private static final int ONTOLOGY_CACHE_TIME_TO_IDLE = 600;

    /**
     * How long after creation before an object is evicted.
     */
    private static final int ONTOLOGY_CACHE_TIME_TO_DIE = 2000;

    Cache childTermCache;

    /**
     * How many term children can stay in memory
     */
    private static final int ONTOLOGY_INFO_CACHE_SIZE = 500;

    private static final int MINIMUM_EE_QUERY_LENGTH = 3;

    private static Log log = LogFactory.getLog( SearchService.class.getName() );

    private Gene2GOAssociationService gene2GOAssociationService;

    private GeneService geneService;

    private GeneProductService geneProductService;

    private CompositeSequenceService compositeSequenceService;

    private ArrayDesignService arrayDesignService;

    private ExpressionExperimentService expressionExperimentService;

    private BibliographicReferenceService bibliographicReferenceService;

    private BioSequenceService bioSequenceService;

    private CharacteristicService characteristicService;

    private OntologyService ontologyService;

    private TaxonService taxonService;

    private Compass geneBean;

    private Compass eeBean;

    private Compass arrayBean;

    private Compass bibliographicReferenceBean;

    // private Compass probeAndBioSequenceBean;

    private Compass biosequenceBean;

    private Compass probeBean;

    public void setBiosequenceBean( Compass biosequenceBean ) {
        this.biosequenceBean = biosequenceBean;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        try {
            CacheManager manager = CacheManager.getInstance();

            if ( manager.cacheExists( "OntologyChildrenCache" ) ) {
                return;
            }

            childTermCache = new Cache( "OntologyChildrenCache", ONTOLOGY_INFO_CACHE_SIZE,
                    MemoryStoreEvictionPolicy.LFU, false, null, false, ONTOLOGY_CACHE_TIME_TO_DIE,
                    ONTOLOGY_CACHE_TIME_TO_IDLE, false, 500, null );

            manager.addCache( childTermCache );
            childTermCache = manager.getCache( "OntologyChildrenCache" );

        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * The results are sorted in order of decreasing score, organized by class. The following objects can be searched
     * for, depending on the configuration of the input object.
     * <ul>
     * <li>Genes
     * <li>ExpressionExperiments
     * <li>CompositeSequences (probes)
     * <li>ArrayDesigns (platforms)
     * <li>Characteristics (e.g., Ontology annotations)
     * <li>BioSequences
     * <li>BibliographicReferences (articles)
     * </ul>
     * 
     * @param settings
     * @return Map of Class to SearchResults. The results are already filtered for security considerations.
     */
    @SuppressWarnings("unchecked")
    public Map<Class, List<SearchResult>> search( SearchSettings settings ) {
        return this.search( settings, true );
    }

    /**
     * @param settings
     * @param fillObjects If false, the entities will not be filled in inside the searchsettings; instead, they will be
     *        nulled (for security purposes). You can then use the id and Class stored in the SearchSettings to load the
     *        entities at your leisure. If true, the entities are loaded in the usual secure fashion. Setting this to
     *        false can be an optimization if all you need is the id.
     * @return
     * @see SearchService.search(SearchSettings settings)
     */
    @SuppressWarnings("unchecked")
    public Map<Class, List<SearchResult>> search( SearchSettings settings, boolean fillObjects ) {
        String searchString = settings.getQuery();

        List<SearchResult> rawResults = new ArrayList<SearchResult>();

        if ( settings.isSearchExperiments() ) {
            Collection<SearchResult> foundEEs = expressionExperimentSearch( settings );
            rawResults.addAll( foundEEs );
        }

        Collection<SearchResult> genes = null;
        if ( settings.isSearchGenes() ) {
            genes = geneSearch( settings );
            accreteResults( rawResults, genes );
        }

        Collection<SearchResult> compositeSequences = null;
        if ( settings.isSearchProbes() ) {
            compositeSequences = compositeSequenceSearch( settings, genes );
            accreteResults( rawResults, compositeSequences );
        }

        if ( settings.isSearchArrays() ) {
            Collection<SearchResult> foundADs = arrayDesignSearch( settings, compositeSequences );
            accreteResults( rawResults, foundADs );
        }

        if ( settings.isSearchBioSequences() ) {
            Collection<SearchResult> bioSequences = bioSequenceSearch( settings, genes );
            accreteResults( rawResults, bioSequences );
        }

        if ( settings.isSearchGenesByGO() ) {
            Collection<SearchResult> ontologyGenes = gene2GOAssociationService.findByGOTerm( searchString, settings
                    .getTaxon() );
            accreteResults( rawResults, ontologyGenes );
        }

        if ( settings.isSearchBibrefs() ) {
            Collection<SearchResult> bibliographicReferences = compassBibliographicReferenceSearch( settings );
            accreteResults( rawResults, bibliographicReferences );
        }

        return getSortedLimitedResults( settings, rawResults, fillObjects );

    }

    /**
     * @param query if empty, all experiments for the taxon are returned; otherwise, we use the search facility.
     * @param taxonId required.
     * @return Collection of ids.
     */
    @SuppressWarnings("unchecked")
    public Collection<Long> searchExpressionExperiments( String query, Long taxonId ) {
        Taxon taxon = taxonService.load( taxonId );
        Collection<Long> eeIds = new HashSet<Long>();
        if ( StringUtils.isNotBlank( query ) ) {
            
            if (query.length() < MINIMUM_EE_QUERY_LENGTH) return eeIds;
            
            List<SearchResult> results = this.search( SearchSettings.ExpressionExperimentSearch( query ), false ).get(
                    ExpressionExperiment.class );
            for ( SearchResult result : results ) {
                eeIds.add( result.getId() );
            }
            if ( taxon != null ) {
                Collection<Long> eeIdsToKeep = new HashSet<Long>();
                Collection<ExpressionExperiment> ees = expressionExperimentService.findByTaxon( taxon );
                for ( ExpressionExperiment ee : ees ) {
                    if ( eeIds.contains( ee.getId() ) ) eeIdsToKeep.add( ee.getId() );
                }
                eeIds.retainAll( eeIdsToKeep );
            }
        } else {
            Collection<ExpressionExperiment> ees = ( taxon != null ) ? expressionExperimentService.findByTaxon( taxon )
                    : expressionExperimentService.loadAll();
            for ( ExpressionExperiment ee : ees ) {
                eeIds.add( ee.getId() );
            }
        }
        return eeIds;
    }

    /**
     * Add results.
     * 
     * @param rawResults To add to
     * @param newResults To be added
     */
    private void accreteResults( List<SearchResult> rawResults, Collection<SearchResult> newResults ) {
        for ( SearchResult sr : newResults ) {
            if ( !rawResults.contains( sr ) ) {
                /*
                 * We do this because we don't want to clobber results, when the same object comes up more than once in
                 * different searches. FIXME - perhaps check if the score of the existing one is lower?
                 */
                rawResults.add( sr );
            }
        }
    }

    /**
     * *
     * 
     * @param searchString
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *        for the genes are added to the final results.
     * @return
     */
    private Collection<SearchResult> bioSequenceSearch( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults ) {
        StopWatch watch = startTiming();

        Collection<SearchResult> allResults = new HashSet<SearchResult>();
        allResults.addAll( compassBioSequenceSearch( settings, previousGeneSearchResults ) );
        allResults.addAll( databaseBioSequenceSearch( settings ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Biosequence search for '" + settings + "' took " + watch.getTime() + " ms " + allResults.size()
                    + " results." );
        return allResults;
    }

    /**
     * @param arrayBean the arrayBean to set
     */
    public void setArrayBean( Compass arrayBean ) {
        this.arrayBean = arrayBean;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bibliographicReferenceBean the bibliographicReferenceBean to set
     */
    public void setBibliographicReferenceBean( Compass bibliographicReferenceBean ) {
        this.bibliographicReferenceBean = bibliographicReferenceBean;
    }

    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    public void setCharacteristicService( CharacteristicService characteristicService ) {
        this.characteristicService = characteristicService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param eeBean the eeBean to set
     */
    public void setEeBean( Compass eeBean ) {
        this.eeBean = eeBean;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param geneBean the geneBean to set
     */
    public void setGeneBean( Compass geneBean ) {
        this.geneBean = geneBean;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setOntologyService( OntologyService ontologyService ) {
        this.ontologyService = ontologyService;
    }

    /**
     * @param probeBean the probeBean to set
     */
    public void setProbeBean( Compass probeBean ) {
        this.probeBean = probeBean;
    }

    /**
     * A general search for array designs.
     * <p>
     * This search does both an database search and a compass search. This is also contains an underlying
     * {@link CompositeSequence} search, returning the {@link ArrayDesign} collection for the given composite sequence
     * search string (the returned collection of array designs does not contain duplicates).
     * 
     * @param searchString
     * @param probeResults Collection of results from a previous CompositeSequence search. Can be null; otherwise used
     *        to avoid a second search for probes. The array designs for the probes are added to the final results.
     * @return
     */
    private Collection<SearchResult> arrayDesignSearch( SearchSettings settings, Collection<SearchResult> probeResults ) {

        StopWatch watch = startTiming();
        String searchString = settings.getQuery();
        Collection<SearchResult> results = new HashSet<SearchResult>();
        ArrayDesign shortNameResult = arrayDesignService.findByShortName( searchString );
        if ( shortNameResult != null ) {
            results.add( new SearchResult( shortNameResult, 1.0 ) );
        } else {
            ArrayDesign nameResult = arrayDesignService.findByName( searchString );
            if ( nameResult != null ) results.add( new SearchResult( nameResult, 1.0 ) );
        }

        results.addAll( compassArrayDesignSearch( settings ) );
        results.addAll( databaseArrayDesignSearch( settings ) );

        Collection<SearchResult> probes = null;
        if ( probeResults == null ) {
            probes = compassCompositeSequenceSearch( settings );
        } else {
            probes = probeResults;
        }

        for ( SearchResult r : probes ) {
            CompositeSequence cs = ( CompositeSequence ) r.getResultObject();
            if ( cs.getArrayDesign() == null ) // This might happen as compass
                // might not have indexed the AD
                // for the CS
                continue;
            results.add( r );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Array Design search for '" + settings + "' took " + watch.getTime() + " ms" );

        return results;
    }

    /**
     * @param settings
     */
    private Collection<SearchResult> characteristicExpressionExperimentSearch( final SearchSettings settings ) {
        Collection<SearchResult> results = new HashSet<SearchResult>();

        Collection<Class> classesToSearch = new HashSet<Class>();
        classesToSearch.add( ExpressionExperiment.class );
        classesToSearch.add( BioMaterial.class );
        classesToSearch.add( FactorValue.class );

        Collection<SearchResult> characterSearchResults = ontologySearchAnnotatedObject( classesToSearch, settings );

        // filter and get parents...
        for ( SearchResult sr : characterSearchResults ) {
            Class resultClass = sr.getResultClass();
            if ( ExpressionExperiment.class.isAssignableFrom( resultClass ) ) {
                results.add( sr );
            } else if ( BioMaterial.class.isAssignableFrom( resultClass ) ) {
                ExpressionExperiment ee = expressionExperimentService.findByBioMaterial( ( BioMaterial ) sr
                        .getResultObject() );
                if ( ee != null ) results.add( new SearchResult( ee, INDIRECT_DB_HIT_PENALTY ) );
            } else if ( FactorValue.class.isAssignableFrom( resultClass ) ) {
                ExpressionExperiment ee = expressionExperimentService.findByFactorValue( ( FactorValue ) sr
                        .getResultObject() );
                if ( ee != null ) results.add( new SearchResult( ee, INDIRECT_DB_HIT_PENALTY ) );
            }
        }

        return results;
    }

    /**
     * Search for the query in ontologies.
     * 
     * @param classes Classes of characteristic-bound entities. For example, to get matching characteristics of
     *        ExpressionExperiments, pass ExpressionExperiments.class in this collection parameter.
     * @param settings
     * @return SearchResults of CharcteristicObjects. Typically to be useful one needs to retrieve the 'parents'
     *         (owners) of those Characteristics.
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> characteristicSearchWithChildren( Collection<Class> classes,
            SearchSettings settings ) {

        StopWatch watch = startTiming();
        Collection<String> characteristicUris = new HashSet<String>();

        Collection<OntologyIndividual> individuals = ontologyService.findIndividuals( settings.getQuery() );
        if ( individuals.size() > 0 || watch.getTime() > 1000 ) {
            log.info( "Found " + individuals.size() + " matching individuals in " + watch.getTime() + "ms" );
        }
        watch.reset();
        watch.start();

        for ( OntologyIndividual term : individuals ) {
            characteristicUris.add( term.getUri() );
        }

        Collection<OntologyTerm> possibleTerms = ontologyService.findTerms( settings.getQuery() );
        if ( ( possibleTerms == null ) || possibleTerms.isEmpty() ) return new HashSet<SearchResult>();

        log.info( "Found " + possibleTerms.size() + " matching classes in " + watch.getTime() + "ms" );
        watch.reset();
        watch.start();

        StopWatch loopWatch = new StopWatch();

        for ( OntologyTerm term : possibleTerms ) {
            loopWatch.start();

            characteristicUris.add( term.getUri() );

            /*
             * getChildren can be very slow for 'high-level' classes like "neoplasm", so we use a cache.
             */
            Collection<OntologyTerm> children = null;
            Element element = this.childTermCache.get( term );
            if ( element == null ) {
                children = term.getChildren( false );
                for ( OntologyTerm child : children ) {
                    characteristicUris.add( child.getUri() );
                }
                // possibly only put large ones in the cache. Small ones are fast enough.
                childTermCache.put( new Element( term, characteristicUris ) );
            } else {
                if ( log.isDebugEnabled() ) log.debug( "Cache hit on " + term );
                characteristicUris = ( Collection<String> ) element.getValue();
            }

            loopWatch.reset();
        }

        if ( watch.getTime() > 1000 ) {
            log.info( "Found " + characteristicUris.size() + " possible matches + child terms in " + watch.getTime()
                    + "ms" );
        }
        watch.reset();
        watch.start();

        /*
         * Find occurrences of these terms in our system. This is fast, so long as there aren't too many.
         */
        Collection<SearchResult> inSystem = dbHitsToSearchResult( new ArrayList<CharacteristicService>(
                characteristicService.findByUri( characteristicUris ) ) );

        Collection<Characteristic> cs = new HashSet<Characteristic>();
        for ( SearchResult crs : inSystem ) {
            cs.add( ( Characteristic ) crs.getResultObject() );
        }

        // / Owners of the characteristics is what we want.
        Map<Characteristic, Object> parentMap = characteristicService.getParents( cs );
        inSystem = filterCharacteristicOwnersByClass( classes, parentMap );

        if ( parentMap.size() > 0 ) {
            log.info( "Found " + parentMap.size() + "  owners for characteristics:" );
            for ( Object obj : parentMap.values() ) {
                if ( obj instanceof Auditable )
                    log.info( "==== Owner Id: " + ( ( Auditable ) obj ).getId() + " Owner Class: " + obj.getClass() );
                else
                    log.info( "==== Owner : " + obj.toString() + " Owner Class: " + obj.getClass() );
            }
        }
        if ( watch.getTime() > 1000 )
            log.info( "Found " + inSystem.size() + " matches in our system in " + watch.getTime() + "ms" );

        watch.stop();
        return inSystem;
    }

    /**
     * A Compass search on array designs.
     * <p>
     * The compass search is backed by a database search so the returned collection is filtered based on access
     * permissions to the objects in the collection.
     * 
     * @param query
     * @return {@link Collection}
     */
    private Collection<SearchResult> compassArrayDesignSearch( SearchSettings settings ) {
        return compassSearch( arrayBean, settings );
    }

    /**
     * @param query
     * @return
     */
    private Collection<SearchResult> compassBibliographicReferenceSearch( SearchSettings settings ) {
        return compassSearch( bibliographicReferenceBean, settings );
    }

    /**
     * A compass backed search that finds biosequences that match the search string. Searches the gene and probe indexes
     * for matches then converts those results to biosequences
     * 
     * @param searchString
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *        for the genes are added to the final results.
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> compassBioSequenceSearch( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults ) {

        Collection<SearchResult> results = compassSearch( biosequenceBean, settings );

        Collection<SearchResult> geneResults = null;
        if ( previousGeneSearchResults == null ) {
            geneResults = compassGeneSearch( settings );
        } else {
            geneResults = previousGeneSearchResults;
        }

        Map<Gene, SearchResult> genes = new HashMap<Gene, SearchResult>();
        for ( SearchResult sr : geneResults ) {
            genes.put( ( Gene ) sr.getResultObject(), sr );
        }

        Map<Gene, Collection<BioSequence>> seqsFromDb = bioSequenceService.findByGenes( genes.keySet() );
        for ( Gene gene : seqsFromDb.keySet() ) {
            List<BioSequence> bs = new ArrayList<BioSequence>( seqsFromDb.get( gene ) );
            bioSequenceService.thaw( bs );
            results.addAll( dbHitsToSearchResult( bs, genes.get( gene ) ) );
        }

        return results;
    }

    /**
     * @param settings
     * @return
     */
    private Collection<SearchResult> compassCompositeSequenceSearch( final SearchSettings settings ) {
        return compassSearch( probeBean, settings );
    }

    /**
     * A compass search on expressionExperiments.
     * 
     * @param query
     * @return {@link Collection}
     */
    private Collection<SearchResult> compassExpressionSearch( SearchSettings settings ) {
        return compassSearch( eeBean, settings );
    }

    /**
     * @param query
     * @return
     */
    private Collection<SearchResult> compassGeneSearch( final SearchSettings settings ) {
        return compassSearch( geneBean, settings );
    }

    /**
     * @param bean
     * @param settings
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> compassSearch( Compass bean, final SearchSettings settings ) {
        CompassTemplate template = new CompassTemplate( bean );
        Collection<SearchResult> searchResults = ( Collection<SearchResult> ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( settings, session );
                    }
                } );
        return searchResults;
    }

    /**
     * Search for composite sequences associated with genes.
     * 
     * @param settings
     * @param geneSearchResults Optional. If non-null, the results here will be used instead of conducting a brand new
     *        search for genes.
     * @param arrayDesign
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> compositeSequenceByGeneSearch( SearchSettings settings,
            Collection<SearchResult> geneSearchResults ) {

        // Note that the gene results are NOT returned.
        final Collection<SearchResult> geneResults;
        Set<SearchResult> allResults = new HashSet<SearchResult>();
        if ( geneSearchResults == null ) {
            geneResults = geneSearch( settings );
        } else {
            geneResults = geneSearchResults;
        }

        // if there have been any genes returned, find the compositeSequences
        // associated with the genes
        if ( geneResults != null && geneResults.size() > 0 ) {
            ArrayDesign arrayDesign = settings.getArrayDesign();
            for ( SearchResult sr : geneResults ) {
                if ( arrayDesign == null ) {
                    Collection<CompositeSequence> geneCs = geneService.getCompositeSequencesById( sr.getId() );
                    allResults.addAll( dbHitsToSearchResult( geneCs ) );
                } else {
                    Collection<CompositeSequence> geneCs = geneService.getCompositeSequences( ( Gene ) sr
                            .getResultObject(), arrayDesign );
                    allResults.addAll( dbHitsToSearchResult( geneCs ) );
                }

            }
        }
        return allResults;
    }

    /**
     * Search by name of the composite sequence as well as gene.
     * 
     * @param searchString
     * @param arrayDesign to restrict to
     * @param geneSearchResults Can be null, otherwise used to avoid a second search.
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> compositeSequenceSearch( SearchSettings settings,
            Collection<SearchResult> geneSearchResults ) {

        StopWatch watch = startTiming();

        Collection<SearchResult> allResults = new HashSet<SearchResult>();
        allResults.addAll( compassCompositeSequenceSearch( settings ) );
        allResults.addAll( databaseCompositeSequenceSearch( settings ) );
        allResults.addAll( compositeSequenceByGeneSearch( settings, geneSearchResults ) );

        /*
         * This last step is needed because the compassSearch for compositeSequences returns bioSequences too.
         */
        Collection<SearchResult> finalResults = new HashSet<SearchResult>();
        for ( SearchResult sr : allResults ) {
            if ( CompositeSequence.class.isAssignableFrom( sr.getResultClass() ) ) {
                finalResults.add( sr );
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Composite sequence search for '" + settings + "' took " + watch.getTime() + " ms, "
                    + finalResults.size() + " results." );
        return finalResults;
    }

    /**
     * Searches the DB for array designs which have composite sequences whose names match the given search string.
     * Because of the underlying database search, this is acl aware. That is, returned array designs are filtered based
     * on access control list (ACL) permissions.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> databaseArrayDesignSearch( SearchSettings settings ) {

        StopWatch watch = startTiming();

        Collection<ArrayDesign> adSet = new HashSet<ArrayDesign>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( settings.getQuery() );
        for ( CompositeSequence sequence : matchedCs ) {
            adSet.add( sequence.getArrayDesign() );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Array Design Compositesequence DB search for " + settings + " took " + watch.getTime() + " ms"
                    + " found " + adSet.size() + " Ads" );

        return dbHitsToSearchResult( adSet );

    }

    /**
     * A database serach for biosequences. Biosequence names are already indexed by compass...
     * 
     * @param searchString
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> databaseBioSequenceSearch( SearchSettings settings ) {
        StopWatch watch = startTiming();

        String searchString = settings.getQuery();

        // replace * with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );

        Collection<BioSequence> bs = bioSequenceService.findByName( inexactString );
        bioSequenceService.thaw( bs );
        Collection<SearchResult> bioSequenceList = new HashSet<SearchResult>( dbHitsToSearchResult( bs ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "BioSequence DB search for " + searchString + " took " + watch.getTime() + " ms and found"
                    + bioSequenceList.size() + " BioSequences" );

        return bioSequenceList;
    }

    /**
     * @param clazz Class of objects to restrict the search to (typically ExpressionExperiment.class, for example).
     * @param settings
     * @return Collection of search results for the objects owning the found characteristics, where the owner is of
     *         class clazz
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> databaseCharacteristicSearchForOwners( Collection<Class> classes,
            SearchSettings settings ) {
        Collection<Characteristic> characteristicValueMatches = characteristicService.findByValue( settings.getQuery()
                + "%" );
        Collection<Characteristic> characteristicURIMatches = characteristicService.findByUri( settings.getQuery() );

        Map parentMap = characteristicService.getParents( characteristicValueMatches );
        parentMap.putAll( characteristicService.getParents( characteristicURIMatches ) );

        return filterCharacteristicOwnersByClass( classes, parentMap );
    }

    /**
     * Search the DB for genes that are matched
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> databaseCompositeSequenceSearch( final SearchSettings settings ) {

        StopWatch watch = startTiming();

        Set<Gene> geneSet = new HashSet<Gene>();

        String searchString = settings.getQuery();
        ArrayDesign ad = settings.getArrayDesign();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = new HashSet<CompositeSequence>();
        if ( ad != null ) {
            CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
            matchedCs.add( cs );
        } else {
            matchedCs = compositeSequenceService.findByName( searchString );
        }

        // search by associated genes.
        for ( CompositeSequence sequence : matchedCs ) {
            geneSet.addAll( compositeSequenceService.getGenes( sequence ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Gene composite sequence DB search " + searchString + " took " + watch.getTime() + " ms, "
                    + geneSet.size() + " items." );

        return dbHitsToSearchResult( new ArrayList<Gene>( geneSet ) );
    }

    /**
     * Does search on exact string by: id, name and short name.
     * <p>
     * The compass search is backed by a database search so the returned collection is filtered based on access
     * permissions to the objects in the collection.
     * 
     * @param query
     * @return {@link Collection}
     */
    private Collection<SearchResult> databaseExpressionExperimentSearch( final SearchSettings settings ) {

        StopWatch watch = startTiming();

        Collection<ExpressionExperiment> results = new HashSet<ExpressionExperiment>();
        ExpressionExperiment ee = expressionExperimentService.findByName( settings.getQuery() );
        if ( ee != null ) {
            results.add( ee );
        } else {
            ee = expressionExperimentService.findByShortName( settings.getQuery() );
            if ( ee != null ) {
                results.add( ee );
            } else {
                try {
                    // maybe user put in a primary key value.
                    ee = expressionExperimentService.load( new Long( settings.getQuery() ) );
                    if ( ee != null ) results.add( ee );
                } catch ( NumberFormatException e ) {
                    // no-op - it's not an ID.
                }
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "DB Expression Experiment search for " + settings + " took " + watch.getTime() + " ms and found "
                    + results.size() + " EEs" );

        Collection<SearchResult> r = dbHitsToSearchResult( results );
        return r;
    }

    /**
     * Search the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
     * tables
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> databaseGeneSearch( SearchSettings settings ) {

        StopWatch watch = startTiming();
        String searchString = settings.getQuery();
        if ( StringUtils.isBlank( searchString ) ) return new HashSet<SearchResult>();

        // replace * at end with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*$" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );
        // note that at this point, the inexactString might not have a wildcard.
        String exactString = inexactString.replaceAll( "%", "" );

        // if the query is shortish, always do a wild card search. This gives better behavior in 'live
        // search' situations. If we do wildcards on very short queries we get too many results.
        Collection<Gene> geneSet = new HashSet<Gene>();
        if ( searchString.length() > 2 && inexactString.endsWith( "%" ) ) {
            // case 1: user asked for wildcard. We allow this on strings of length 3 or more.
            geneSet.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );
        } else if ( searchString.length() > 3 && searchString.length() < 6 ) {
            // case 2: user did not ask for a wildcard, but we add it anyway, if the string is 4 or 5 characters.
            if ( !inexactString.endsWith( "%" ) ) {
                inexactString = inexactString + "%";
            }
            geneSet.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );
        } else {
            // case 3: string is long enough, and user did not ask for wildcard.
            geneSet.addAll( geneService.findByOfficialSymbol( exactString ) );
        }

        /*
         * TODO The rest we are doing by exact matches only. For aliases it would probably be good to do it like
         * official symbols. For the other searches I think exact matches are the only thing that makes sense.
         */

        geneSet.addAll( geneService.findByAlias( exactString ) );
        geneSet.addAll( geneProductService.getGenesByName( exactString ) );
        geneSet.addAll( geneProductService.getGenesByNcbiId( exactString ) );
        geneSet.addAll( bioSequenceService.getGenesByAccession( exactString ) );
        geneSet.addAll( bioSequenceService.getGenesByName( exactString ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                    + geneSet.size() + " genes" );

        Collection<SearchResult> results = dbHitsToSearchResult( geneSet );
        filterByTaxon( settings, results );
        return results;
    }

    /**
     * Convert hits from database searches into SearchResults.
     * 
     * @param entities
     * @return
     */
    private Collection<SearchResult> dbHitsToSearchResult( Collection<? extends Object> entities ) {
        return this.dbHitsToSearchResult( entities, null );
    }

    /**
     * Convert hits from database searches into SearchResults.
     * 
     * @param entities
     * @param compassHitDerivedFrom SearchResult that these entities were derived from. For example, if you
     *        compass-searched for genes, and then used the genes to get sequences from the database, the gene is
     *        compassHitsDerivedFrom. If null, we treat this as a direct hit.
     * @return
     */
    private List<SearchResult> dbHitsToSearchResult( Collection<? extends Object> entities,
            SearchResult compassHitDerivedFrom ) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        for ( Object e : entities ) {
            if ( compassHitDerivedFrom != null ) {
                SearchResult esr = new SearchResult( e, compassHitDerivedFrom.getScore() * INDIRECT_DB_HIT_PENALTY );
                esr.setHighlightedText( compassHitDerivedFrom.getHighlightedText() );
                results.add( esr );
            } else {

                results.add( new SearchResult( e, 1.0 ) );
            }
        }
        return results;
    }

    /**
     * A general search for expression experiments. This search does both an database search and a compass search.
     * 
     * @param settings
     * @return {@link Collection}
     */
    private Collection<SearchResult> expressionExperimentSearch( final SearchSettings settings ) {
        StopWatch watch = startTiming();
        Collection<SearchResult> results = databaseExpressionExperimentSearch( settings );
        Collection<SearchResult> compassExpressionSearchResults = compassExpressionSearch( settings );
        results.addAll( compassExpressionSearchResults );
        results.addAll( characteristicExpressionExperimentSearch( settings ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Expression Experiment search for '" + settings + "' took " + watch.getTime() + " ms, "
                    + results.size() + " hits." );

        return results;
    }

    /**
     * This can only be used on SearchResults where the result object has a "getTaxon" method.
     * 
     * @param settings
     * @param geneSet
     */
    private void filterByTaxon( SearchSettings settings, Collection<SearchResult> results ) {
        if ( settings.getTaxon() != null ) {
            Collection<SearchResult> toRemove = new HashSet<SearchResult>();
            Taxon t = settings.getTaxon();
            for ( SearchResult sr : results ) {

                Object o = sr.getResultObject();
                try {
                    Method m = o.getClass().getMethod( "getTaxon", new Class[] {} );
                    Taxon currentTaxon = ( Taxon ) m.invoke( o, new Object[] {} );
                    if ( !currentTaxon.equals( t ) ) {
                        toRemove.add( sr );
                    }
                } catch ( SecurityException e ) {
                    throw new RuntimeException( e );
                } catch ( NoSuchMethodException e ) {
                    throw new RuntimeException( e );
                } catch ( IllegalArgumentException e ) {
                    throw new RuntimeException( e );
                } catch ( IllegalAccessException e ) {
                    throw new RuntimeException( e );
                } catch ( InvocationTargetException e ) {
                    throw new RuntimeException( e );
                }
            }
            results.removeAll( toRemove );
        }
    }

    /**
     * @param clazz
     * @param parentMap
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> filterCharacteristicOwnersByClass( Collection<Class> classes, Map parentMap ) {
        Collection<SearchResult> results = new HashSet<SearchResult>();
        for ( Object c : parentMap.keySet() ) {
            Object o = parentMap.get( c );
            for ( Class clazz : classes ) {
                if ( clazz.isAssignableFrom( o.getClass() ) ) {
                    results.add( new SearchResult( o, 1.0 ) );
                }
            }
        }
        return results;
    }

    /**
     * Combines compass style search, the db style search, and the compositeSequence search and returns 1 combined list
     * with no duplicates.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    private Collection<SearchResult> geneSearch( final SearchSettings settings ) {

        StopWatch watch = startTiming();

        String searchString = settings.getQuery();

        Collection<SearchResult> geneDbList = databaseGeneSearch( settings );
        Collection<SearchResult> geneCompassList = compassGeneSearch( settings );

        Collection<SearchResult> geneCsList = databaseCompositeSequenceSearch( settings );

        Set<SearchResult> combinedGeneList = new HashSet<SearchResult>();
        combinedGeneList.addAll( geneDbList );
        combinedGeneList.addAll( geneCompassList );
        combinedGeneList.addAll( geneCsList );

        filterByTaxon( settings, combinedGeneList );

        if ( watch.getTime() > 1000 )
            log.info( "Gene search for " + searchString + " took " + watch.getTime() + " ms; "
                    + combinedGeneList.size() + " results." );
        return combinedGeneList;
    }

    /**
     * @param searchResults
     * @return List of ids for the entities held by the search results.
     */
    private List<Long> getIds( List<SearchResult> searchResults ) {
        List<Long> list = new ArrayList<Long>();
        for ( SearchResult ee : searchResults ) {
            list.add( ee.getId() );
        }
        return list;
    }

    /**
     * @param hits
     * @return
     */
    private Collection<SearchResult> getSearchResults( CompassHit[] hits ) {
        Collection<SearchResult> results = new HashSet<SearchResult>();
        for ( CompassHit compassHit : hits ) {
            SearchResult r = new SearchResult( compassHit.getData() );
            /*
             * Always give compass hits a lower score so they can be differentiated from exact database hits.
             */
            r.setScore( new Double( compassHit.getScore() * COMPASS_HIT_SCORE_PENALTY_FACTOR ) );
            CompassHighlightedText highlightedText = compassHit.getHighlightedText();
            if ( highlightedText != null ) {
                r.setHighlightedText( highlightedText.getHighlightedText() );
            }
            results.add( r );
        }
        return results;
    }

    /**
     * @param settings
     * @param results
     * @param rawResults
     * @param fillObjects
     */
    private Map<Class, List<SearchResult>> getSortedLimitedResults( SearchSettings settings,
            List<SearchResult> rawResults, boolean fillObjects ) {

        Map<Class, List<SearchResult>> results = new HashMap<Class, List<SearchResult>>();
        Collections.sort( rawResults );

        results.put( ArrayDesign.class, new ArrayList<SearchResult>() );
        results.put( BioSequence.class, new ArrayList<SearchResult>() );
        results.put( BibliographicReference.class, new ArrayList<SearchResult>() );
        results.put( CompositeSequence.class, new ArrayList<SearchResult>() );
        results.put( ExpressionExperiment.class, new ArrayList<SearchResult>() );
        results.put( Gene.class, new ArrayList<SearchResult>() );
        results.put( PredictedGene.class, new ArrayList<SearchResult>() );
        results.put( ProbeAlignedRegion.class, new ArrayList<SearchResult>() );

        /*
         * Get the top N results, overall (NOT within each class - experimental.)
         */
        for ( int i = 0, limit = Math.min( rawResults.size(), settings.getMaxResults() ); i < limit; i++ ) {
            SearchResult sr = rawResults.get( i );

            /*
             * FIXME This is unpleasant and should be removed when BioSequences are correctly detached.
             */
            Class resultClass = EntityUtils.getImplementationForProxy( sr.getResultObject() ).getClass();

            resultClass = ReflectionUtil.getBaseForImpl( resultClass );

            assert results.containsKey( resultClass ) : "Unknown class " + resultClass;
            results.get( resultClass ).add( sr );
        }

        if ( fillObjects ) {
            /**
             * Now retrieve the entities and put them in the SearchResult. Entities that are filtered out by the
             * SecurityInterceptor will be removed at this stage.
             */
            for ( Class clazz : results.keySet() ) {
                List<SearchResult> r = results.get( clazz );
                if ( r.size() == 0 ) continue;
                Map<Long, SearchResult> rMap = new HashMap<Long, SearchResult>();
                for ( SearchResult searchResult : r ) {
                    rMap.put( searchResult.getId(), searchResult );
                }

                Collection entities = retrieveResultEntities( clazz, r );
                List<SearchResult> filteredResults = new ArrayList<SearchResult>();
                for ( Object entity : entities ) {
                    Long id = EntityUtils.getId( entity );
                    SearchResult keeper = rMap.get( id );
                    keeper.setResultObject( entity );
                    filteredResults.add( keeper );
                }
                results.put( clazz, filteredResults );

            }
        } else {
            for ( SearchResult sr : rawResults ) {
                sr.setResultObject( null );
            }
        }

        return results;
    }

    /**
     * Attempts to find an exact match for the search term in the characteristic table (by value and value URI). If the
     * search term is found then uses that URI to find the parents and returns them as SearchResults.
     * 
     * @param classes
     * @param searchString
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> ontologySearchAnnotatedObject( Collection<Class> classes, SearchSettings settings ) {

        /*
         * Direct search.
         */
        Collection<SearchResult> results = databaseCharacteristicSearchForOwners( classes, settings );

        /*
         * Include children in ontologies, if any. This can be very slow.
         */
        Collection<SearchResult> childResults = characteristicSearchWithChildren( classes, settings );

        results.addAll( childResults );

        return results;

    }

    /**
     * @param query
     * @param session
     * @return
     */
    private Collection<SearchResult> performSearch( SearchSettings settings, CompassSession session ) {
        StopWatch watch = startTiming();

        String query = settings.getQuery();
        if ( StringUtils.isBlank( query ) || query.equals( "*" ) ) return new ArrayList<SearchResult>();

        CompassQuery compassQuery = session.queryBuilder().queryString( query.trim() ).toQuery();
        CompassHits hits = compassQuery.hits();

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Getting " + hits.getLength() + " hits for " + query + " took " + watch.getTime() + " ms" );
        watch.reset();
        watch.start();

        cacheHighlightedText( hits );

        // Put a limit on the number of hits to detach.
        // Detaching hits can be time consuming (somewhat like thawing).

        CompassDetachedHits detachedHits = hits.detach( 0, Math.min( hits.getLength(), MAX_COMPASS_HITS_TO_DETACH ) );

        watch.stop();

        if ( watch.getTime() > 1000 )
            log.info( "Detaching " + detachedHits.getLength() + " hits for " + query + " took " + watch.getTime()
                    + " ms" );

        Collection<SearchResult> searchResults = getSearchResults( detachedHits.getHits() );

        return searchResults;
    }

    /**
     * @param hits
     */
    private void cacheHighlightedText( CompassHits hits ) {
        for ( int i = 0; i < hits.getLength(); i++ ) {
            CompassHighlighter highlighter = hits.highlighter( i );
            try {
                highlighter.fragment( "description" );
            } catch ( SearchEngineException e ) {
                // no big deal - we asked for a property it doesn't have. Must be a
                // better way...
            }

            try {
                highlighter.fragment( "name" );
            } catch ( SearchEngineException e ) {
                // no big deal - we asked for a property it doesn't have. Must be a
                // better way...
            }

            try {
                highlighter.fragment( "shortName" );
            } catch ( SearchEngineException e ) {
                // no big deal - we asked for a property it doesn't have. Must be a better way...
            }
            try {
                highlighter.fragment( "title" );
            } catch ( SearchEngineException e ) {
                // no big deal - we asked for a property it doesn't have. Must be a better way...
            }
            try {
                highlighter.fragment( "abstract" );
            } catch ( SearchEngineException e ) {
                // no big deal - we asked for a property it doesn't have. Must be a better way...
            }
        }
    }

    /**
     * Retrieve entities from the persistent store.
     * 
     * @param entityClass
     * @param results
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection retrieveResultEntities( Class entityClass, List<SearchResult> results ) {
        List<Long> ids = getIds( results );
        if ( ExpressionExperiment.class.isAssignableFrom( entityClass ) ) {
            return expressionExperimentService.loadMultiple( ids );
        } else if ( ArrayDesign.class.isAssignableFrom( entityClass ) ) {
            return arrayDesignService.loadMultiple( ids );
        } else if ( CompositeSequence.class.isAssignableFrom( entityClass ) ) {
            return compositeSequenceService.loadMultiple( ids );
        } else if ( BibliographicReference.class.isAssignableFrom( entityClass ) ) {
            return bibliographicReferenceService.loadMultiple( ids );
        } else if ( Gene.class.isAssignableFrom( entityClass ) ) {
            return geneService.loadMultiple( ids );
        } else if ( BioSequence.class.isAssignableFrom( entityClass ) ) {
            return bioSequenceService.loadMultiple( ids );
        } else {
            throw new UnsupportedOperationException( "Don't know how to retrieve objects for class=" + entityClass );
        }
    }

    private StopWatch startTiming() {
        StopWatch watch = new StopWatch();
        watch.start();
        return watch;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

}
