/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.ontology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.ontology.providers.OntologyServiceFactory;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.common.description.CharacteristicDao;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Has a static method for finding out which ontologies are loaded into the system and a general purpose find method
 * that delegates to the many ontology services. NOTE: Logging messages from this service are important for tracking
 * changes to annotations.
 *
 * @author pavlidis
 */
@Service
public class OntologyServiceImpl implements OntologyService, InitializingBean {
    /**
     * Throttle how many ontology terms we retrieve. We search the ontologies in a favored order, so we can stop when we
     * find "enough stuff".
     */
    private static final int MAX_TERMS_TO_FETCH = 200;
    private static final Log log = LogFactory.getLog( OntologyServiceImpl.class.getName() );

    private static final Comparator<CharacteristicValueObject> CHARACTERISTIC_VO_COMPARATOR = new CharacteristicComparator();

    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private GeneOntologyService geneOntologyService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private AsyncTaskExecutor taskExecutor;

    @Autowired
    private CellLineOntologyService cellLineOntologyService;
    @Autowired
    private CellTypeOntologyService cellTypeOntologyService;
    @Autowired
    private ChebiOntologyService chebiOntologyService;
    @Autowired
    private DiseaseOntologyService diseaseOntologyService;
    @Autowired
    private ExperimentalFactorOntologyService experimentalFactorOntologyService;
    //    @Deprecated
//    @Autowired
//    private FMAOntologyService fmaOntologyService;
    @Autowired
    private GemmaOntologyService gemmaOntologyService;
    @Autowired
    private HumanDevelopmentOntologyService humanDevelopmentOntologyService;
    @Autowired
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService;
    @Autowired
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService;
    @Autowired
    private MouseDevelopmentOntologyService mouseDevelopmentOntologyService;
    //    @Deprecated
//    @Autowired
//    private NIFSTDOntologyService nifstdOntologyService;
    @Autowired
    private ObiService obiService;
    @Autowired
    private SequenceOntologyService sequenceOntologyService;
    @Autowired
    private UberonOntologyService uberonOntologyService;

    @Autowired
    private List<OntologyServiceFactory<?>> ontologyServiceFactories;

    @Autowired
    private List<ubic.basecode.ontology.providers.OntologyService> ontologyServices;

