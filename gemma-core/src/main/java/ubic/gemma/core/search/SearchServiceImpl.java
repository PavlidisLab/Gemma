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

package ubic.gemma.core.search;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.*;
import org.compass.core.mapping.CompassMapping;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.mapping.osem.ClassMapping;
import org.compass.core.mapping.osem.ComponentMapping;
import org.compass.core.spi.InternalCompassSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneSearchService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.UserQuery;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;
import ubic.gemma.persistence.util.CacheUtils;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service is used for performing searches using free text or exact matches to items in the database.
 * <h2>Implementation notes</h2>
 * Internally, there are generally two kinds of searches performed, precise database searches looking for exact matches
 * in the database and compass/lucene searches which look for matches in the stored index.
 * To add more dependencies to this Service edit the applicationContext-search.xml
 *
 * @author klc
 * @author paul
 * @author keshav
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Log log = LogFactory.getLog( SearchServiceImpl.class.getName() );

    private static final String ONTOLOGY_CHILDREN_CACHE_NAME = "OntologyChildrenCache";
    private static final String NCBI_GENE = "ncbi_gene";
    private static final int MINIMUM_EE_QUERY_LENGTH = 3;
    private static final int MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH = 2;
    private static final int MAX_LUCENE_HITS = 750;

    /**
     * Penalty applied to all 'index' hits
     */
    private static final double COMPASS_HIT_SCORE_PENALTY_FACTOR = 0.9;

    /**
     * Penalty applied to scores on hits for entities that derive from an association. For example, if a hit to an EE
     * came from text associated with one of its biomaterials, the score is penalized by this amount.
     */
    private static final double INDIRECT_DB_HIT_PENALTY = 0.8;

    /**
     * How long after creation before an object is evicted, no matter what.
     */
    private static final int ONTOLOGY_CACHE_TIME_TO_DIE = 10000;

    /**
     * How long an item in the cache lasts when it is not accessed.
     */
    private static final int ONTOLOGY_CACHE_TIME_TO_IDLE = 3600;

    /**
     * How many term children can stay in memory
     */
    private static final int ONTOLOGY_INFO_CACHE_SIZE = 30000;

    /**
     * If fewer than this number of experiments are returned from the a search of experiment characteristics, then
     * search for experiments indirectly as well (ex: by finding bioMaterials tagged with the characteristics and
     * getting the experiments associated with them ). See also MAX_CHARACTERISTIC_SEARCH_RESULTS.
     */
    private static final int SUFFICIENT_EXPERIMENT_RESULTS_FROM_CHARACTERISTICS = 100;

    /**
     * The maximum number of characteristics to search while walking down a ontology graph.
     */
    private static final int MAX_CHARACTERISTIC_SEARCH_RESULTS = 500;

    private final HashMap<String, Taxon> nameToTaxonMap = new LinkedHashMap<>();

    private Cache childTermCache;

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    @Autowired
    private BioSequenceService bioSequenceService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private ExpressionExperimentSetService experimentSetService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private GeneSearchService geneSearchService;
    @Autowired
    private GeneProductService geneProductService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    @Autowired
    private TaxonDao taxonDao;
    @Autowired
    private AuditTrailService auditTrailService;

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

    @Override
    public Map<Class<?>, List<SearchResult>> ajaxSearch( SearchSettingsValueObject settingsValueObject ) {
        SearchSettings settings = SearchSettingsValueObject.toEntity( settingsValueObject );
        return this.search( settings );
    }

    @Override
    public Map<Class<?>, List<SearchResult>> search( SearchSettings settings ) {
        Map<Class<?>, List<SearchResult>> searchResults = new HashMap<>();
        try {
            searchResults = this.search( settings, true, false );

        } catch ( org.compass.core.engine.SearchEngineQueryParseException qpe ) {
            SearchServiceImpl.log.error( "Query parse Error: " + settings + "; message=" + qpe.getMessage(), qpe );

        } catch ( Exception e ) {
            SearchServiceImpl.log.error( "Search error on settings: " + settings + "; message=" + e.getMessage(), e );
        }

        return searchResults;
    }

    @Override
    public Map<Class<?>, List<SearchResult>> speedSearch( SearchSettings settings ) {
        Map<Class<?>, List<SearchResult>> searchResults = new HashMap<>();
        try {
            searchResults = this.search( settings, true, true );

        } catch ( org.compass.core.engine.SearchEngineQueryParseException qpe ) {
            SearchServiceImpl.log.error( "Query parse Error: " + settings + "; message=" + qpe.getMessage(), qpe );

        } catch ( Exception e ) {
            SearchServiceImpl.log.error( "Search error on settings: " + settings + "; message=" + e.getMessage(), e );
        }

        return searchResults;
    }

    @Override
    public Map<Class<?>, List<SearchResult>> search( SearchSettings settings, boolean fillObjects,
            boolean webSpeedSearch ) {

        if ( StringUtils.isBlank( settings.getTermUri() ) && !settings.getQuery().startsWith( "http://" ) ) {
            return this.generalSearch( settings, fillObjects, webSpeedSearch );
        }

        // we only attempt an ontology search if the uri looks remotely like a url.
        return this.ontologyUriSearch( settings );

    }

    @Override
    public Collection<Long> searchExpressionExperiments( String query, Long taxonId ) {
        Taxon taxon = taxonDao.load( taxonId );
        Collection<Long> eeIds = new HashSet<>();
        if ( StringUtils.isNotBlank( query ) ) {

            if ( query.length() < SearchServiceImpl.MINIMUM_EE_QUERY_LENGTH )
                return eeIds;

            // Initial list
            List<SearchResult> results = this
                    .search( SearchSettingsImpl.expressionExperimentSearch( query ), false, false )
                    .get( ExpressionExperiment.class );
            for ( SearchResult result : results ) {
                eeIds.add( result.getId() );
            }

            // Filter by taxon
            if ( taxon != null ) {
                eeIds.retainAll( EntityUtils.getIds( expressionExperimentService.findByTaxon( taxon ) ) );
            }
        } else {
            Collection<ExpressionExperiment> ees = ( taxon != null ) ?
                    expressionExperimentService.findByTaxon( taxon ) :
                    expressionExperimentService.loadAll();
            for ( ExpressionExperiment ee : ees ) {
                eeIds.add( ee.getId() );
            }
        }
        return eeIds;
    }

    @Override
    public List<?> search( SearchSettings settings, Class<?> resultClass ) {
        Map<Class<?>, List<SearchResult>> searchResults = this.search( settings );
        List<Object> resultObjects = new ArrayList<>();

        List<SearchResult> searchResultObjects = searchResults.get( resultClass );
        if ( searchResultObjects == null )
            return resultObjects;

        for ( SearchResult sr : searchResultObjects ) {
            resultObjects.add( sr.getResultObject() );
        }

        return resultObjects;
    }

    @Override
    public Map<Class<?>, List<SearchResult>> searchForNewlyCreatedUserQueryResults( UserQuery query ) {

        Map<Class<?>, List<SearchResult>> searchResults;
        Map<Class<?>, List<SearchResult>> finalResults = new HashMap<>();

        SearchSettings settings = query.getSearchSettings();

        if ( StringUtils.isBlank( settings.getTermUri() ) && !settings.getQuery().startsWith( "http://" ) ) {
            // fill objects=true, speedySearch=false
            searchResults = this.generalSearch( settings, true, false );
        } else {
            // we only attempt an ontology search if the uri looks remotely like a url.
            searchResults = this.ontologyUriSearch( settings );
        }

        if ( searchResults == null ) {
            return finalResults;
        }

        for ( Class<?> clazz : searchResults.keySet() ) {

            List<SearchResult> results = searchResults.get( clazz );

            List<SearchResult> updatedResults = new ArrayList<>();

            if ( results.size() == 0 )
                continue;

            SearchServiceImpl.log
                    .info( "Search for newly createdQuery with settings: " + settings + "; result: " + results.size()
                            + " " + clazz.getSimpleName() + "s" );

            for ( SearchResult sr : results ) {

                // Are SearchResults always auditable? maybe put in some error handling in case they are not or
                // enforce searchSettings object to be of a certain form
                Auditable auditableResult = ( Auditable ) sr.getResultObject();

                // this list is ordered by date (not descending)
                List<AuditEvent> eventList = auditTrailService.getEvents( auditableResult );

                if ( eventList == null || eventList.isEmpty() )
                    continue;

                for ( AuditEvent ae : eventList ) {

                    // assuming there is only one create event
                    if ( ae.getAction() == AuditAction.CREATE && ae.getDate().after( query.getLastUsed() ) ) {
                        updatedResults.add( sr );
                        break;
                    }

                }

            }

            if ( !updatedResults.isEmpty() ) {
                finalResults.put( clazz, updatedResults );
            }

        }

        return finalResults;

    }

    @PostConstruct
    void initializeSearchService() {
        try {
            boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", false );
            this.childTermCache = CacheUtils
                    .createOrLoadCache( cacheManager, SearchServiceImpl.ONTOLOGY_CHILDREN_CACHE_NAME, terracottaEnabled,
                            SearchServiceImpl.ONTOLOGY_INFO_CACHE_SIZE, false, false,
                            SearchServiceImpl.ONTOLOGY_CACHE_TIME_TO_IDLE, SearchServiceImpl.ONTOLOGY_CACHE_TIME_TO_DIE,
                            false );
        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }
        this.initializeNameToTaxonMap();
    }

    private void initializeNameToTaxonMap() {

        Collection<? extends Taxon> taxonCollection = taxonDao.loadAll();

        for ( Taxon taxon : taxonCollection ) {
            if ( taxon.getScientificName() != null )
                nameToTaxonMap.put( taxon.getScientificName().trim().toLowerCase(), taxon );
            if ( taxon.getCommonName() != null )
                nameToTaxonMap.put( taxon.getCommonName().trim().toLowerCase(), taxon );
            if ( taxon.getAbbreviation() != null )
                nameToTaxonMap.put( taxon.getAbbreviation().trim().toLowerCase(), taxon );
        }

        // Loop through again breaking up multi-word taxon database names.
        // Doing this is a separate loop so that these names take lower precedence when matching than the full terms in
        // the generated keySet.
        for ( Taxon taxon : taxonCollection ) {
            this.addTerms( taxon, taxon.getCommonName() );
            this.addTerms( taxon, taxon.getScientificName() );
        }

    }

    private void addTerms( Taxon taxon, String taxonName ) {
        String[] terms;
        if ( StringUtils.isNotBlank( taxonName ) ) {
            terms = taxonName.split( "\\s+" );
            // Only continue for multi-word
            if ( terms.length > 1 ) {
                for ( String s : terms ) {
                    if ( !nameToTaxonMap.containsKey( s.trim().toLowerCase() ) ) {
                        nameToTaxonMap.put( s.trim().toLowerCase(), taxon );
                    }
                }
            }
        }
    }

    /**
     * @return results, if the settings.termUri is populated. This includes gene uris.
     */
    private Map<Class<?>, List<SearchResult>> ontologyUriSearch( SearchSettings settings ) {
        Map<Class<?>, List<SearchResult>> results = new HashMap<>();

        // 1st check to see if the query is a URI (from an ontology).
        // Do this by seeing if we can find it in the loaded ontologies.
        // Escape with general utilities because might not be doing a lucene backed search. (just a hibernate one).
        String termUri = settings.getTermUri();

        if ( StringUtils.isBlank( termUri ) ) {
            termUri = settings.getQuery();
        }

        if ( !termUri.startsWith( "http://" ) ) {
            return results;
        }

        OntologyTerm matchingTerm;
        String uriString;

        uriString = StringEscapeUtils.escapeJava( StringUtils.strip( termUri ) );

        if ( StringUtils.containsIgnoreCase( uriString, SearchServiceImpl.NCBI_GENE ) ) {
            // Perhaps is a valid gene URL. Want to search for the gene in gemma.
            // 1st get objects tagged with the given gene identifier
            Collection<Class<?>> classesToFilterOn = new HashSet<>();
            classesToFilterOn.add( ExpressionExperiment.class );

            Collection<Characteristic> foundCharacteristics = characteristicService
                    .findByUri( classesToFilterOn, uriString );
            Map<Characteristic, Object> parentMap = characteristicService
                    .getParents( classesToFilterOn, foundCharacteristics );

            Collection<SearchResult> characteristicOwnerResults = this
                    .filterCharacteristicOwnersByClass( classesToFilterOn, parentMap );

            if ( !characteristicOwnerResults.isEmpty() ) {
                results.put( ExpressionExperiment.class, new ArrayList<SearchResult>() );
                results.get( ExpressionExperiment.class ).addAll( characteristicOwnerResults );
            }

            if ( settings.getSearchGenes() ) {
                // Get the gene
                String ncbiAccessionFromUri = StringUtils.substringAfterLast( uriString, "/" );
                Gene g = null;

                try {
                    g = geneService.findByNCBIId( Integer.parseInt( ncbiAccessionFromUri ) );
                } catch ( NumberFormatException e ) {
                    // ok
                }

                if ( g != null ) {
                    results.put( Gene.class, new ArrayList<SearchResult>() );
                    results.get( Gene.class ).add( new SearchResult( g ) );
                }
            }
            return results;
        }

        /*
         * Not searching for a gene.
         */
        Collection<SearchResult> matchingResults;
        Collection<Class<?>> classesToSearch = new HashSet<>();
        if ( settings.getSearchExperiments() ) {
            classesToSearch.add( ExpressionExperiment.class ); // not sure ...
            classesToSearch.add( BioMaterial.class );
            classesToSearch.add( FactorValue.class );
        }

        // this doesn't seem to be implemented yet, LiteratureEvidence and GenericEvidence aren't handled in the
        // fillValueObjects method downstream
        /*
         * if ( settings.getSearchPhenotypes() ) { classesToSearch.add( PhenotypeAssociation.class ); }
         */
        matchingTerm = this.ontologyService.getTerm( uriString );
        if ( matchingTerm == null || matchingTerm.getUri() == null ) {
            /*
             * Maybe the ontology isn't loaded. Look anyway.
             */
            Map<Characteristic, Object> parentMap = characteristicService
                    .getParents( classesToSearch, characteristicService.findByUri( classesToSearch, uriString ) );
            matchingResults = this.filterCharacteristicOwnersByClass( classesToSearch, parentMap );

        } else {

            SearchServiceImpl.log.info( "Found ontology term: " + matchingTerm );

            // Was a URI from a loaded ontology soo get the children.
            Collection<OntologyTerm> terms2Search4 = matchingTerm.getChildren( true );
            terms2Search4.add( matchingTerm );

            matchingResults = this.databaseCharacteristicExactUriSearchForOwners( classesToSearch, terms2Search4 );
        }

        for ( SearchResult searchR : matchingResults ) {
            if ( results.containsKey( searchR.getResultClass() ) ) {
                results.get( searchR.getResultClass() ).add( searchR );
            } else {
                List<SearchResult> rs = new ArrayList<>();
                rs.add( searchR );
                results.put( searchR.getResultClass(), rs );
            }
        }

        return results;
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
                 * different searches.
                 */
                rawResults.add( sr );
            }
        }
    }

    /**
     * Returns children one step down.
     *
     * @param term starting point
     */
    @SuppressWarnings("unchecked")
    private Collection<OntologyTerm> getDirectChildTerms( OntologyTerm term ) {
        String uri = term.getUri();
        /*
         * getChildren can be very slow for 'high-level' classes like "neoplasm", so we use a cache.
         */
        Collection<OntologyTerm> children = null;
        if ( StringUtils.isBlank( uri ) ) {
            // shouldn't happen, but just in case
            if ( SearchServiceImpl.log.isDebugEnabled() )
                SearchServiceImpl.log.debug( "Blank uri for " + term );
        }

        Element cachedChildren = this.childTermCache.get( uri );
        if ( cachedChildren == null ) {
            try {
                children = term.getChildren( true );
                childTermCache.put( new Element( uri, children ) );
            } catch ( com.hp.hpl.jena.ontology.ConversionException ce ) {
                SearchServiceImpl.log.warn( "getting children for term: " + term
                        + " caused com.hp.hpl.jena.ontology.ConversionException. " + ce.getMessage() );
            }
        } else {
            children = ( Collection<OntologyTerm> ) cachedChildren.getObjectValue();
        }

        return children;
    }

    /**
     * A general search for array designs.
     * This search does both an database search and a compass search. This is also contains an underlying
     * {@link CompositeSequence} search, returning the {@link ArrayDesign} collection for the given composite sequence
     * search string (the returned collection of array designs does not contain duplicates).
     *
     * @param probeResults Collection of results from a previous CompositeSequence search. Can be null; otherwise used
     *                     to avoid a second search for probes. The array designs for the probes are added to the final results.
     */
    private Collection<SearchResult> arrayDesignSearch( SearchSettings settings,
            Collection<SearchResult> probeResults ) {

        StopWatch watch = this.startTiming();
        String searchString = settings.getQuery();
        Collection<SearchResult> results = new HashSet<>();

        ArrayDesign shortNameResult = arrayDesignService.findByShortName( searchString );
        if ( shortNameResult != null ) {
            results.add( new SearchResult( shortNameResult, 1.0 ) );
        } else {
            Collection<ArrayDesign> nameResult = arrayDesignService.findByName( searchString );
            if ( nameResult != null )
                for ( ArrayDesign ad : nameResult ) {
                    results.add( new SearchResult( ad, 1.0 ) );
                }
        }

        Collection<ArrayDesign> altNameResults = arrayDesignService.findByAlternateName( searchString );
        for ( ArrayDesign arrayDesign : altNameResults ) {
            results.add( new SearchResult( arrayDesign, 0.9 ) );
        }

        Collection<ArrayDesign> manufacturerResults = arrayDesignService.findByManufacturer( searchString );
        for ( ArrayDesign arrayDesign : manufacturerResults ) {
            results.add( new SearchResult( arrayDesign, 0.9 ) );
        }

        results.addAll( this.compassArrayDesignSearch( settings ) );
        results.addAll( this.databaseArrayDesignSearch( settings ) );

        Collection<SearchResult> probes;
        if ( probeResults == null ) {
            probes = this.compassCompositeSequenceSearch( settings );
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
            SearchServiceImpl.log.info( "Array Design search for '" + settings + "' took " + watch.getTime() + " ms" );

        return results;
    }

    /**
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *                                  for the genes are added to the final results.
     */
    private Collection<SearchResult> bioSequenceSearch( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults ) {
        StopWatch watch = this.startTiming();

        Collection<SearchResult> searchResults = new HashSet<>();
        searchResults.addAll( this.compassBioSequenceSearch( settings, previousGeneSearchResults ) );
        searchResults.addAll( this.databaseBioSequenceSearch( settings ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Biosequence search for '" + settings + "' took " + watch.getTime() + " ms " + searchResults
                            .size() + " results." );

        return searchResults;
    }

    private Collection<SearchResult> characteristicExpressionExperimentSearch( final SearchSettings settings ) {

        Collection<SearchResult> results = new HashSet<>();

        Collection<Class<?>> classToSearch = new ArrayList<>( 1 ); // this is a collection because of the API
        // for characteristicService; could add
        // findByUri(Class<?>...)

        // order matters.
        Queue<Class<?>> orderedClassesToSearch = new LinkedList<>();
        orderedClassesToSearch.add( ExpressionExperiment.class );
        orderedClassesToSearch.add( FactorValue.class );
        orderedClassesToSearch.add( BioMaterial.class );

        Collection<SearchResult> characterSearchResults = new HashSet<>();

        while ( characterSearchResults.size() < SearchServiceImpl.SUFFICIENT_EXPERIMENT_RESULTS_FROM_CHARACTERISTICS
                && !orderedClassesToSearch.isEmpty() ) {
            classToSearch.clear();
            classToSearch.add( orderedClassesToSearch.poll() );
            // We handle the OR clauses here.
            String[] subclauses = settings.getQuery().split( " OR " );
            for ( String subclause : subclauses ) {
                /*
                 * Note that the AND is applied only within one entity type. The fix would be to apply AND at this
                 * level.
                 */
                Collection<SearchResult> classResults = this
                        .characteristicSearchWithChildren( classToSearch, subclause );
                if ( !classResults.isEmpty() ) {
                    String msg = "Found " + classResults.size() + " " + classToSearch.iterator().next().getSimpleName()
                            + " results from characteristic search.";
                    if ( characterSearchResults.size()
                            >= SearchServiceImpl.SUFFICIENT_EXPERIMENT_RESULTS_FROM_CHARACTERISTICS ) {
                        msg += " Total found > " + SearchServiceImpl.SUFFICIENT_EXPERIMENT_RESULTS_FROM_CHARACTERISTICS
                                + ", will not search for more entities.";
                    }
                    SearchServiceImpl.log.info( msg );
                }
                characterSearchResults.addAll( classResults );
            }

        }

        StopWatch watch = new StopWatch();
        watch.start();

        // filter and get parents...
        int numEEs = 0;
        Collection<BioMaterial> biomaterials = new HashSet<>();
        Collection<FactorValue> factorValues = new HashSet<>();
        Collection<Treatment> treatments = new HashSet<>();

        for ( SearchResult sr : characterSearchResults ) {
            Class<?> resultClass = sr.getResultClass();
            // highlightedText.put( sr.getResultObject(), sr.getHighlightedText() );
            if ( ExpressionExperiment.class.isAssignableFrom( resultClass ) ) {
                sr.setHighlightedText( sr.getHighlightedText() + " (characteristic)" );
                results.add( sr );
                numEEs++;
            } else if ( BioMaterial.class.isAssignableFrom( resultClass ) ) {
                biomaterials.add( ( BioMaterial ) sr.getResultObject() );
            } else if ( FactorValue.class.isAssignableFrom( resultClass ) ) {
                factorValues.add( ( FactorValue ) sr.getResultObject() );
            } else if ( Treatment.class.isAssignableFrom( resultClass ) ) {
                treatments.add( ( Treatment ) sr.getResultObject() );
            }
        }

        /*
         * Much faster to batch it...but we loose track of which search result came from which, so we put generic
         * highlighted text.
         */
        if ( biomaterials.size() > 0 ) {
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByBioMaterials( biomaterials );
            for ( ExpressionExperiment ee : ees ) {
                results.add( new SearchResult( ee, SearchServiceImpl.INDIRECT_DB_HIT_PENALTY,
                        "BioMaterial characteristic" ) );
            }
        }

        this.addEEeByFactorvalues( results, factorValues );

        if ( treatments.size() > 0 ) {
            SearchServiceImpl.log.info( "Not processing treatments, but hits were found" );
        }

        if ( SearchServiceImpl.log.isDebugEnabled() ) {
            SearchServiceImpl.log.debug( "ExpressionExperiment search: " + settings + " -> " + results.size()
                    + " characteristic hits" );
        }

        if ( watch.getTime() > 1000 ) {
            SearchServiceImpl.log
                    .info( "Retrieving " + results.size() + " experiments from " + characterSearchResults.size()
                            + " retrieved characteristics took " + watch.getTime() + " ms" );
            SearchServiceImpl.log
                    .info( "Breakdown: " + numEEs + " via direct association with EE; " + biomaterials.size()
                            + " via association with Biomaterial; " + factorValues.size()
                            + " via experimental design" );
        }

        return results;
    }

    private void addEEeByFactorvalues( Collection<SearchResult> results, Collection<FactorValue> factorValues ) {
        if ( factorValues.size() > 0 ) {
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactorValues( factorValues );
            for ( ExpressionExperiment ee : ees ) {
                if ( SearchServiceImpl.log.isDebugEnabled() )
                    SearchServiceImpl.log.debug( ee );
                results.add(
                        new SearchResult( ee, SearchServiceImpl.INDIRECT_DB_HIT_PENALTY, "Factor characteristic" ) );
            }
        }
    }

    /**
     * Search for the query in ontologies, including items that are associated with children of matching query terms.
     * That is, 'brain' should return entities tagged as 'hippocampus'. This method will return results only up to
     * MAX_CHARACTERISTIC_SEARCH_RESULTS. It can handle AND in searches, so Parkinson's AND neuron finds items tagged
     * with both of those terms. The use of OR is handled by the caller.
     *
     * @param classes Classes of characteristic-bound entities. For example, to get matching characteristics of
     *                ExpressionExperiments, pass ExpressionExperiments.class in this collection parameter.
     * @return SearchResults of CharacteristicObjects. Typically to be useful one needs to retrieve the 'parents'
     * (entities which have been 'tagged' with the term) of those Characteristics
     */
    private Collection<SearchResult> characteristicSearchWithChildren( Collection<Class<?>> classes, String query ) {
        StopWatch timer = this.startTiming();

        /*
         * The tricky part here is if the user has entered a boolean query. If they put in Parkinson's disease AND neuron,
         * then we want to eventually return entities that are associated with both. We don't expect to find single
         * characteristics that match both.
         *
         * But if they put in Parkinson's disease we don't want to do two queries.
         */
        String[] subparts = query.split( " AND " );

        // we would have to first deal with the separate queries, and then apply the logic.
        Collection<SearchResult> allResults = new HashSet<>();

        SearchServiceImpl.log
                .info( "Starting characteristic search: " + query + " for type=" + StringUtils.join( classes, "," ) );
        for ( String rawTerm : subparts ) {
            String trimmed = StringUtils.strip( rawTerm );
            if ( StringUtils.isBlank( trimmed ) ) {
                continue;
            }
            Collection<SearchResult> subqueryResults = this.characteristicSearchTerm( classes, trimmed );
            if ( allResults.isEmpty() ) {
                allResults.addAll( subqueryResults );
            } else {
                // this is our Intersection operation.
                allResults.retainAll( subqueryResults );

                // aggregate the highlighted text.
                Map<SearchResult, String> highlights = new HashMap<>();
                for ( SearchResult sqr : subqueryResults ) {
                    highlights.put( sqr, sqr.getHighlightedText() );
                }

                for ( SearchResult ar : allResults ) {
                    String k = highlights.get( ar );
                    if ( StringUtils.isNotBlank( k ) ) {
                        String highlightedText = ar.getHighlightedText();
                        if ( StringUtils.isBlank( highlightedText ) ) {
                            ar.setHighlightedText( k );
                        } else {
                            ar.setHighlightedText( highlightedText + "," + k );
                        }
                    }
                }
            }

            if ( timer.getTime() > 1000 ) {
                SearchServiceImpl.log.info( "Characteristic search for '" + rawTerm + "': " + allResults.size()
                        + " hits retained so far; " + timer.getTime() + "ms" );
                timer.reset();
                timer.start();
            }

        }

        return allResults;

    }

    /**
     * Perform a search on a query - it does not have to be one word, it could be "parkinson's disease"
     */
    private Collection<SearchResult> characteristicSearchTerm( Collection<Class<?>> classes, String query ) {
        if ( SearchServiceImpl.log.isDebugEnabled() )
            SearchServiceImpl.log.debug( "Starting search for " + query );
        StopWatch watch = this.startTiming();

        Collection<Characteristic> cs = new HashSet<>();

        Collection<OntologyIndividual> individuals = ontologyService.findIndividuals( query );

        for ( Collection<OntologyIndividual> individualbatch : BatchIterator.batches( individuals, 10 ) ) {
            Collection<String> uris = new HashSet<>();
            for ( OntologyIndividual individual : individualbatch ) {
                uris.add( individual.getUri() );
            }
            Collection<SearchResult> dbhits = this
                    .dbHitsToSearchResult( characteristicService.findByUri( classes, uris ), null );
            for ( SearchResult crs : dbhits ) {
                cs.add( ( Characteristic ) crs.getResultObject() );
            }
            if ( cs.size() >= SearchServiceImpl.MAX_CHARACTERISTIC_SEARCH_RESULTS ) {
                break;
            }

        }

        if ( individuals.size() > 0 && watch.getTime() > 1000 ) {
            SearchServiceImpl.log
                    .info( "Found " + individuals.size() + " individuals matching '" + query + "' in " + watch.getTime()
                            + "ms" );
        }

        /*
         * Add characteristics that have values matching the query; this pulls in items not associated with ontology
         * terms (free text). We do this here so we can apply the query logic to the matches.
         */
        if ( cs.size() < SearchServiceImpl.MAX_CHARACTERISTIC_SEARCH_RESULTS ) {
            String dbQueryString = query.replaceAll( "\\*", "" ); // note I changed the order of search operations so
            // this might not be wanted.
            Collection<Characteristic> valueMatches = characteristicService.findByValue( classes, dbQueryString );

            if ( valueMatches != null && !valueMatches.isEmpty() ) {
                cs.addAll( valueMatches );

                if ( watch.getTime() > 1000 ) {
                    SearchServiceImpl.log
                            .info( "Found " + valueMatches.size() + " characteristics matching value '" + query
                                    + "' in " + watch.getTime() + "ms" );
                }
                watch.reset();
                watch.start();
            }
        }

        if ( cs.size() < SearchServiceImpl.MAX_CHARACTERISTIC_SEARCH_RESULTS ) {

            /*
             * Identify initial set of matches to the query.
             */
            Collection<OntologyTerm> matchingTerms = ontologyService.findTerms( query );

            if ( watch.getTime() > 1000 ) {
                SearchServiceImpl.log
                        .info( "Found " + matchingTerms.size() + " ontology classes matching '" + query + "' in "
                                + watch.getTime() + "ms" );
            }

            /*
             * Search for child terms.
             */
            if ( !matchingTerms.isEmpty() ) {

                for ( OntologyTerm term : matchingTerms ) {
                    /*
                     * In this loop, each term is a match directly to our query, and we do a depth-first fetch of the
                     * children.
                     */
                    String uri = term.getUri();
                    if ( StringUtils.isBlank( uri ) )
                        continue;

                    int sizeBefore = cs.size();
                    this.getCharacteristicsAnnotatedToChildren( classes, term, cs );

                    if ( SearchServiceImpl.log.isDebugEnabled() && cs.size() > sizeBefore ) {
                        SearchServiceImpl.log
                                .debug( ( cs.size() - sizeBefore ) + " characteristics matching children term of "
                                        + term );
                    }

                    if ( cs.size() >= SearchServiceImpl.MAX_CHARACTERISTIC_SEARCH_RESULTS ) {
                        break;
                    }
                }

                if ( watch.getTime() > 1000 ) {
                    SearchServiceImpl.log.info( "Found " + cs.size() + " characteristics for '" + query
                            + "' including child terms in " + watch.getTime() + "ms" );
                }
                watch.reset();
                watch.start();

            }
        }

        /*
         * Retrieve the owner objects
         */
        watch.reset();
        watch.start();
        Collection<SearchResult> matchingEntities = this.getAnnotatedEntities( classes, cs );

        if ( watch.getTime() > 1000 ) {
            SearchServiceImpl.log
                    .info( "Retrieved " + matchingEntities.size() + " entities via characteristics for '" + query
                            + "' in " + watch.getTime() + "ms" );
        }

        if ( SearchServiceImpl.log.isDebugEnabled() )
            SearchServiceImpl.log.debug( "End search for " + query );

        return matchingEntities;
    }

    /**
     * Recursively
     */
    private void getCharacteristicsAnnotatedToChildren( Collection<Class<?>> classes, OntologyTerm term,
            Collection<Characteristic> results ) {

        Collection<OntologyTerm> children = this.getDirectChildTerms( term );

        /*
         * Find occurrences of these terms in our system. This is fast, so long as there aren't too many.
         */
        if ( !children.isEmpty() ) {
            Collection<String> uris = new ArrayList<>();
            for ( OntologyTerm ontologyTerm : children ) {
                if ( ontologyTerm.getUri() == null )
                    continue;
                uris.add( ontologyTerm.getUri() );
            }

            if ( !uris.isEmpty() ) {
                Collection<SearchResult> dbhits = this
                        .dbHitsToSearchResult( characteristicService.findByUri( classes, uris ), null );
                for ( SearchResult crs : dbhits ) {
                    results.add( ( Characteristic ) crs.getResultObject() );
                }
            }
        }

        if ( results.size() >= SearchServiceImpl.MAX_CHARACTERISTIC_SEARCH_RESULTS ) {
            return;
        }

        for ( OntologyTerm child : children ) {
            this.getCharacteristicsAnnotatedToChildren( classes, child, results );
        }

    }

    /**
     * A Compass search on array designs.
     *
     * @return {@link Collection}
     */
    private Collection<SearchResult> compassArrayDesignSearch( SearchSettings settings ) {
        return this.compassSearch( compassArray, settings );
    }

    private Collection<SearchResult> compassBibliographicReferenceSearch( SearchSettings settings ) {
        return this.compassSearch( compassBibliographic, settings );
    }

    /**
     * A compass backed search that finds biosequences that match the search string. Searches the gene and probe indexes
     * for matches then converts those results to biosequences
     *
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *                                  for the genes are added to the final results.
     */
    private Collection<SearchResult> compassBioSequenceSearch( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults ) {

        Collection<SearchResult> results = this.compassSearch( compassBiosequence, settings );

        Collection<SearchResult> geneResults;
        if ( previousGeneSearchResults == null ) {
            SearchServiceImpl.log.info( "Biosequence Search:  running gene search with " + settings.getQuery() );
            geneResults = this.compassGeneSearch( settings );
        } else {
            SearchServiceImpl.log.info( "Biosequence Search:  using previous results" );
            geneResults = previousGeneSearchResults;
        }

        Map<Gene, SearchResult> genes = new HashMap<>();
        for ( SearchResult sr : geneResults ) {
            Object resultObject = sr.getResultObject();
            if ( Gene.class.isAssignableFrom( resultObject.getClass() ) ) {
                genes.put( ( Gene ) resultObject, sr );
            } else {
                // see bug 1774 -- may not be happening any more.
                SearchServiceImpl.log
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

    private Collection<SearchResult> compassCompositeSequenceSearch( final SearchSettings settings ) {
        return this.compassSearch( compassProbe, settings );
    }

    /**
     * A compass search on expressionExperiments.
     *
     * @return {@link Collection}
     */
    private Collection<SearchResult> compassExpressionSearch( SearchSettings settings ) {
        return this.compassSearch( compassExpression, settings );
    }

    private Collection<SearchResult> compassGeneSearch( final SearchSettings settings ) {
        return this.compassSearch( compassGene, settings );
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
                return SearchServiceImpl.this.performSearch( settings, session );
            }
        } );
        if ( SearchServiceImpl.log.isDebugEnabled() ) {
            SearchServiceImpl.log
                    .debug( "Compass search via " + bean.getSettings().getSetting( "compass.name" ) + " : " + settings
                            + " -> " + searchResults.size() + " hits" );
        }
        return searchResults;
    }

    /**
     * Search by name of the composite sequence as well as gene.
     */
    private Collection<SearchResult> compositeSequenceSearch( SearchSettings settings ) {

        StopWatch watch = this.startTiming();

        /*
         * FIXME: this at least partly ignores any array design that was set as a restriction, especially in a gene
         * search.
         */

        // Skip compass searching of composite sequences because it only bloats the results.
        Collection<SearchResult> allResults = new HashSet<>( this.databaseCompositeSequenceSearch( settings ) );

        /*
         * This last step is needed because the compassSearch for compositeSequences returns bioSequences too.
         */
        Collection<SearchResult> finalResults = new HashSet<>();
        for ( SearchResult sr : allResults ) {
            if ( CompositeSequence.class.isAssignableFrom( sr.getResultClass() ) ) {
                finalResults.add( sr );
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Composite sequence search for '" + settings + "' took " + watch.getTime() + " ms, "
                            + finalResults.size() + " results." );
        return finalResults;
    }

    private List<SearchResult> convertEntitySearchResutsToValueObjectsSearchResults(
            Collection<SearchResult> searchResults ) {
        List<SearchResult> convertedSearchResults = new ArrayList<>();
        for ( SearchResult searchResult : searchResults ) {
            // this is a special case ... for some reason.
            if ( BioSequence.class.isAssignableFrom( searchResult.getResultClass() ) ) {
                SearchResult convertedSearchResult = new SearchResult( BioSequenceValueObject
                        .fromEntity( bioSequenceService.thaw( ( BioSequence ) searchResult.getResultObject() ) ),
                        searchResult.getScore(), searchResult.getHighlightedText() );
                convertedSearchResults.add( convertedSearchResult );
            } else {
                convertedSearchResults.add( searchResult );
            }
        }
        return convertedSearchResults;
    }

    /**
     * Searches the DB for array designs which have composite sequences whose names match the given search string.
     * Because of the underlying database search, this is acl aware. That is, returned array designs are filtered based
     * on access control list (ACL) permissions.
     */
    private Collection<SearchResult> databaseArrayDesignSearch( SearchSettings settings ) {

        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = this.startTiming();

        Collection<ArrayDesign> adSet = new HashSet<>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( settings.getQuery() );
        for ( CompositeSequence sequence : matchedCs ) {
            adSet.add( sequence.getArrayDesign() );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Array Design Composite Sequence DB search for " + settings + " took " + watch.getTime()
                            + " ms" + " found " + adSet.size() + " Ads" );

        return this.dbHitsToSearchResult( adSet, null );

    }

    /**
     * A database search for biosequences. Biosequence names are already indexed by compass...
     */
    private Collection<SearchResult> databaseBioSequenceSearch( SearchSettings settings ) {

        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = this.startTiming();

        String searchString = settings.getQuery();

        // replace * with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );

        Collection<BioSequence> bs = bioSequenceService.findByName( inexactString );
        // bioSequenceService.thawRawAndProcessed( bs );
        Collection<SearchResult> bioSequenceList = new HashSet<>( this.dbHitsToSearchResult( bs, null ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "BioSequence DB search for " + searchString + " took " + watch.getTime() + " ms and found"
                            + bioSequenceList.size() + " BioSequences" );

        return bioSequenceList;
    }

    /**
     * Takes a list of ontology terms, and classes of objects of interest to be returned. Looks through the
     * characteristic table for an exact match with the given ontology terms. Only tries to match the uri's.
     *
     * @param classes Class of objects to restrict the search to (typically ExpressionExperiment.class, for
     *                example).
     * @param terms   A list of ontology terms to search for
     * @return Collection of search results for the objects owning the found characteristics, where the owner is of
     * class clazz
     */
    private Collection<SearchResult> databaseCharacteristicExactUriSearchForOwners( Collection<Class<?>> classes,
            Collection<OntologyTerm> terms ) {

        // Collection<Characteristic> characteristicValueMatches = new ArrayList<Characteristic>();
        Collection<Characteristic> characteristicURIMatches = new ArrayList<>();

        for ( OntologyTerm term : terms ) {
            // characteristicValueMatches.addAll( characteristicService.findByValue( term.getUri() ));
            characteristicURIMatches.addAll( characteristicService.findByUri( classes, term.getUri() ) );
        }

        Map<Characteristic, Object> parentMap = characteristicService.getParents( classes, characteristicURIMatches );
        // parentMap.putAll( characteristicService.getParents(characteristicValueMatches ) );

        return this.filterCharacteristicOwnersByClass( classes, parentMap );
    }

    /**
     * Search the DB for composite sequences and the genes that are matched to them.
     */
    private Collection<SearchResult> databaseCompositeSequenceSearch( final SearchSettings settings ) {

        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = this.startTiming();

        Set<Gene> geneSet = new HashSet<>();

        String searchString = settings.getQuery();
        ArrayDesign ad = settings.getPlatformConstraint();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = new HashSet<>();
        if ( ad != null ) {
            CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
            if ( cs != null )
                matchedCs.add( cs );
        } else {
            matchedCs = compositeSequenceService.findByName( searchString );
        }

        /*
         * In case the query _is_ a gene
         */
        Collection<SearchResult> rawGeneResults = this.databaseGeneSearch( settings );
        for ( SearchResult searchResult : rawGeneResults ) {
            Object j = searchResult.getResultObject();
            if ( Gene.class.isAssignableFrom( j.getClass() ) ) {
                geneSet.add( ( Gene ) j );
            }
        }

        for ( Gene g : geneSet ) {
            if ( settings.getPlatformConstraint() != null ) {
                matchedCs.addAll( compositeSequenceService.findByGene( g, settings.getPlatformConstraint() ) );
            } else {
                matchedCs.addAll( compositeSequenceService.findByGene( g ) );
            }
        }

        // search by associated genes.
        for ( CompositeSequence sequence : matchedCs ) {
            geneSet.addAll( compositeSequenceService.getGenes( sequence ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Gene composite sequence DB search " + searchString + " took " + watch.getTime() + " ms, "
                            + geneSet.size() + " items." );

        Collection<SearchResult> results = this.dbHitsToSearchResult( geneSet, null );

        results.addAll( this.dbHitsToSearchResult( matchedCs, null ) );

        return results;
    }

    /**
     * Does search on exact string by: id, name and short name. This only returns results if these fields match exactly,
     * but it's fast.
     *
     * @return {@link Collection}
     */
    private Collection<SearchResult> databaseExpressionExperimentSearch( final SearchSettings settings ) {

        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = this.startTiming();

        Map<ExpressionExperiment, String> results = new HashMap<>();
        String query = StringEscapeUtils.unescapeJava( settings.getQuery() );
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByName( query );
        if ( !ees.isEmpty() ) {
            for ( ExpressionExperiment ee : ees ) {
                results.put( ee, ee.getName() );
            }
        } else {
            ExpressionExperiment ee = expressionExperimentService.findByShortName( query );
            if ( ee != null ) {
                results.put( ee, ee.getShortName() );
            } else {

                ees = expressionExperimentService.findByAccession( query );
                for ( ExpressionExperiment e : ees ) {
                    results.put( e, e.getId().toString() );
                }

                if ( results.isEmpty() ) {
                    try {
                        // maybe user put in a primary key value.
                        ee = expressionExperimentService.load( new Long( query ) );
                        if ( ee != null )
                            results.put( ee, ee.getId().toString() );
                    } catch ( NumberFormatException e ) {
                        // no-op - it's not an ID.
                    }
                }
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log.info( "DB Expression Experiment search for " + settings + " took " + watch.getTime()
                    + " ms and found " + results.size() + " EEs" );

        return this.dbHitsToSearchResult( results );
    }

    /**
     * Search the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
     * tables
     */
    private Collection<SearchResult> databaseGeneSearch( SearchSettings settings ) {

        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = this.startTiming();
        String searchString = StringEscapeUtils.unescapeJava( settings.getQuery() );
        if ( StringUtils.isBlank( searchString ) )
            return new HashSet<>();

        Collection<SearchResult> results = new HashSet<>();

        /*
         * First search by accession. If we find it, stop.
         */
        Gene result = null;
        try {
            result = geneService.findByNCBIId( Integer.parseInt( searchString ) );
        } catch ( NumberFormatException e ) {
            //
        }
        if ( result != null ) {
            results.add( this.dbHitToSearchResult( result ) );
        } else {
            result = geneService.findByAccession( searchString, null );
            if ( result != null ) {
                results.add( this.dbHitToSearchResult( result ) );
            }
        }
        if ( results.size() > 0 ) {
            this.filterByTaxon( settings, results, true );
            watch.stop();
            if ( watch.getTime() > 1000 )
                SearchServiceImpl.log
                        .info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                                + results.size() + " genes" );
            return results;
        }

        // replace * at end with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*$" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );
        // note that at this point, the inexactString might not have a wildcard - only if the user asked for it.

        String exactString = inexactString.replaceAll( "%", "" );

        // if the query is shortish, always do a wild card search. This gives better behavior in 'live
        // search' situations. If we do wildcards on very short queries we get too many results.
        Collection<Gene> geneSet = new HashSet<>();
        if ( searchString.length() <= 2 ) {
            // case 0: we got no result syet, or user entered a very short string. We search only for exact matches.
            geneSet.addAll( geneService.findByOfficialSymbolInexact( exactString ) );
        } else if ( inexactString.endsWith( "%" ) ) {
            // case 1: user explicitly asked for wildcard. We allow this on strings of length 3 or more.
            geneSet.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );
        } else if ( searchString.length() > 3 ) {
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
         * If we found a match using official symbol or name, don't bother with this
         */
        if ( geneSet.isEmpty() ) {
            geneSet.addAll( geneService.findByAlias( exactString ) );
            geneSet.addAll( geneProductService.getGenesByName( exactString ) );
            geneSet.addAll( geneProductService.getGenesByNcbiId( exactString ) );
            geneSet.addAll( bioSequenceService.getGenesByAccession( exactString ) );
            geneSet.addAll( bioSequenceService.getGenesByName( exactString ) );
            geneSet.add( geneService.findByEnsemblId( exactString ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                            + geneSet.size() + " genes" );

        results = this.dbHitsToSearchResult( geneSet, null );
        this.filterByTaxon( settings, results, true );
        return results;
    }

    /**
     * Convert hits from database searches into SearchResults.
     */
    private Collection<SearchResult> dbHitsToSearchResult( Collection<?> entities, String matchText ) {
        return this.dbHitsToSearchResult( entities, null, matchText );
    }

    /**
     * Convert hits from database searches into SearchResults.
     *
     * @param compassHitDerivedFrom SearchResult that these entities were derived from. For example, if you
     *                              compass-searched for genes, and then used the genes to get sequences from the database, the gene is
     *                              compassHitsDerivedFrom. If null, we treat this as a direct hit.
     */
    private List<SearchResult> dbHitsToSearchResult( Collection<?> entities, SearchResult compassHitDerivedFrom,
            String matchText ) {
        StopWatch timer = this.startTiming();
        List<SearchResult> results = new ArrayList<>();
        for ( Object e : entities ) {
            if ( e == null ) {
                SearchServiceImpl.log.warn( "Null search result object" );
                continue;
            }
            SearchResult esr = this.dbHitToSearchResult( compassHitDerivedFrom, e, matchText );
            results.add( esr );
        }
        if ( timer.getTime() > 1000 ) {
            SearchServiceImpl.log.info( "Unpack " + results.size() + " search resultsS: " + timer.getTime() + "ms" );
        }
        return results;
    }

    /**
     * Convert hits from database searches into SearchResults.
     */
    private List<SearchResult> dbHitsToSearchResult( Map<?, String> entities ) {
        List<SearchResult> results = new ArrayList<>();
        for ( Object e : entities.keySet() ) {
            SearchResult esr = this.dbHitToSearchResult( null, e, entities.get( e ) );
            results.add( esr );
        }
        return results;
    }

    private SearchResult dbHitToSearchResult( Object e ) {
        return this.dbHitToSearchResult( null, e, null );
    }

    /**
     * @param text that matched the query (for highlighting)
     */
    private SearchResult dbHitToSearchResult( SearchResult compassHitDerivedFrom, Object e, String text ) {
        SearchResult esr;
        if ( compassHitDerivedFrom != null && text == null ) {
            esr = new SearchResult( e, compassHitDerivedFrom.getScore() * SearchServiceImpl.INDIRECT_DB_HIT_PENALTY );
            esr.setHighlightedText( compassHitDerivedFrom.getHighlightedText() );
        } else {
            esr = new SearchResult( e, 1.0, text );
        }
        return esr;
    }

    private void debugParentFetch( Map<Characteristic, Object> parentMap ) {
        /*
         * This is purely debugging.
         */
        if ( parentMap.size() > 0 ) {
            if ( SearchServiceImpl.log.isDebugEnabled() )
                SearchServiceImpl.log.debug( "Found " + parentMap.size() + " owners for " + parentMap.keySet().size()
                        + " characteristics:" );
        }
    }

    /**
     * Find phenotypes.
     */
    private Collection<SearchResult> phenotypeSearch( SearchSettings settings ) {
        return this.dbHitsToSearchResult(
                this.phenotypeAssociationManagerService.searchInDatabaseForPhenotype( settings.getQuery() ), null );
    }

    private Collection<SearchResult> experimentSetSearch( SearchSettings settings ) {
        Collection<SearchResult> results = this
                .dbHitsToSearchResult( this.experimentSetService.findByName( settings.getQuery() ), null );

        results.addAll( this.compassSearch( compassExperimentSet, settings ) );
        return results;
    }

    /**
     * A general search for expression experiments. This search does both an database search and a compass search.
     * A problem with this is that we cap the number of results that can be returned. This could be a limitation for
     * applications like building data set groups. Thus MAX_CHARACTERISTIC_SEARCH_RESULTS should not be too low.
     *
     * @return {@link Collection}
     */
    private Collection<SearchResult> expressionExperimentSearch( final SearchSettings settings ) {
        StopWatch watch = this.startTiming();

        SearchServiceImpl.log
                .info( "Starting search for " + settings.getQuery() + " in taxon: " + settings.getTaxon() );

        Collection<SearchResult> results = new HashSet<>();

        if ( settings.getUseDatabase() ) {
            results.addAll( this.databaseExpressionExperimentSearch( settings ) );
            if ( watch.getTime() > 1000 )
                SearchServiceImpl.log
                        .info( "Expression Experiment database search for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );
            watch.reset();
            watch.start();
        }

        if ( settings.getUseIndices() && results.size() < SearchServiceImpl.MAX_CHARACTERISTIC_SEARCH_RESULTS ) {
            results.addAll( this.compassExpressionSearch( settings ) );
            if ( watch.getTime() > 1000 )
                SearchServiceImpl.log
                        .info( "Expression Experiment index search for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );
            watch.reset();
            watch.start();
        }

        if ( results.size() < SearchServiceImpl.MAX_CHARACTERISTIC_SEARCH_RESULTS ) {
            /*
             * Try a more thorough search. This is slower; calls to ontologySearchAnnotatedObject take a long time
             */
            if ( settings.getUseCharacteristics() ) {
                results.addAll( this.characteristicExpressionExperimentSearch( settings ) );
            }
            if ( watch.getTime() > 1000 )
                SearchServiceImpl.log
                        .info( "Expression Experiment ontology search for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );
            watch.reset();
            watch.start();
        }

        /*
         * Find data sets that match the platform
         */
        if ( results.size() == 0 ) {
            Collection<SearchResult> matchingPlatforms = this.arrayDesignSearch( settings, null );
            for ( SearchResult adRes : matchingPlatforms ) {
                if ( adRes.getResultObject() instanceof ArrayDesign ) {
                    ArrayDesign ad = ( ArrayDesign ) adRes.getResultObject();
                    Collection<ExpressionExperiment> expressionExperiments = this.arrayDesignService
                            .getExpressionExperiments( ad );
                    if ( expressionExperiments.size() > 0 )
                        results.addAll( this.dbHitsToSearchResult( expressionExperiments, null ) );
                }
            }
            if ( watch.getTime() > 1000 )
                SearchServiceImpl.log
                        .info( "Expression Experiment platform search for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );
            watch.reset();
            watch.start();
        }

        if ( results.size() == 0 ) {
            /*
             * Search for bib refs
             */
            List<BibliographicReferenceValueObject> bibrefs = bibliographicReferenceService
                    .search( settings.getQuery() );

            if ( !bibrefs.isEmpty() ) {
                Collection<BibliographicReference> refs = new HashSet<>();
                Collection<SearchResult> r = this.compassBibliographicReferenceSearch( settings );
                for ( SearchResult searchResult : r ) {
                    refs.add( ( BibliographicReference ) searchResult.getResultObject() );
                }

                Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this.bibliographicReferenceService
                        .getRelatedExperiments( refs );
                for ( Entry<BibliographicReference, Collection<ExpressionExperiment>> e : relatedExperiments
                        .entrySet() ) {
                    results.addAll( this.dbHitsToSearchResult( e.getValue(), null ) );
                }
                if ( watch.getTime() > 1000 )
                    SearchServiceImpl.log
                            .info( "Expression Experiment publication search for '" + settings + "' took " + watch
                                    .getTime() + " ms, " + results.size() + " hits." );
                watch.reset();
                watch.start();
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Expression Experiment search for '" + settings + "' took " + watch.getTime() + " ms, "
                            + results.size() + " hits." );

        return results;
    }

    /**
     * @param excludeWithoutTaxon if true: If the SearchResults have no "getTaxon" method then the results will get
     *                            filtered out Results with no taxon associated will also get removed.
     */
    private void filterByTaxon( SearchSettings settings, Collection<SearchResult> results,
            boolean excludeWithoutTaxon ) {
        if ( settings.getTaxon() == null ) {
            return;
        }
        Collection<SearchResult> toRemove = new HashSet<>();
        Taxon t = settings.getTaxon();

        if ( results == null )
            return;

        for ( SearchResult sr : results ) {

            Object o = sr.getResultObject();
            try {

                Taxon currentTaxon;

                if ( o instanceof ExpressionExperiment ) {
                    ExpressionExperiment ee = ( ExpressionExperiment ) o;
                    currentTaxon = expressionExperimentService.getTaxon( ee );

                } else if ( o instanceof ExpressionExperimentSet ) {
                    ExpressionExperimentSet ees = ( ExpressionExperimentSet ) o;
                    currentTaxon = ees.getTaxon();

                } else if ( o instanceof Gene ) {
                    Gene gene = ( Gene ) o;
                    currentTaxon = gene.getTaxon();

                } else if ( o instanceof GeneSet ) {
                    GeneSet geneSet = ( GeneSet ) o;
                    currentTaxon = geneSetService.getTaxon( geneSet );

                } else if ( o instanceof CharacteristicValueObject ) {
                    CharacteristicValueObject charVO = ( CharacteristicValueObject ) o;
                    currentTaxon = taxonDao.findByCommonName( charVO.getTaxon() );

                } else {
                    Method m = o.getClass().getMethod( "getTaxon" );
                    currentTaxon = ( Taxon ) m.invoke( o );
                }

                if ( currentTaxon == null || !currentTaxon.getId().equals( t.getId() ) ) {
                    if ( currentTaxon == null ) {
                        // Sanity check for bad data in db (could happen if EE has no samples). Can happen that
                        // searchResults have a vaild getTaxon method
                        // but the method returns null (shouldn't make it this far)
                        SearchServiceImpl.log.debug( "Object has getTaxon method but it returns null. Obj is: " + o );
                    }
                    toRemove.add( sr );
                }
            } catch ( SecurityException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( NoSuchMethodException e ) {
                /*
                 * In case of a programming error where the results don't have a taxon at all, we assume we should
                 * filter them out but issue a warning.
                 */
                if ( excludeWithoutTaxon ) {
                    toRemove.add( sr );
                    SearchServiceImpl.log
                            .warn( "No getTaxon method for: " + o.getClass() + ".  Filtering from results. Error was: "
                                    + e );
                }

            }
        }
        results.removeAll( toRemove );
    }

    private Collection<SearchResult> filterCharacteristicOwnersByClass( Collection<Class<?>> classes,
            Map<Characteristic, Object> characteristic2entity ) {

        Collection<BioMaterial> biomaterials = new HashSet<>();
        Collection<FactorValue> factorValues = new HashSet<>();
        Collection<SearchResult> results = new HashSet<>();
        for ( Characteristic c : characteristic2entity.keySet() ) {
            Object o = characteristic2entity.get( c );
            for ( Class<?> clazz : classes ) {
                if ( clazz.isAssignableFrom( o.getClass() ) ) {
                    String matchedText = c.getValue();

                    if ( o instanceof BioMaterial ) {
                        biomaterials.add( ( BioMaterial ) o );

                    } else if ( o instanceof FactorValue ) {
                        factorValues.add( ( FactorValue ) o );
                    } else {

                        if ( c instanceof VocabCharacteristic && c.getValueUri() != null ) {
                            matchedText =
                                    "Ontology term: <a href=\"" + Settings.getRootContext() + "/searcher.html?query="
                                            + c.getValueUri() + "\">" + matchedText + "</a>";
                        }
                        results.add( new SearchResult( o, 1.0, matchedText ) );
                    }
                }
            }
        }

        this.addEEeByFactorvalues( results, factorValues );

        if ( biomaterials.size() > 0 ) {
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByBioMaterials( biomaterials );
            for ( ExpressionExperiment ee : ees ) {
                results.add( new SearchResult( ee, SearchServiceImpl.INDIRECT_DB_HIT_PENALTY,
                        "BioMaterial characteristic" ) );
            }
        }
        return results;
    }

    /**
     * Combines compass style search, the db style search, and the compositeSequence search and returns 1 combined list
     * with no duplicates.
     *
     * @param returnOnDbHit if true and if there is a match for a gene from the database, return immediately - much
     *                      faster
     */
    private Collection<SearchResult> geneSearch( final SearchSettings settings, boolean returnOnDbHit ) {

        StopWatch watch = this.startTiming();

        String searchString = settings.getQuery();

        Collection<SearchResult> geneDbList = this.databaseGeneSearch( settings );

        if ( returnOnDbHit && geneDbList.size() > 0 ) {
            return geneDbList;
        }

        Set<SearchResult> combinedGeneList = new HashSet<>( geneDbList );

        Collection<SearchResult> geneCompassList = this.compassGeneSearch( settings );
        combinedGeneList.addAll( geneCompassList );

        if ( combinedGeneList.isEmpty() ) {
            Collection<SearchResult> geneCsList = this.databaseCompositeSequenceSearch( settings );
            for ( SearchResult res : geneCsList ) {
                if ( res.getResultClass().isAssignableFrom( Gene.class ) )
                    combinedGeneList.add( res );
            }
        }

        /*
         * Possibly search for genes linked via a phenotype, but only if we don't have anything here.
         *
         */
        if ( combinedGeneList.isEmpty() ) {
            Collection<CharacteristicValueObject> phenotypeTermHits = this.phenotypeAssociationManagerService
                    .searchInDatabaseForPhenotype( settings.getQuery() );

            for ( CharacteristicValueObject phenotype : phenotypeTermHits ) {
                Set<String> phenotypeUris = new HashSet<>();
                phenotypeUris.add( phenotype.getValueUri() );

                // DATABASE HIT!
                Collection<GeneEvidenceValueObject> phenotypeGenes = phenotypeAssociationManagerService
                        .findCandidateGenes( phenotypeUris, settings.getTaxon() );

                if ( !phenotypeGenes.isEmpty() ) {
                    SearchServiceImpl.log
                            .info( phenotypeGenes.size() + " genes associated with " + phenotype + " (via query='"
                                    + settings.getQuery() + "')" );

                    for ( GeneEvidenceValueObject gvo : phenotypeGenes ) {
                        Gene g = Gene.Factory.newInstance();
                        g.setId( gvo.getId() );
                        g.setTaxon( settings.getTaxon() );
                        SearchResult sr = new SearchResult( g );
                        sr.setHighlightedText( phenotype.getValue() + " (" + phenotype.getValueUri() + ")" );

                        // if ( gvo.getScore() != null ) {
                        // TODO If we get evidence quality, use that in the score.
                        // }
                        sr.setScore( 1.0 ); // maybe lower, if we do this search when combinedGeneList is nonempty.
                        combinedGeneList.add( sr );
                    }
                    if ( combinedGeneList.size() > 100 /* some limit */ ) {
                        break;
                    }
                }
            }
        }

        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Gene search for " + searchString + " took " + watch.getTime() + " ms; " + combinedGeneList
                            .size() + " results." );
        return combinedGeneList;
    }

    private Collection<SearchResult> geneSetSearch( SearchSettings settings ) {
        Collection<SearchResult> hits;
        if ( settings.getTaxon() != null ) {
            hits = this
                    .dbHitsToSearchResult( this.geneSetService.findByName( settings.getQuery(), settings.getTaxon() ),
                            null );
        } else {
            hits = this.dbHitsToSearchResult( this.geneSetService.findByName( settings.getQuery() ), null );
        }

        hits.addAll( this.compassSearch( compassGeneSet, settings ) );
        return hits;
    }

    /**
     * Given classes to search and characteristics,
     *
     * @param classes Which classes of entities to look for
     */
    private Collection<SearchResult> getAnnotatedEntities( Collection<Class<?>> classes,
            Collection<Characteristic> cs ) {

        Map<Characteristic, Object> characteristic2entity = characteristicService.getParents( classes, cs );
        Collection<SearchResult> matchedEntities = this
                .filterCharacteristicOwnersByClass( classes, characteristic2entity );

        if ( SearchServiceImpl.log.isDebugEnabled() ) {
            this.debugParentFetch( characteristic2entity );
        }
        return matchedEntities;
    }

    /**
     * @return List of ids for the entities held by the search results.
     */
    private List<Long> getIds( List<SearchResult> searchResults ) {
        List<Long> list = new ArrayList<>();
        for ( SearchResult r : searchResults ) {
            list.add( r.getId() );
        }
        assert list.size() == searchResults.size();
        return list;
    }

    private Collection<SearchResult> getSearchResults( CompassHits hits ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<SearchResult> results = new HashSet<>();
        /*
         * Note that hits come in decreasing score order.
         */
        for ( int i = 0, len = Math.min( SearchServiceImpl.MAX_LUCENE_HITS, hits.getLength() ); i < len; i++ ) {

            SearchResult r = new SearchResult( hits.data( i ) );

            /*
             * Always give compass hits a lower score so they can be differentiated from exact database hits.
             */
            r.setScore( hits.score( i ) * SearchServiceImpl.COMPASS_HIT_SCORE_PENALTY_FACTOR );

            this.getHighlightedText( hits, i, r );

            if ( SearchServiceImpl.log.isDebugEnabled() )
                SearchServiceImpl.log.debug( i + " " + hits.score( i ) + " " + r );

            results.add( r );
        }

        if ( timer.getTime() > 100 ) {
            SearchServiceImpl.log.info( results.size() + " hits retrieved (out of " + Math
                    .min( SearchServiceImpl.MAX_LUCENE_HITS, hits.getLength() ) + " raw hits tested) in " + timer
                    .getTime() + "ms" );
        }
        if ( timer.getTime() > 5000 ) {
            SearchServiceImpl.log
                    .info( "****Extremely long Lucene Search processing! " + results.size() + " hits retrieved (out of "
                            + Math.min( SearchServiceImpl.MAX_LUCENE_HITS, hits.getLength() ) + " raw hits tested) in "
                            + timer.getTime() + "ms" );
        }

        return results;
    }

    private void getHighlightedText( CompassHits hits, int i, SearchResult r ) {
        CompassHighlightedText highlightedText = hits.highlightedText( i );
        if ( highlightedText != null && highlightedText.getHighlightedText() != null ) {
            r.setHighlightedText( highlightedText.getHighlightedText() );
        } else {
            if ( SearchServiceImpl.log.isDebugEnabled() )
                SearchServiceImpl.log.debug( "No highlighted text for " + r );
        }
    }

    private Map<Class<?>, List<SearchResult>> getSortedLimitedResults( SearchSettings settings,
            List<SearchResult> rawResults, boolean fillObjects ) {

        Map<Class<?>, List<SearchResult>> results = new HashMap<>();
        Collections.sort( rawResults );

        results.put( ArrayDesign.class, new ArrayList<SearchResult>() );
        results.put( BioSequence.class, new ArrayList<SearchResult>() );
        results.put( BibliographicReference.class, new ArrayList<SearchResult>() );
        results.put( CompositeSequence.class, new ArrayList<SearchResult>() );
        results.put( ExpressionExperiment.class, new ArrayList<SearchResult>() );
        results.put( Gene.class, new ArrayList<SearchResult>() );
        results.put( GeneSet.class, new ArrayList<SearchResult>() );
        results.put( ExpressionExperimentSet.class, new ArrayList<SearchResult>() );
        results.put( Characteristic.class, new ArrayList<SearchResult>() );
        results.put( CharacteristicValueObject.class, new ArrayList<SearchResult>() );

        /*
         * Get the top N results for each class.
         */
        for ( SearchResult sr : rawResults ) {
            Class<?> resultClass = sr.getResultClass();
            List<SearchResult> resultsForClass = results.get( resultClass );
            if ( resultsForClass != null && resultsForClass.size() < settings.getMaxResults() ) {
                resultsForClass.add( sr );
            }
        }

        if ( fillObjects ) {
            /*
             * Now retrieve the entities and put them in the SearchResult. Entities that are filtered out by the
             * SecurityInterceptor will be removed at this stage.
             */
            for ( Class<?> clazz : results.keySet() ) {
                List<SearchResult> r = results.get( clazz );
                if ( r.isEmpty() )
                    continue;
                Map<Long, SearchResult> rMap = new HashMap<>();
                for ( SearchResult searchResult : r ) {
                    if ( !rMap.containsKey( searchResult.getId() ) || ( rMap.get( searchResult.getId() ).getScore()
                            < searchResult.getScore() ) ) {
                        rMap.put( searchResult.getId(), searchResult );
                    }
                }

                Collection<?> entities = this.retrieveResultEntities( clazz, r );
                List<SearchResult> filteredResults = new ArrayList<>();
                for ( Object entity : entities ) {
                    Long id = EntityUtils.getId( entity );
                    SearchResult keeper = rMap.get( id );
                    keeper.setResultObject( entity );
                    filteredResults.add( keeper );
                }

                this.filterByTaxon( settings, filteredResults, false );

                results.put( clazz, filteredResults );

            }
        } else {
            for ( SearchResult sr : rawResults ) {
                sr.setResultObject( null );
            }
        }

        List<SearchResult> convertedResults = this
                .convertEntitySearchResutsToValueObjectsSearchResults( results.get( BioSequence.class ) );
        results.put( BioSequenceValueObject.class, convertedResults );
        results.remove( BioSequence.class );

        return results;
    }

    /**
     * Retrieve entities from the persistent store.
     */
    private Collection<?> retrieveResultEntities( Class<?> entityClass, List<SearchResult> results ) {
        List<Long> ids = this.getIds( results );
        if ( ExpressionExperiment.class.isAssignableFrom( entityClass ) ) {
            return expressionExperimentService.load( ids );
        } else if ( ArrayDesign.class.isAssignableFrom( entityClass ) ) {
            return arrayDesignService.load( ids );
        } else if ( CompositeSequence.class.isAssignableFrom( entityClass ) ) {
            return compositeSequenceService.load( ids );
        } else if ( BibliographicReference.class.isAssignableFrom( entityClass ) ) {
            return bibliographicReferenceService.load( ids );
        } else if ( Gene.class.isAssignableFrom( entityClass ) ) {
            return geneService.load( ids );
        } else if ( BioSequence.class.isAssignableFrom( entityClass ) ) {
            return bioSequenceService.load( ids );
        } else if ( GeneSet.class.isAssignableFrom( entityClass ) ) {
            return geneSetService.load( ids );
        } else if ( ExpressionExperimentSet.class.isAssignableFrom( entityClass ) ) {
            return experimentSetService.load( ids );
        } else if ( Characteristic.class.isAssignableFrom( entityClass ) ) {
            Collection<Characteristic> chars = new ArrayList<>();
            for ( Long id : ids ) {
                chars.add( characteristicService.load( id ) );
            }
            return chars;
        } else if ( CharacteristicValueObject.class.isAssignableFrom( entityClass ) ) {
            // TEMP HACK this whole method should not be needed in many cases
            Collection<CharacteristicValueObject> chars = new ArrayList<>();
            for ( SearchResult result : results ) {
                if ( result.getResultClass().isAssignableFrom( CharacteristicValueObject.class ) ) {
                    chars.add( ( CharacteristicValueObject ) result.getResultObject() );
                }
            }
            return chars;
        } else if ( ExpressionExperimentSet.class.isAssignableFrom( entityClass ) ) {
            return experimentSetService.load( ids );
        } else {
            throw new UnsupportedOperationException( "Don't know how to retrieve objects for class=" + entityClass );
        }
    }

    private StopWatch startTiming() {
        StopWatch watch = new StopWatch();
        watch.start();
        return watch;
    }

    /**
     * @return a collection of SearchResults holding all the genes resulting from the search with given SearchSettings.
     */
    private Collection<SearchResult> getGenesFromSettings( SearchSettings settings, boolean webSpeedSearch ) {
        Collection<SearchResult> genes = null;
        if ( settings.getSearchGenes() ) {
            genes = this.geneSearch( settings, webSpeedSearch );
        }
        return genes;
    }

    /**
     * Checks whether settings have the search genes flag and does the search if needed.
     *
     * @param results the results to which should any new results be accreted.
     */
    private void accreteResultsGenes( List<SearchResult> results, SearchSettings settings, boolean webSpeedSearch ) {
        if ( settings.getSearchGenes() ) {
            Collection<SearchResult> genes = this.getGenesFromSettings( settings, webSpeedSearch );
            this.accreteResults( results, genes );
        }
    }

    /**
     * Checks settings for all do-search flags, except for gene (see
     * {@link #accreteResultsGenes(List, SearchSettings, boolean)}), and does the search if needed.
     *
     * @param results the results to which should any new results be accreted.
     * @return same object as given, possibly extended by new items from search.
     */
    private List<SearchResult> accreteResultsOthers( List<SearchResult> results, SearchSettings settings,
            boolean webSpeedSearch ) {

        if ( settings.getSearchExperiments() ) {
            Collection<SearchResult> foundEEs = this.expressionExperimentSearch( settings );
            results.addAll( foundEEs );
        }

        Collection<SearchResult> compositeSequences = null;
        if ( settings.getSearchProbes() ) {
            compositeSequences = this.compositeSequenceSearch( settings );
            this.accreteResults( results, compositeSequences );
        }

        if ( settings.getSearchPlatforms() ) {
            Collection<SearchResult> foundADs = this.arrayDesignSearch( settings, compositeSequences );
            this.accreteResults( results, foundADs );
        }

        if ( settings.getSearchBioSequences() ) {
            Collection<SearchResult> genes = this.getGenesFromSettings( settings, webSpeedSearch );

            Collection<SearchResult> bioSequences = this.bioSequenceSearch( settings, genes );
            this.accreteResults( results, bioSequences );
        }

        if ( settings.getUseGo() ) {
            Collection<SearchResult> ontologyGenes = this.dbHitsToSearchResult(
                    geneSearchService.getGOGroupGenes( settings.getQuery(), settings.getTaxon() ), "From GO group" );
            this.accreteResults( results, ontologyGenes );
        }

        if ( settings.getSearchBibrefs() ) {
            Collection<SearchResult> bibliographicReferences = this.compassBibliographicReferenceSearch( settings );
            this.accreteResults( results, bibliographicReferences );
        }

        if ( settings.getSearchGeneSets() ) {
            Collection<SearchResult> geneSets = this.geneSetSearch( settings );
            this.accreteResults( results, geneSets );
        }

        if ( settings.getSearchExperimentSets() ) {
            Collection<SearchResult> experimentSets = this.experimentSetSearch( settings );
            this.accreteResults( results, experimentSets );
        }

        if ( settings.getSearchPhenotypes() ) {
            Collection<SearchResult> phenotypes = this.phenotypeSearch( settings );
            this.accreteResults( results, phenotypes );
        }

        return results;
    }

    /**
     * Makes no attempt at resolving the search query as a URI. Will tokenize the search query if there are control
     * characters in the String. URI's will get parsed into multiple query terms and lead to bad results.
     *
     * @param settings       Will try to resolve general terms like brain --> to appropriate OntologyTerms and search for
     *                       objects tagged with those terms (if isUseCharacte = true)
     * @param fillObjects    If false, the entities will not be filled in inside the searchsettings; instead, they will be
     *                       nulled (for security purposes). You can then use the id and Class stored in the SearchSettings to load the
     *                       entities at your leisure. If true, the entities are loaded in the usual secure fashion. Setting this to
     *                       false can be an optimization if all you need is the id. Note: filtering by taxon will not be done unless
     *                       objects are filled
     * @param webSpeedSearch if true, this call is probably coming from a web app combo box and results will be limited
     *                       to improve speed
     */
    private Map<Class<?>, List<SearchResult>> generalSearch( SearchSettings settings, boolean fillObjects,
            boolean webSpeedSearch ) {

        settings = SearchSettingsStringUtils.processSettings( settings, this.nameToTaxonMap );

        List<SearchResult> rawResults = new ArrayList<>();

        // do gene first first before we munge the query too much.
        this.accreteResultsGenes( rawResults, settings, webSpeedSearch );

        // some strings of size 1 cause lucene to barf and they were slipping through in multi-term queries, get rid of
        // them
        settings.setQuery( SearchSettingsStringUtils.stripShortTerms( settings.getQuery() ) );

        // If nothing to search return nothing.
        if ( StringUtils.isBlank( settings.getQuery() ) ) {
            return new HashMap<>();
        }

        rawResults = this.accreteResultsOthers( rawResults, settings, webSpeedSearch );

        Map<Class<?>, List<SearchResult>> sortedLimitedResults = this
                .getSortedLimitedResults( settings, rawResults, fillObjects );

        SearchServiceImpl.log.info( "search for: " + settings.getQuery() + " yielded " + rawResults.size()
                + " raw results (final tally may be filtered)" );

        return sortedLimitedResults;
    }

    /**
     * Runs inside Compass transaction
     */
    private Collection<SearchResult> performSearch( SearchSettings settings, CompassSession session ) {
        StopWatch watch = this.startTiming();
        String enhancedQuery = settings.getQuery().trim();

        //noinspection ConstantConditions // Not obvious to me why that would have to be false.
        if ( StringUtils.isBlank( enhancedQuery )
                || enhancedQuery.length() < SearchServiceImpl.MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH
                || enhancedQuery.equals( "*" ) )
            return new ArrayList<>();

        CompassQuery compassQuery = session.queryBuilder().queryString( enhancedQuery ).toQuery();
        SearchServiceImpl.log.debug( "Parsed query: " + compassQuery );

        CompassHits hits = compassQuery.hits();

        // highlighting.
        if ( ( ( SearchSettingsImpl ) settings ).getDoHighlighting() ) {
            if ( session instanceof InternalCompassSession ) { // always ...
                CompassMapping mapping = ( ( InternalCompassSession ) session ).getMapping();
                ResourceMapping[] rootMappings = mapping.getRootMappings();
                // should only be one rootMapping.
                this.process( rootMappings, hits );
            }
        }

        watch.stop();
        if ( watch.getTime() > 100 ) {
            SearchServiceImpl.log
                    .info( "Getting " + hits.getLength() + " lucene hits for " + enhancedQuery + " took " + watch
                            .getTime() + " ms" );
        }
        if ( watch.getTime() > 5000 ) {
            SearchServiceImpl.log
                    .info( "*****Extremely long Lucene Index Search!  " + hits.getLength() + " lucene hits for "
                            + enhancedQuery + " took " + watch.getTime() + " ms" );
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
                } else {
                    String name = m.getName();
                    for ( int i = 0; i < hits.getLength(); i++ ) {
                        try {
                            // we might want to bail as soon as we find something?
                            hits.highlighter( i ).fragment( name );
                            if ( SearchServiceImpl.log.isDebugEnabled() )
                                SearchServiceImpl.log.debug( "Cached " + name );
                        } catch ( Exception e ) {
                            break; // skip this property entirely...
                        }
                    }
                }
            }
        }
    }
}