    private Set<OntologyTerm> categoryTerms = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        List<ubic.basecode.ontology.providers.OntologyService> enabledOntologyServices = ontologyServiceFactories.stream()
                .filter( OntologyServiceFactory::isAutoLoaded )
                .map( factory -> {
                    try {
                        return factory.getObject();
                    } catch ( Exception e ) {
                        throw new RuntimeException( e );
                    }
                } )
                .filter( ubic.basecode.ontology.providers.OntologyService::isEnabled )
                .collect( Collectors.toList() );
        if ( enabledOntologyServices.isEmpty() ) {
            log.info( "No ontology services are enabled, consider enabling them by setting 'load.{name}Ontology' options in Gemma.properties." );
        } else {
            log.info( "The following ontology services are enabled:\n\t" + enabledOntologyServices.stream()
                    .map( ubic.basecode.ontology.providers.OntologyService::toString )
                    .collect( Collectors.joining( "\n\t" ) ) );
        }
        // remove GeneOntologyService, it was originally not included in the list before bean injection was used
        ontologyServices.remove( geneOntologyService );
        initializeCategoryTerms();
    }

    private void countOccurrences( Collection<CharacteristicValueObject> searchResults,
            Map<String, CharacteristicValueObject> previouslyUsedInSystem ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Set<String> uris = new HashSet<>();
        for ( CharacteristicValueObject cvo : searchResults ) {
            uris.add( cvo.getValueUri() );
        }

        Map<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> existingCharacteristicsUsingTheseTerms = characteristicService.countCharacteristicValueUriInByValueUriOrValue( uris );
        for ( Map.Entry<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> c : existingCharacteristicsUsingTheseTerms.entrySet() ) {
            // count up number of usages; see bug 3897
            String key = c.getKey();
            if ( !previouslyUsedInSystem.containsKey( key ) ) {
                if ( OntologyServiceImpl.log.isDebugEnabled() )
                    OntologyServiceImpl.log.debug( "saw " + key + " (" + key + ")" );
                previouslyUsedInSystem.put( key, characteristicByValueCountToValueObject( c.getValue() ) );
            }
        }

        if ( OntologyServiceImpl.log.isDebugEnabled() || ( watch.getTime() > 200
                && previouslyUsedInSystem.size() > 0 ) )
            OntologyServiceImpl.log.warn( String.format(
                    "Found %d matching characteristics used in the database in %d ms  Filtered from initial set of %d",
                    previouslyUsedInSystem.size(), watch.getTime(), existingCharacteristicsUsingTheseTerms.size() ) );
    }

    /**
     * Using the ontology and values in the database, for a search searchQuery given by the client give an ordered list
     * of possible choices
     */
    @Override
    public Collection<CharacteristicValueObject> findExperimentsCharacteristicTags( String searchQueryString,
            boolean useNeuroCartaOntology ) throws OntologySearchException {

        String searchQuery = OntologySearch.stripInvalidCharacters( searchQueryString );

        if ( searchQuery.length() < 3 ) {
            return new HashSet<>();
        }

        // this will do like %search%
        Collection<CharacteristicValueObject> characteristicsFromDatabase = CharacteristicValueObject
                .characteristic2CharacteristicVO( this.characteristicService.findByValue( "%" + searchQuery ) );

        Map<String, CharacteristicValueObject> characteristicFromDatabaseWithValueUri = new HashMap<>();
        Collection<CharacteristicValueObject> characteristicFromDatabaseFreeText = new HashSet<>();

        for ( CharacteristicValueObject characteristicInDatabase : characteristicsFromDatabase ) {

            // flag to let know that it was found in the database
            characteristicInDatabase.setAlreadyPresentInDatabase( true );

            if ( characteristicInDatabase.getValueUri() != null && !characteristicInDatabase.getValueUri()
                    .equals( "" ) ) {
                characteristicFromDatabaseWithValueUri
                        .put( characteristicInDatabase.getValueUri(), characteristicInDatabase );
            } else {
                // free txt, no value uri
                characteristicFromDatabaseFreeText.add( characteristicInDatabase );
            }
        }

        // search the ontology for the given searchTerm, but if already found in the database dont add it again
        Collection<CharacteristicValueObject> characteristicsFromOntology = this
                .findCharacteristicsFromOntology( searchQuery, useNeuroCartaOntology,
                        characteristicFromDatabaseWithValueUri );

        // order to show the the term: 1-exactMatch, 2-startWith, 3-substring and 4- no rule
        // order to show values for each List : 1-From database with Uri, 2- from Ontology, 3- from from database with
        // no Uri
        Collection<CharacteristicValueObject> characteristicsWithExactMatch = new ArrayList<>();
        Collection<CharacteristicValueObject> characteristicsStartWithQuery = new ArrayList<>();
        Collection<CharacteristicValueObject> characteristicsSubstring = new ArrayList<>();
        Collection<CharacteristicValueObject> characteristicsNoRuleFound = new ArrayList<>();

        // from the database with a uri
        this.putCharacteristicsIntoSpecificList( searchQuery, characteristicFromDatabaseWithValueUri.values(),
                characteristicsWithExactMatch, characteristicsStartWithQuery, characteristicsSubstring,
                characteristicsNoRuleFound );
        // from the ontology
        this.putCharacteristicsIntoSpecificList( searchQuery, characteristicsFromOntology,
                characteristicsWithExactMatch, characteristicsStartWithQuery, characteristicsSubstring,
                characteristicsNoRuleFound );
        // from the database with no uri
        this.putCharacteristicsIntoSpecificList( searchQuery, characteristicFromDatabaseFreeText,
                characteristicsWithExactMatch, characteristicsStartWithQuery, characteristicsSubstring,
                characteristicsNoRuleFound );

        List<CharacteristicValueObject> allCharacteristicsFound = new ArrayList<>();
        allCharacteristicsFound.addAll( characteristicsWithExactMatch );
        allCharacteristicsFound.addAll( characteristicsStartWithQuery );
        allCharacteristicsFound.addAll( characteristicsSubstring );
        allCharacteristicsFound.addAll( characteristicsNoRuleFound );

        // limit the size of the returned phenotypes to 100 terms
        if ( allCharacteristicsFound.size() > 100 ) {
            return allCharacteristicsFound.subList( 0, 100 );
        }

        return allCharacteristicsFound;
    }

    @Override
    public Set<OntologyIndividual> findIndividuals( String givenSearch ) throws OntologySearchException {
        String query = OntologySearch.stripInvalidCharacters( givenSearch );
        return searchInThreads( ontology -> {
            StopWatch timer = StopWatch.createStarted();
            try {
                return ontology.findIndividuals( query );
            } finally {
                if ( timer.getTime( TimeUnit.MILLISECONDS ) > 100 ) {
                    log.warn( String.format( "Finding individuals for ontology %s for input query '%s' took %d ms",
                            ontology, givenSearch, timer.getTime( TimeUnit.MILLISECONDS ) ) );
                }
            }
        } );
    }

    @Override
    public Collection<Characteristic> findTermAsCharacteristic( String search ) throws OntologySearchException {
        return convert( new HashSet<>( findTerms( search ) ) );
    }

    @Override
    public Collection<OntologyTerm> findTerms( String search ) throws OntologySearchException {

        /*
         * URI input: just retrieve the term.
         */
        if ( search.startsWith( "http://" ) ) {
            return combineInThreads( ontology -> {
                OntologyTerm found = ontology.getTerm( search );
                if ( found != null ) {
                    return Collections.singleton( found );
                } else {
                    return Collections.emptySet();
                }
            } );
        }

        Collection<OntologyTerm> results = new HashSet<>();

        /*
         * Other queries:
         */
        String query = OntologySearch.stripInvalidCharacters( search );

        if ( StringUtils.isBlank( query ) ) {
            return results;
        }

        results = searchInThreads( ontology -> ontology.findTerm( query ) );

        if ( geneOntologyService.isOntologyLoaded() )
            results.addAll( geneOntologyService.findTerm( search ) );

        return results;
    }

    @Override
    public Collection<CharacteristicValueObject> findTermsInexact( String givenQueryString, Taxon taxon ) throws OntologySearchException, SearchException {

        if ( StringUtils.isBlank( givenQueryString ) )
            return null;

        StopWatch watch = new StopWatch();
        watch.start();

        String queryString = OntologySearch.stripInvalidCharacters( givenQueryString );
        if ( StringUtils.isBlank( queryString ) ) {
            OntologyServiceImpl.log.warn( "The query was not valid (ended up being empty): " + givenQueryString );
            return new HashSet<>();
        }

        if ( OntologyServiceImpl.log.isDebugEnabled() ) {
            OntologyServiceImpl.log
                    .debug( "starting findExactTerm for " + queryString + ". Timing information begins from here" );
        }

        Collection<CharacteristicValueObject> searchResults = new HashSet<>();

        Map<String, CharacteristicValueObject> previouslyUsedInSystem = new HashMap<>();

        StopWatch countOccurencesTimer = StopWatch.createStarted();
        this.countOccurrences( queryString, previouslyUsedInSystem );
        countOccurencesTimer.stop();

        StopWatch searchForGenesTimer = StopWatch.createStarted();
        this.searchForGenes( queryString, taxon, searchResults );
        searchForGenesTimer.stop();

        StopWatch countOccurrencesTimerAfter = StopWatch.createStarted();
        this.countOccurrences( searchResults, previouslyUsedInSystem );
        countOccurrencesTimerAfter.stop();

        // get ontology terms
        Set<CharacteristicValueObject> ontologySearchResults = new HashSet<>();
        ontologySearchResults.addAll( searchInThreads( service -> {
            Collection<OntologyResource> results;
            results = service.findResources( queryString );
            if ( results.isEmpty() )
                return Collections.emptySet();
            return CharacteristicValueObject.characteristic2CharacteristicVO( this.termsToCharacteristics( results ) );
        } ) );

        // get GO terms, if we don't already have a lot of possibilities. (might have to adjust this)
        StopWatch findGoTerms = StopWatch.createStarted();
        if ( geneOntologyService.isOntologyLoaded() ) {
            ontologySearchResults.addAll( CharacteristicValueObject.characteristic2CharacteristicVO(
                    this.termsToCharacteristics( geneOntologyService.findTerm( queryString ) ) ) );
        }
        findGoTerms.stop();

        // replace terms labels by their ontology equivalent
        Map<CharacteristicValueObject, CharacteristicValueObject> ontologySearchResultsMap = ontologySearchResults.stream().collect( Collectors.toMap( identity(), identity() ) );
        searchResults.forEach( ( k ) -> {
            CharacteristicValueObject x = ontologySearchResultsMap.get( k );
            if ( x != null ) {
                k.setValue( k.getValue() );
            }
        } );

        searchResults.addAll( ontologySearchResults );

        // Sort the results rather elaborately.
        Collection<CharacteristicValueObject> sortedResults = this
                .sort( previouslyUsedInSystem, searchResults, queryString );

        watch.stop();

        if ( watch.getTime() > 1000 ) {
            OntologyServiceImpl.log
                    .info( "Ontology term query for: " + givenQueryString + ": " + watch.getTime() + " ms "
                            + "count occurrences: " + countOccurencesTimer.getTime() + " ms "
                            + "search for genes: " + searchForGenesTimer.getTime() + " ms "
                            + "count occurrences (after ont): " + countOccurrencesTimerAfter.getTime() + " ms "
                            + "find GO terms: " + findGoTerms.getTime() );
        }

        return sortedResults;

    }

    @Override
    public Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return combineInThreads( os -> os.getParents( terms, direct, includeAdditionalProperties ) );
    }

    @Override
    public Set<OntologyTerm> getChildren( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties ) {
        return combineInThreads( os -> os.getChildren( terms, direct, includeAdditionalProperties ) );
    }

    @Override
    public Collection<OntologyTerm> getCategoryTerms() {
        if ( experimentalFactorOntologyService.isOntologyLoaded() ) {
            return categoryTerms.stream()
                    .map( OntologyTerm::getUri )
                    .map( experimentalFactorOntologyService::getTerm )
                    .map( Objects::requireNonNull )
                    .collect( Collectors.toSet() );
        } else {
            OntologyServiceImpl.log.warn( "Ontology needed is not loaded? Using light-weight placeholder for categories." );
            return categoryTerms;
        }
    }

    @Override
    public CellLineOntologyService getCellLineOntologyService() {
        return cellLineOntologyService;
    }

    @Override
    public CellTypeOntologyService getCellTypeOntologyService() {
        return cellTypeOntologyService;
    }

    @Override
    public ChebiOntologyService getChebiOntologyService() {
        return chebiOntologyService;
    }

    @Override
    public GemmaOntologyService getGemmaOntologyService() {
        return gemmaOntologyService;
    }

    @Override
    public HumanDevelopmentOntologyService getHumanDevelopmentOntologyService() {
        return humanDevelopmentOntologyService;
    }

    @Override
    public MouseDevelopmentOntologyService getMouseDevelopmentOntologyService() {
        return mouseDevelopmentOntologyService;
    }

    @Override
    public DiseaseOntologyService getDiseaseOntologyService() {
        return diseaseOntologyService;
    }

    @Override
    public ExperimentalFactorOntologyService getExperimentalFactorOntologyService() {
        return experimentalFactorOntologyService;
    }

    @Override
    public HumanPhenotypeOntologyService getHumanPhenotypeOntologyService() {
        return humanPhenotypeOntologyService;
    }

    @Override
    public MammalianPhenotypeOntologyService getMammalianPhenotypeOntologyService() {
        return mammalianPhenotypeOntologyService;
    }

    @Override
    public ObiService getObiService() {
        return obiService;
    }

    @Override
    public UberonOntologyService getUberonService() {
        return this.uberonOntologyService;
    }

    @Override
    public OntologyResource getResource( String uri ) {
        return findFirst( ontology -> ontology.getResource( uri ) );
    }

    @Override
    public SequenceOntologyService getSequenceOntologyService() {
        return this.sequenceOntologyService;
    }

    @Override
    public OntologyTerm getTerm( String uri ) {
        return findFirst( ontology -> ontology.getTerm( uri ) );
    }

    /**
     * @return true if the Uri is an ObsoleteClass. This will only work if the ontology in question is loaded.
     */
    @Override
    public boolean isObsolete( String uri ) {
        if ( uri == null )
            return false;
        OntologyTerm t = this.getTerm( uri );
        return t != null && t.isObsolete();
    }

    @Override
    public void reindexAllOntologies() {
        for ( ubic.basecode.ontology.providers.OntologyService serv : this.ontologyServices ) {
            if ( serv.isOntologyLoaded() ) {
                OntologyServiceImpl.log.info( "Reindexing: " + serv );
                try {
                    serv.index( true );
                } catch ( Exception e ) {
                    OntologyServiceImpl.log.error( "Failed to index " + serv + ": " + e.getMessage(), e );
                }
            } else {
                if ( serv.isEnabled() )
                    OntologyServiceImpl.log
                            .info( "Not available for reindexing (not enabled or finished initialization): " + serv );
            }
        }
    }

    @Override
    public void reinitializeAndReindexAllOntologies() {
        for ( ubic.basecode.ontology.providers.OntologyService serv : this.ontologyServices ) {
            serv.startInitializationThread( true, true );
        }
    }

    @Override
    public void removeBioMaterialStatement( Long characterId, BioMaterial bm ) {
        Characteristic vc = characteristicService.load( characterId );
        if ( vc == null )
            throw new IllegalArgumentException( "No characteristic with id=" + characterId + " was foundF" );
        bm.getCharacteristics().remove( vc );
        characteristicService.remove( characterId );
    }

    @Override
    public void saveBioMaterialStatement( Characteristic vc, BioMaterial bm ) {

        OntologyServiceImpl.log.debug( "Vocab Characteristic: " + vc );

        vc.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        Set<Characteristic> chars = new HashSet<>();
        chars.add( vc );

        Set<Characteristic> current = bm.getCharacteristics();
        if ( current == null )
            current = new HashSet<>( chars );
        else
            current.addAll( chars );

        for ( Characteristic characteristic : chars ) {
            OntologyServiceImpl.log.info( "Adding characteristic to " + bm + " : " + characteristic );
        }

        bm.setCharacteristics( current );
        bioMaterialService.update( bm );

    }

    @Override
    public void addExpressionExperimentStatement( Characteristic vc, ExpressionExperiment ee ) {
        if ( vc == null ) {
            throw new IllegalArgumentException( "Null characteristic" );
        }
        if ( StringUtils.isBlank( vc.getCategory() ) ) {
            throw new IllegalArgumentException( "Must provide a category" );
        }

        if ( StringUtils.isBlank( vc.getValue() ) ) {
            throw new IllegalArgumentException( "Must provide a value" );
        }

        if ( vc.getEvidenceCode() == null ) {
            vc.setEvidenceCode( GOEvidenceCode.IC ); // assume: manually added characteristic
        }

        if ( StringUtils.isNotBlank( vc.getValueUri() ) && this.isObsolete( vc.getValueUri() ) ) {
            throw new IllegalArgumentException( vc + " is an obsolete term! Not saving." );
        }

        if ( ee == null )
            throw new IllegalArgumentException( "Experiment cannot be null" );

        OntologyServiceImpl.log
                .info( "Adding characteristic '" + vc.getValue() + "' to " + ee.getShortName() + " (ID=" + ee.getId()
                        + ") : " + vc );

        ee.getCharacteristics().add( vc );
    }

    /**
     * Convert raw ontology resources into Characteristics.
     */
    @Override
    public Collection<Characteristic> termsToCharacteristics( final Collection<? extends OntologyResource> terms ) {

        Collection<Characteristic> results = new HashSet<>();

        if ( ( terms == null ) || ( terms.isEmpty() ) )
            return results;

        for ( OntologyResource term : terms ) {

            if ( term == null )
                continue;

            Characteristic vc = this.termToCharacteristic( term );
            if ( vc == null )
                continue;
            results.add( vc );

        }
        OntologyServiceImpl.log.debug( "returning " + results.size() + " terms after filter" );

        return results;
    }

    @Override
    public boolean isInitializing() {
        return ontologyServices.stream().allMatch( o -> o.isInitializationThreadAlive() );
    }

    @Override
    public Map<String, CharacteristicValueObject> findObsoleteTermUsage() {
        Map<String, CharacteristicValueObject> vos = new HashMap<>();

        int start = 0;
        int step = 5000;

        int obsoleteCnt = 0;
        int prevObsoleteCnt = 0;
        int checked = 0;
        while ( true ) {

            Collection<Characteristic> chars = characteristicService.browse( start, step );
            start += step;

            if ( chars == null || chars.isEmpty() ) {
                break;
            }

            OntologyServiceImpl.log.debug( "Found " + chars.size()
                    + " characteristics in the current ID range, checking for obsoletes." );

            CharacteristicValueObject lastObsolete = null;
            for ( Characteristic ch : chars ) {
                if ( StringUtils.isBlank( ch.getValueUri() ) ) {
                    continue;
                }

                checked++;

                if ( this.isObsolete( ch.getValueUri() ) ) {
                    String key = this.foundValueKey( ch );
                    if ( !vos.containsKey( key ) ) {
                        vos.put( key, new CharacteristicValueObject( ch ) );
                    }
                    vos.get( key ).incrementOccurrenceCount();
                    if ( log.isDebugEnabled() )
                        OntologyServiceImpl.log.debug( "Found obsolete term: " + ch.getValue() + " - " + ch.getValueUri() );
                    lastObsolete = vos.get( key );
                }
            }

            if ( obsoleteCnt > prevObsoleteCnt ) {
                OntologyServiceImpl.log.info( "Found " + vos.size() + " obsolete terms so far, tested " + checked + " characteristics" );
                OntologyServiceImpl.log.info( "Last obsolete term seen: " + lastObsolete.getValue() + " -" + lastObsolete.getValueUri() );
            }

            prevObsoleteCnt = obsoleteCnt;
        }

        OntologyServiceImpl.log.info( "Done, obsolete terms found: " + vos.size() );

        return vos;
    }

    private Characteristic termToCharacteristic( OntologyResource res ) {
        if ( res.isObsolete() ) {
            OntologyServiceImpl.log.warn( "Skipping an obsolete term: " + res.getLabel() + " / " + res.getUri() );
            return null;
        }

        Characteristic vc = Characteristic.Factory.newInstance();
        vc.setValue( res.getLabel() );
        vc.setValueUri( res.getUri() );

        if ( res instanceof OntologyTerm ) {
            OntologyTerm term = ( OntologyTerm ) res;
            vc.setDescription( term.getComment() );
        } else if ( res instanceof OntologyIndividual ) {
            vc.setDescription( "Individual" );
        } else {
            OntologyServiceImpl.log.warn( "This is neither an OntologyTerm or an OntologyIndividual: " + res );
            return null;
        }

        if ( vc.getValue() == null ) {
            OntologyServiceImpl.log
                    .warn( "Skipping a characteristic with no value: " + res.getLabel() + " / " + res.getUri() );
            return null;
        }

        return vc;
    }

    /**
     * Given a collection of ontology terms converts them to a collection of Characteristics
     */
    private Collection<Characteristic> convert( final Collection<OntologyResource> resources ) {

        Collection<Characteristic> converted = new HashSet<>();

        if ( ( resources == null ) || ( resources.isEmpty() ) )
            return converted;

        for ( OntologyResource res : resources ) {
            Characteristic vc = Characteristic.Factory.newInstance();

            // If there is no URI we don't want to send it back (ie useless)
            if ( ( res.getUri() == null ) || StringUtils.isEmpty( res.getUri() ) )
                continue;

            vc.setValue( res.getLabel() );
            vc.setValueUri( res.getUri() );

            if ( res instanceof OntologyTerm ) {
                OntologyTerm term = ( OntologyTerm ) res;
                vc.setDescription( term.getComment() );
            }
            if ( res instanceof OntologyIndividual ) {
                vc.setDescription( "Individual" );
            }

            converted.add( vc );
        }

        return converted;
    }

    private void countOccurrences( String queryString, Map<String, CharacteristicValueObject> previouslyUsedInSystem ) {
        StopWatch watch = new StopWatch();
        watch.start();

        Map<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> foundChars = characteristicService.countCharacteristicValueLikeByValueUriOrValue( queryString );

        /*
         * Want to flag in the web interface that these are already used by Gemma (also ignore capitalization; category
         * is always ignored; remove duplicates.)
         */
        for ( Map.Entry<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> characteristicByValueCount : foundChars.entrySet() ) {
            // count up number of usages; see bug 3897
            String key = characteristicByValueCount.getKey();
            if ( !previouslyUsedInSystem.containsKey( key ) ) {
                if ( OntologyServiceImpl.log.isDebugEnabled() )
                    OntologyServiceImpl.log.debug( "saw " + key + " (" + key + ") for " + characteristicByValueCount );
                previouslyUsedInSystem.put( key, characteristicByValueCountToValueObject( characteristicByValueCount.getValue() ) );
            }
        }

        String message = String.format( "Found %d matching characteristics used in the database in %d ms  Filtered from initial set of %d",
                previouslyUsedInSystem.size(), watch.getTime(), foundChars.size() );
        if ( watch.getTime() > 300 ) {
            OntologyServiceImpl.log.warn( message );
        } else {
            OntologyServiceImpl.log.debug( message );
        }
    }

    private CharacteristicValueObject characteristicByValueCountToValueObject( CharacteristicDao.CharacteristicByValueUriOrValueCount characteristic ) {
        CharacteristicValueObject vo = new CharacteristicValueObject( -1L, characteristic.getValue(), characteristic.getValueUri() );
        vo.setCategory( null );
        vo.setCategoryUri( null ); // to avoid us counting separately by category.
        vo.setAlreadyPresentInDatabase( true );
        vo.setNumTimesUsed( characteristic.getCount().intValue() );
        return vo;
    }

    /**
     * given a collection of characteristics add them to the correct List
     */
    private Collection<CharacteristicValueObject> findCharacteristicsFromOntology( String searchQuery,
            boolean useNeuroCartaOntology,
            Map<String, CharacteristicValueObject> characteristicFromDatabaseWithValueUri ) throws OntologySearchException {

        // in neurocarta we don't need to search all Ontologies
        List<ubic.basecode.ontology.providers.OntologyService> ontologyServicesToUse;
        if ( useNeuroCartaOntology ) {
            ontologyServicesToUse = Arrays.asList(
//                    nifstdOntologyService,
//                    fmaOntologyService,
                    obiService );
        } else {
            ontologyServicesToUse = this.ontologyServices;
        }

        return searchInThreads( ontologyService -> {
            Collection<OntologyTerm> ontologyTerms = ontologyService.findTerm( searchQuery );
            Collection<CharacteristicValueObject> characteristicsFromOntology = new HashSet<>();
            for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                // if the ontology term wasnt already found in the database
                if ( characteristicFromDatabaseWithValueUri.get( ontologyTerm.getUri() ) == null ) {
                    CharacteristicValueObject phenotype = new CharacteristicValueObject( -1L,
                            ontologyTerm.getLabel().toLowerCase(), ontologyTerm.getUri() );
                    characteristicsFromOntology.add( phenotype );
                }
            }
            return characteristicsFromOntology;
        }, ontologyServicesToUse );
    }

    private String foundValueKey( Characteristic c ) {
        if ( StringUtils.isNotBlank( c.getValueUri() ) ) {
            return c.getValueUri().toLowerCase();
        }
        return c.getValue().toLowerCase();
    }

    private String foundValueKey( CharacteristicValueObject c ) {
        if ( c.getValueUri() != null && StringUtils.isNotBlank( c.getValueUri() ) ) {
            return c.getValueUri().toLowerCase();
        }
        return c.getValue().toLowerCase();
    }

    /**
     * Allow us to store gene information as a characteristic associated with our entities. This doesn't work so well
     * for non-ncbi genes.
     */
    private Characteristic gene2Characteristic( GeneValueObject g ) {
        Characteristic vc = Characteristic.Factory.newInstance();
        vc.setCategory( "gene" );
        vc.setCategoryUri( "http://purl.org/commons/hcls/gene" );
        vc.setValue( g.getOfficialSymbol() + " [" + g.getTaxonCommonName() + "]" + " " + g.getOfficialName() );
        vc.setDescription( g.toString() );
        if ( g.getNcbiId() != null ) {
            vc.setValueUri( "http://purl.org/commons/record/ncbi_gene/" + g.getNcbiId() );
        }
        return vc;
    }

    private void initializeCategoryTerms() throws IOException {
        Set<OntologyTerm> categoryTerms = new HashSet<>();
        Resource resource = new ClassPathResource( "/ubic/gemma/core/ontology/EFO.factor.categories.txt" );
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( resource.getInputStream() ) ) ) {
            String line;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.startsWith( "#" ) || StringUtils.isEmpty( line ) )
                    continue;
                String[] f = StringUtils.split( line, '\t' );
                if ( f.length < 2 ) {
                    continue;
                }
                categoryTerms.add( new OntologyTermSimple( f[0], f[1] ) );
            }
            this.categoryTerms = Collections.unmodifiableSet( categoryTerms );
        }
    }

    /**
     * given a collection of characteristics add them to the correct List
     */
    private void putCharacteristicsIntoSpecificList( String searchQuery,
            Collection<CharacteristicValueObject> characteristics,
            Collection<CharacteristicValueObject> characteristicsWithExactMatch,
            Collection<CharacteristicValueObject> characteristicsStartWithQuery,
            Collection<CharacteristicValueObject> characteristicsSubstring,
            Collection<CharacteristicValueObject> characteristicsNoRuleFound ) {

        for ( CharacteristicValueObject cha : characteristics ) {
            // Case 1, exact match
            if ( cha.getValue().equalsIgnoreCase( searchQuery ) ) {
                characteristicsWithExactMatch.add( cha );
            }
            // Case 2, starts with a substring of the word
            else if ( cha.getValue().toLowerCase().startsWith( searchQuery.toLowerCase() ) ) {
                characteristicsStartWithQuery.add( cha );
            }
            // Case 3, contains a substring of the word
            else if ( cha.getValue().toLowerCase().contains( searchQuery.toLowerCase() ) ) {
                characteristicsSubstring.add( cha );
            } else {
                characteristicsNoRuleFound.add( cha );
            }
        }
    }

    /**
     * Look for genes, but only for certain category Uris (genotype, etc.)
     *
     * @param taxon         okay if null, but then all matches returned.
     * @param searchResults added to this
     */
    private void searchForGenes( String queryString, Taxon taxon,
            Collection<CharacteristicValueObject> searchResults ) throws SearchException {

        SearchSettings ss = SearchSettings.builder()
                .query( queryString )
                .taxon( taxon )
                .resultType( Gene.class )
                .build();
        SearchService.SearchResultMap geneResults = this.searchService.search( ss, false, false );

        for ( SearchResult<Gene> sr : geneResults.get( Gene.class ) ) {
            if ( !sr.getResultType().isAssignableFrom( Gene.class ) ) {
                throw new IllegalStateException( "Expected a gene search result, got a " + sr.getResultType() );
            }

            GeneValueObject g = this.geneService.loadValueObjectById( sr.getResultId() );

            if ( g == null ) {
                log.warn(
                        "There is no gene with ID=" + sr.getResultId() + " (in response to search for "
                                + queryString + ") - index out of date?" );
                continue;
            }

            if ( OntologyServiceImpl.log.isDebugEnabled() )
                OntologyServiceImpl.log.debug( "Search for " + queryString + " returned: " + g );
            searchResults.add( new CharacteristicValueObject( this.gene2Characteristic( g ) ) );
        }
    }

    /**
     * @param alreadyUsedResults items already in the system; remove singleton free-text terms.
     * @param otherResults       other results
     * @param searchTerm         the query
     */
    private Collection<CharacteristicValueObject> sort( Map<String, CharacteristicValueObject> alreadyUsedResults,
            Collection<CharacteristicValueObject> otherResults, String searchTerm ) {
        /*
         * Organize the list into 3 parts. Want to get the exact match showing up on top
         */

        List<CharacteristicValueObject> sortedResultsExact = new ArrayList<>();
        List<CharacteristicValueObject> sortedResultsStartsWith = new ArrayList<>();
        List<CharacteristicValueObject> sortedResultsBottom = new ArrayList<>();
        Set<String> foundValues = new HashSet<>();
        for ( String key : alreadyUsedResults.keySet() ) {
            CharacteristicValueObject c = alreadyUsedResults.get( key );

            if ( foundValues.contains( key ) )
                continue;
            foundValues.add( key );

            // don't show singletons of free-text terms.
            if ( c.getValueUri() == null && c.getNumTimesUsed() < 2 ) {
                continue;
            }

            //Skip obsolete terms
            if ( this.isObsolete( c.getValueUri() ) ) {
                OntologyServiceImpl.log.warn( "Skipping an obsolete term: " + c.getValue() + " / " + c.getValueUri() );
                continue;
            }

            this.addToAppropriateList( searchTerm, sortedResultsExact, sortedResultsStartsWith, sortedResultsBottom,
                    c );
        }

        for ( CharacteristicValueObject c : otherResults ) {
            assert c.getValueUri() != null;
            String key = this.foundValueKey( c );
            if ( foundValues.contains( key ) )
                continue;

            foundValues.add( key );

            this.addToAppropriateList( searchTerm, sortedResultsExact, sortedResultsStartsWith, sortedResultsBottom,
                    c );
        }

        sortedResultsExact.sort( CHARACTERISTIC_VO_COMPARATOR );
        sortedResultsStartsWith.sort( CHARACTERISTIC_VO_COMPARATOR );
        sortedResultsBottom.sort( CHARACTERISTIC_VO_COMPARATOR );

        List<CharacteristicValueObject> sortedTerms = new ArrayList<>( foundValues.size() );
        sortedTerms.addAll( sortedResultsExact );
        sortedTerms.addAll( sortedResultsStartsWith );
        sortedTerms.addAll( sortedResultsBottom );

        return sortedTerms;
    }

    private void addToAppropriateList( String searchTerm, List<CharacteristicValueObject> sortedResultsExact,
            List<CharacteristicValueObject> sortedResultsStartsWith,
            List<CharacteristicValueObject> sortedResultsBottom, CharacteristicValueObject c ) {
        if ( c.getValue().equalsIgnoreCase( searchTerm ) ) {
            sortedResultsExact.add( c );
        } else if ( c.getValue().toLowerCase().startsWith( searchTerm.toLowerCase() ) || c.getValueUri() != null ) {
            sortedResultsStartsWith.add( c );
        } else {
            sortedResultsBottom.add( c );
        }
    }

    /**
     * Find the first non-null result among loaded ontology services.
     */
    @Nullable
    private <T> T findFirst( Function<ubic.basecode.ontology.providers.OntologyService, T> function ) {
        BlockingQueue<Future<T>> futures = new ArrayBlockingQueue<>( ontologyServices.size() );
        for ( ubic.basecode.ontology.providers.OntologyService service : ontologyServices ) {
            if ( service.isOntologyLoaded() ) {
                futures.add( taskExecutor.submit( () -> function.apply( service ) ) );
            }
        }
        // find the first completing future
        CompletionService<T> completionService = new ExecutorCompletionService<>( taskExecutor, futures );
        try {
            while ( !futures.isEmpty() ) {
                T result = completionService.take().get();
                if ( result != null ) {
                    return result;
                }
            }
        } catch ( InterruptedException e ) {
            return null;
        } catch ( ExecutionException e ) {
            if ( e.getCause() instanceof RuntimeException ) {
                throw ( RuntimeException ) e.getCause();
            } else {
                throw new RuntimeException( e.getCause() );
            }
        } finally {
            // cancel all the remaining futures
            for ( Future<T> future : futures ) {
                future.cancel( true );
            }
        }
        return null;
    }

    /**
     * Apply a given function to all the loaded ontology service and combine the results in a set.
     * <p>
     * The functions are evaluated using Gemma's short-lived task executor.
     */
    private <T> Set<T> combineInThreads( Function<ubic.basecode.ontology.providers.OntologyService, Collection<T>> work, List<ubic.basecode.ontology.providers.OntologyService> ontologyServices ) {
        List<Future<Collection<T>>> futures = new ArrayList<>( ontologyServices.size() );
        for ( ubic.basecode.ontology.providers.OntologyService os : ontologyServices ) {
            if ( os.isOntologyLoaded() ) {
                futures.add( taskExecutor.submit( () -> {
                    StopWatch timer = StopWatch.createStarted();
                    try {
                        return work.apply( os );
                    } finally {
                        if ( timer.getTime() > 200 ) {
                            log.warn( String.format( "Gathering results from %s took %d ms.", os, timer.getTime() ) );
                        }
                    }
                } ) );
            }
        }
        Set<T> children = new HashSet<>();
        for ( Future<Collection<T>> f : futures ) {
            try {
                children.addAll( f.get() );
            } catch ( InterruptedException e ) {
                log.warn( "Current thread was interrupted while waiting, will only return results collected so far.", e );
                Thread.currentThread().interrupt();
                return children;
            } catch ( ExecutionException e ) {
                if ( e.getCause() instanceof RuntimeException ) {
                    throw ( RuntimeException ) e.getCause();
                } else {
                    throw new RuntimeException( e.getCause() );
                }
            }
        }
        return children;
    }

    private <T> Set<T> combineInThreads( Function<ubic.basecode.ontology.providers.OntologyService, Collection<T>> work ) {
        return combineInThreads( work, ontologyServices );
    }

    @FunctionalInterface
    private interface SearchFunction<T> {
        Collection<T> apply( ubic.basecode.ontology.providers.OntologyService service ) throws OntologySearchException;
    }

    private <T> Set<T> searchInThreads( SearchFunction<T> function, List<ubic.basecode.ontology.providers.OntologyService> ontologyServices ) throws OntologySearchException {
        try {
            return combineInThreads( os -> {
                StopWatch timer = StopWatch.createStarted();
                try {
                    return function.apply( os );
                } catch ( OntologySearchException e ) {
                    throw new OntologySearchExceptionWrapper( e );
                } finally {
                    timer.stop();
                    if ( timer.getTime() > 50 ) {
                        log.warn( String.format( "Gathering search results from %s took %d ms.", os, timer.getTime() ) );
                    }
                }
            }, ontologyServices );
        } catch ( OntologySearchExceptionWrapper e ) {
            throw e.getCause();
        }
    }

    /**
     * Similar to {@link #combineInThreads(Function)}, but also handles {@link OntologySearchException}.
     */
    private <T> Set<T> searchInThreads( SearchFunction<T> function ) throws OntologySearchException {
        return searchInThreads( function, ontologyServices );
    }

    private static class OntologySearchExceptionWrapper extends RuntimeException {

        private final OntologySearchException cause;

        public OntologySearchExceptionWrapper( OntologySearchException e ) {
            super( e );
            this.cause = e;
        }

        @Override
        public synchronized OntologySearchException getCause() {
            return cause;
        }
    }

    /**
     * Sorts Characteristics in our preferred ordering
     */
    static class CharacteristicComparator implements Comparator<CharacteristicValueObject> {

        @Override
        public int compare( CharacteristicValueObject o1, CharacteristicValueObject o2 ) {
            // sort by whether used or not, and then by URI; terms without URIs are listed later; break ties by length
            if ( o1.getValueUri() != null ) {
                if ( o2.getValueUri() != null ) {

                    // both have uri, break tie.
                    if ( o1.isAlreadyPresentInDatabase() ) {
                        if ( o2.isAlreadyPresentInDatabase() ) {

                            // both are used, break tie by who is used most.
                            if ( o1.getNumTimesUsed() > o2.getNumTimesUsed() ) {
                                return -1;
                            } else if ( o2.getNumTimesUsed() > o1.getNumTimesUsed() ) {
                                return 1;
                            }

                            // both are used same number of times, compare by length (shorter better, typically...)
                            if ( o1.getValue().length() < o2.getValue().length() ) {
                                return -1;
                            } else if ( o1.getValue().length() > o2.getValue().length() ) {
                                return 1;
                            }

                            // equal length, compare by lexig. value.
                            return o1.getValue().toLowerCase().compareTo( o2.getValue().toLowerCase() );
                        }

                        // o1 is used, o2 is not; o1 should be first.
                        return -1;

                    } else if ( o2.isAlreadyPresentInDatabase() ) {
                        // o2 is used and o1 is not; o2 should be first.
                        return 1;
                    }

                }

                // o1 has uri, o2 does not.
                return -1;

            } else if ( o2.getValueUri() != null ) {
                // we know o1 does not have a uri, o2 goes first.
                return 1;
            }

            // neither has URI. By definition these are in the database, so we just rank by length/text
            if ( o1.getValue().length() < o2.getValue().length() ) {
                return -1;
            } else if ( o1.getValue().length() > o2.getValue().length() ) {
                return 1;
            }

            // equal length, compare by lexig. value.
            return o1.getValue().toLowerCase().compareTo( o2.getValue().toLowerCase() );
        }

    }

}