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

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.ParseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.*;
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.ontology.providers.OntologyServiceFactory;
import ubic.gemma.core.search.*;
import ubic.gemma.core.search.lucene.LuceneParseSearchException;
import ubic.gemma.core.search.lucene.LuceneQueryUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.common.description.CharacteristicService;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Has a static method for finding out which ontologies are loaded into the system and a general purpose find method
 * that delegates to the many ontology services. NOTE: Logging messages from this service are important for tracking
 * changes to annotations.
 *
 * @author pavlidis
 */
@Service
public class OntologyServiceImpl implements OntologyService, InitializingBean {

    private static final Log log = LogFactory.getLog( OntologyServiceImpl.class.getName() );
    private static final String
            SEARCH_CACHE_NAME = "OntologyService.search",
            PARENTS_CACHE_NAME = "OntologyService.parents",
            CHILDREN_CACHE_NAME = "OntologyService.children";

    /**
     * The amount of time to wait for resolving the next available future.
     */
    private static final long checkFrequencyMillis = 1000;

    /**
     * If the future does not resolve within the timeout, increase it by the given amount.
     */
    private static final double exponentialBackoff = 1.5;

    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private GeneOntologyService geneOntologyService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

    @Autowired
    private ExperimentalFactorOntologyService experimentalFactorOntologyService;
    @Autowired
    private ObiService obiService;

    @Autowired(required = false)
    private List<OntologyServiceFactory<?>> ontologyServiceFactories;

    @Autowired
    private List<ubic.basecode.ontology.providers.OntologyService> ontologyServices;

    @Autowired
    @Qualifier("ontologyTaskExecutor")
    private TaskExecutor ontologyTaskExecutor;

    @Autowired
    private CacheManager cacheManager;

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    private OntologyCache ontologyCache;
    private Set<OntologyTermSimple> categoryTerms = null;

    private Set<OntologyPropertySimple> relationTerms = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        ontologyCache = new OntologyCache( cacheManager.getCache( SEARCH_CACHE_NAME ), cacheManager.getCache( PARENTS_CACHE_NAME ), cacheManager.getCache( CHILDREN_CACHE_NAME ) );
        if ( ontologyServiceFactories != null && autoLoadOntologies ) {
            List<ubic.basecode.ontology.providers.OntologyService> enabledOntologyServices = ontologyServiceFactories.stream()
                    .map( factory -> {
                        try {
                            return factory.getObject();
                        } catch ( Exception e ) {
                            throw new RuntimeException( e );
                        }
                    } )
                    .filter( ubic.basecode.ontology.providers.OntologyService::isEnabled )
                    .collect( Collectors.toList() );
            if ( !enabledOntologyServices.isEmpty() ) {
                log.info( "The following ontologies are enabled:\n\t" + enabledOntologyServices.stream()
                        .map( ubic.basecode.ontology.providers.OntologyService::toString )
                        .collect( Collectors.joining( "\n\t" ) ) );
            } else {
                log.warn( "No ontologies are enabled, consider enabling them by setting 'load.{name}Ontology' options in Gemma.properties." );
            }
        }
        // remove GeneOntologyService, it was originally not included in the list before bean injection was used
        ontologyServices.remove( geneOntologyService );
        initializeCategoryTerms();
        initializeRelationTerms();
    }

    private void countOccurrences( Map<String, CharacteristicValueObject> results ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Set<String> uris = results.values().stream()
                .map( CharacteristicValueObject::getValueUri )
                .filter( Objects::nonNull ) // ignore free-text terms
                .map( String::toLowerCase ) // we can merge URIs with the same case
                .collect( Collectors.toSet() );

        Map<String, Long> existingCharacteristicsUsingTheseTerms = characteristicService.countCharacteristicsByValueUri( uris );

        for ( Map.Entry<String, CharacteristicValueObject> entry : results.entrySet() ) {
            String k = entry.getKey();
            CharacteristicValueObject v = entry.getValue();
            if ( v.getValueUri() == null ) {
                // free text, frequency is 1
                v.setNumTimesUsed( 1 );
            } else {
                v.setNumTimesUsed( existingCharacteristicsUsingTheseTerms.getOrDefault( k, 0L ).intValue() );
            }
        }

        if ( OntologyServiceImpl.log.isDebugEnabled() || ( watch.getTime() > 200
                && results.size() > 0 ) )
            OntologyServiceImpl.log.warn( String.format(
                    "Found %d matching characteristics used in the database in %d ms  Filtered from initial set of %d",
                    results.size(), watch.getTime(), existingCharacteristicsUsingTheseTerms.size() ) );
    }

    /**
     * Using the ontology and values in the database, for a search searchQuery given by the client give an ordered list
     * of possible choices
     */
    @Override
    public Collection<CharacteristicValueObject> findExperimentsCharacteristicTags( String searchQuery,
            boolean useNeuroCartaOntology, long timeout, TimeUnit timeUnit ) throws SearchException {

        if ( searchQuery.trim().length() < 3 ) {
            return new HashSet<>();
        }

        Map<String, CharacteristicValueObject> characteristicFromDatabaseWithValueUri = new HashMap<>();
        Collection<CharacteristicValueObject> characteristicFromDatabaseFreeText = new HashSet<>();

        // this will do like 'search%'
        String wildcardQuery = LuceneQueryUtils.prepareDatabaseQuery( searchQuery, true );
        if ( wildcardQuery != null ) {
            Collection<CharacteristicValueObject> characteristicsFromDatabase = CharacteristicValueObject
                    .characteristic2CharacteristicVO( this.characteristicService.findByValueLike( wildcardQuery ) );
            for ( CharacteristicValueObject characteristicInDatabase : characteristicsFromDatabase ) {
                // flag to let know that it was found in the database
                characteristicInDatabase.setAlreadyPresentInDatabase( true );
                if ( characteristicInDatabase.getValueUri() != null && !characteristicInDatabase.getValueUri().isEmpty() ) {
                    characteristicFromDatabaseWithValueUri
                            .put( characteristicInDatabase.getValueUri(), characteristicInDatabase );
                } else {
                    // free txt, no value uri
                    characteristicFromDatabaseFreeText.add( characteristicInDatabase );
                }
            }
        }

        // search the ontology for the given searchTerm, but if already found in the database dont add it again
        Collection<CharacteristicValueObject> characteristicsFromOntology = this
                .findCharacteristicsFromOntology( searchQuery, useNeuroCartaOntology,
                        characteristicFromDatabaseWithValueUri, timeUnit.toMillis( timeout ) );

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
    public Collection<OntologyTerm> findTerms( String search, long timeout, TimeUnit timeUnit ) throws SearchException {
        Collection<OntologyTerm> results = new HashSet<>();

        if ( StringUtils.isBlank( search ) ) {
            return results;
        }

        /*
         * URI input: just retrieve the term.
         */
        if ( search.startsWith( "http://" ) ) {
            try {
                return Collections.singleton( findFirst( ontology -> ontology.getTerm( search ), "terms matching " + search, timeUnit.toMillis( timeout ) ) );
            } catch ( TimeoutException e ) {
                throw new SearchTimeoutException( "Ontology search timed out for querying terms matching " + search, e );
            }
        }

        results = searchInThreads( ontology -> ontologyCache.findTerm( ontology, search ), search, 5000 );

        if ( geneOntologyService.isOntologyLoaded() ) {
            try {
                results.addAll( ontologyCache.findTerm( geneOntologyService, search ) );
            } catch ( OntologySearchException e ) {
                throw convertBaseCodeOntologySearchExceptionToSearchException( e, search );
            }
        }

        return results.stream()
                .sorted( Comparator.comparing( OntologyTerm::getScore, Comparator.nullsLast( Comparator.reverseOrder() ) ) )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    @Override
    public Collection<CharacteristicValueObject> findTermsInexact( String queryString, @Nullable Taxon taxon, long timeout, TimeUnit timeUnit ) throws SearchException {
        if ( StringUtils.isBlank( queryString ) )
            return Collections.emptySet();

        StopWatch watch = new StopWatch();
        watch.start();

        if ( StringUtils.isBlank( queryString ) ) {
            OntologyServiceImpl.log.warn( "The query was not valid (ended up being empty): " + queryString );
            return Collections.emptySet();
        }

        if ( OntologyServiceImpl.log.isDebugEnabled() ) {
            OntologyServiceImpl.log
                    .debug( "starting findExactTerm for " + queryString + ". Timing information begins from here" );
        }

        Map<String, CharacteristicValueObject> results = new HashMap<>();

        StopWatch searchForCharacteristics = StopWatch.createStarted();
        this.searchForCharacteristics( queryString, results );
        searchForCharacteristics.stop();

        StopWatch searchForGenesTimer = StopWatch.createStarted();
        this.searchForGenes( queryString, taxon, results );
        searchForGenesTimer.stop();

        // get ontology terms
        Set<CharacteristicValueObject> ontologySearchResults = new HashSet<>();
        ontologySearchResults.addAll( searchInThreads( service -> {
            Collection<OntologyTerm> results2 = ontologyCache.findTerm( service, queryString );
            if ( results2.isEmpty() )
                return Collections.emptySet();
            return CharacteristicValueObject.characteristic2CharacteristicVO( this.termsToCharacteristics( results2 ) );
        }, queryString, timeUnit.toMillis( timeout ) ) );

        // get GO terms, if we don't already have a lot of possibilities. (might have to adjust this)
        StopWatch findGoTerms = StopWatch.createStarted();
        if ( geneOntologyService.isOntologyLoaded() ) {
            try {
                ontologySearchResults.addAll( CharacteristicValueObject.characteristic2CharacteristicVO(
                        this.termsToCharacteristics( ontologyCache.findTerm( geneOntologyService, queryString ) ) ) );
            } catch ( OntologySearchException e ) {
                throw convertBaseCodeOntologySearchExceptionToSearchException( e, queryString );
            }
        }
        findGoTerms.stop();

        // replace terms labels by their ontology equivalent
        ontologySearchResults.forEach( or -> {
            String k = or.getValueUri().toLowerCase();
            if ( results.containsKey( k ) ) {
                results.get( k ).setValue( or.getValue() );
            }
        } );

        // since those are ontology terms, the normalized value is always the lowercase URI
        for ( CharacteristicValueObject cvo : ontologySearchResults ) {
            String key = cvo.getValueUri().toLowerCase();
            if ( results.containsKey( key ) ) {
                // only update the label, prefer the value in the database already
                results.get( key ).setValue( cvo.getValue() );
            } else {
                results.put( key, cvo );
            }
        }

        StopWatch countOccurrencesTimerAfter = StopWatch.createStarted();
        this.countOccurrences( results );
        countOccurrencesTimerAfter.stop();

        // Sort the results rather elaborately.
        LinkedHashSet<CharacteristicValueObject> sortedResults = results.values().stream()
                .sorted( getCharacteristicComparator( queryString ) )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );

        watch.stop();

        if ( watch.getTime() > 1000 ) {
            OntologyServiceImpl.log
                    .info( "Ontology term query for: " + queryString + ": " + watch.getTime() + " ms "
                            + "count occurrences: " + searchForCharacteristics.getTime() + " ms "
                            + "search for genes: " + searchForGenesTimer.getTime() + " ms "
                            + "count occurrences (after ont): " + countOccurrencesTimerAfter.getTime() + " ms "
                            + "find GO terms: " + findGoTerms.getTime() );
        }

        return sortedResults;
    }

    @Override
    public Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        return getParentsOrChildren( terms, direct, includeAdditionalProperties, true, timeUnit.toMillis( timeout ) );
    }

    @Override
    public Set<OntologyTerm> getChildren( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        return getParentsOrChildren( terms, direct, includeAdditionalProperties, false, timeUnit.toMillis( timeout ) );
    }

    private Set<OntologyTerm> getParentsOrChildren( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, boolean parents, long timeoutMs ) throws TimeoutException {
        if ( terms.isEmpty() ) {
            return Collections.emptySet();
        }
        StopWatch totalTimer = StopWatch.createStarted();
        Set<OntologyTerm> toQuery = new HashSet<>( terms );
        List<OntologyTerm> results = new ArrayList<>();
        while ( !toQuery.isEmpty() ) {
            List<OntologyTerm> newResults = combineInThreads( os -> {
                StopWatch timer = StopWatch.createStarted();
                try {
                    return parents ? ontologyCache.getParents( os, toQuery, direct, includeAdditionalProperties )
                            : ontologyCache.getChildren( os, toQuery, direct, includeAdditionalProperties );
                } finally {
                    if ( timer.getTime() > Math.max( 10L * terms.size(), 1000L ) ) {
                        log.warn( String.format( "Obtaining %s from %s for %s took %d ms",
                                parents ? "parents" : "children",
                                os,
                                terms.size() == 1 ? terms.iterator().next() : terms.size() + " terms",
                                timer.getTime() ) );
                    }
                }
            }, String.format( "%s %s of %d terms", direct ? "direct" : "all", parents ? "parents" : "children", terms.size() ), Math.max( timeoutMs - totalTimer.getTime(), 0 ) );

            if ( results.addAll( newResults ) && !direct ) {
                // there are new results (i.e. a term was inferred from a different ontology), we need to requery them
                // if they were not in the query
                newResults.removeAll( toQuery );
                toQuery.clear();
                toQuery.addAll( newResults );
                log.debug( String.format( "Found %d new %s terms, will requery them.", newResults.size(),
                        parents ? "parents" : "children" ) );
            } else {
                toQuery.clear();
            }
        }

        // when an ontology returns a result without a label, it might be referring to another ontology, so we attempt
        // to retrieve a results with a label as a replacement
        Set<String> resultsWithMissingLabels = results.stream()
                .filter( t -> t.getLabel() == null )
                .map( OntologyResource::getUri )
                .collect( Collectors.toSet() );
        if ( !resultsWithMissingLabels.isEmpty() ) {
            Set<OntologyTerm> replacements = getTerms( resultsWithMissingLabels );
            results.removeAll( replacements );
            results.addAll( replacements );
        }

        // drop terms without labels
        results.removeIf( t -> t.getLabel() == null );
        return new HashSet<>( results );
    }

    @Override
    public Set<OntologyTerm> getCategoryTerms() {
        return categoryTerms.stream()
                .map( term -> {
                    String termUri = term.getUri();
                    if ( termUri == null ) {
                        return term; // a free-text category
                    }
                    if ( experimentalFactorOntologyService.isOntologyLoaded() ) {
                        OntologyTerm efoTerm = experimentalFactorOntologyService.getTerm( termUri );
                        if ( efoTerm != null ) {
                            return efoTerm;
                        }
                    }
                    return term;
                } )
                .collect( Collectors.toSet() );
    }


    @Override
    public Set<OntologyProperty> getRelationTerms() {
        // FIXME: it's not quite like categoryTerms so this map operation is probably not needed at all, the relations don't come from any particular ontology
        return Collections.unmodifiableSet( relationTerms );
    }

    @Override
    public String getDefinition( String uri ) {
        OntologyTerm ot = this.getTerm( uri );
        if ( ot != null ) {
            // FIXME: not clear this will work with all ontologies. UBERON, HP, MP, MONDO does it this way.
            AnnotationProperty annot = ot.getAnnotation( "http://purl.obolibrary.org/obo/IAO_0000115" );
            if ( annot != null ) {
                return annot.getContents();
            }
        }
        return null;
    }

    @Override
    public OntologyTerm getTerm( String uri ) {
        try {
            return findFirst( ontology -> {
                OntologyTerm term = ontology.getTerm( uri );
                if ( term != null && term.getLabel() == null ) {
                    return null;
                }
                return term;
            }, uri, 5000 );
        } catch ( TimeoutException e ) {
            throw new RuntimeException( String.format( "Retrieving a term for %s timed out.", uri ), e );
        }
    }

    @Override
    public Set<OntologyTerm> getTerms( Collection<String> uris, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        Set<String> distinctUris = uris instanceof Set ? ( Set<String> ) uris : new HashSet<>( uris );
        List<OntologyTerm> results = combineInThreads( os -> distinctUris.stream().map( os::getTerm ).filter( Objects::nonNull ).collect( Collectors.toSet() ),
                String.format( "terms for %d URIs", uris.size() ), timeUnit.toMillis( timeout ) );
        results.removeIf( t -> t.getLabel() == null );
        return new HashSet<>( results );
    }

    @Override
    public void reindexAllOntologies() {
        for ( ubic.basecode.ontology.providers.OntologyService serv : this.ontologyServices ) {
            if ( serv.isEnabled() && serv.isSearchEnabled() ) {
                ontologyTaskExecutor.execute( () -> {
                    OntologyServiceImpl.log.info( "Reindexing " + serv + "..." );
                    serv.index( true );
                    ontologyCache.clearSearchCacheByOntology( serv );
                } );
            }
        }
    }

    @Override
    public void reinitializeAndReindexAllOntologies() {
        for ( ubic.basecode.ontology.providers.OntologyService serv : this.ontologyServices ) {
            if ( serv.isOntologyLoaded() ) {
                if ( serv.isEnabled() ) {
                    boolean isSearchEnabled = serv.isSearchEnabled();
                    ontologyTaskExecutor.execute( () -> {
                        OntologyServiceImpl.log.info( "Reinitializing " + serv + "..." );
                        serv.initialize( true, isSearchEnabled );
                        ontologyCache.clearByOntology( serv );
                        if ( isSearchEnabled ) {
                            ontologyCache.clearSearchCacheByOntology( serv );
                        }
                    } );
                }
            }
        }
    }

    /**
     * Convert raw ontology resources into Characteristics.
     */
    private Collection<Characteristic> termsToCharacteristics( Collection<OntologyTerm> terms ) {

        Collection<Characteristic> results = new HashSet<>();

        if ( ( terms == null ) || ( terms.isEmpty() ) )
            return results;

        for ( OntologyTerm term : terms ) {

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


    private Characteristic termToCharacteristic( OntologyTerm res ) {
        if ( res.isObsolete() ) {
            OntologyServiceImpl.log.warn( "Skipping an obsolete term: " + res.getLabel() + " / " + res.getUri() );
            return null;
        }

        Characteristic vc = Characteristic.Factory.newInstance();
        vc.setValue( res.getLabel() );
        vc.setValueUri( res.getUri() );
        vc.setDescription( res.getComment() );

        if ( vc.getValue() == null ) {
            OntologyServiceImpl.log
                    .warn( "Skipping a characteristic with no value: " + res.getLabel() + " / " + res.getUri() );
            return null;
        }

        return vc;
    }

    @Override
    public Map<Characteristic, Long> findObsoleteTermUsage() {
        Map<Characteristic, Long> results = new HashMap<>();

        int prevObsoleteCnt = 0;
        int checked = 0;
        Characteristic lastObsolete = null;
        long total = characteristicService.countAll();

        int step = 5000;
        for ( int start = 0; ; start += step ) {
            Collection<Characteristic> chars = characteristicService.browse( start, step );

            if ( chars == null || chars.isEmpty() ) {
                break;
            }

            for ( Characteristic ch : chars ) {
                String valueUri = ch.getValueUri();
                if ( StringUtils.isBlank( valueUri ) ) {
                    continue;
                }

                checked++;

                OntologyTerm term = this.getTerm( valueUri );
                if ( term != null && term.isObsolete() ) {
                    if ( valueUri.startsWith( "http://purl.org/commons/record/ncbi_gene" ) || valueUri.startsWith( "http://purl.obolibrary.org/obo/GO_" ) ) {
                        // these are false positives, they aren't in an ontology, and we aren't looking at GO Terms.
                        continue;
                    }
                    results.compute( ch, ( k, v ) -> v == null ? 1L : v + 1L );
                    if ( log.isDebugEnabled() )
                        OntologyServiceImpl.log.debug( "Found obsolete or missing term: " + ch.getValue() + " - " + valueUri );
                    lastObsolete = ch;
                }
            }

            if ( results.size() > prevObsoleteCnt ) {
                OntologyServiceImpl.log.info( "Found " + results.size() + " obsolete or missing terms so far, tested " + checked + " out of " + total + " characteristics" );
                OntologyServiceImpl.log.info( "Last obsolete term seen: " + lastObsolete.getValue() + " - " + lastObsolete.getValueUri() );
            }

            prevObsoleteCnt = results.size();
        }

        OntologyServiceImpl.log.info( "Done, obsolete or missing terms found: " + results.size() );

        return results;
    }

    private void searchForCharacteristics( String queryString, Map<String, CharacteristicValueObject> previouslyUsedInSystem ) {
        StopWatch watch = new StopWatch();
        watch.start();

        Map<String, Characteristic> foundChars = characteristicService.findCharacteristicsByValueUriOrValueLike( queryString );

        /*
         * Want to flag in the web interface that these are already used by Gemma (also ignore capitalization; category
         * is always ignored; remove duplicates.)
         */
        for ( Map.Entry<String, Characteristic> characteristicByValueCount : foundChars.entrySet() ) {
            // count up number of usages; see bug 3897
            String key = characteristicByValueCount.getKey();
            if ( !previouslyUsedInSystem.containsKey( key ) ) {
                if ( OntologyServiceImpl.log.isDebugEnabled() )
                    OntologyServiceImpl.log.debug( "saw " + key + " (" + key + ") for " + characteristicByValueCount );
                previouslyUsedInSystem.put( key, characteristicToValueObject( characteristicByValueCount.getValue() ) );
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

    private CharacteristicValueObject characteristicToValueObject( Characteristic characteristic ) {
        CharacteristicValueObject vo = new CharacteristicValueObject( characteristic.getValue(), characteristic.getValueUri() );
        vo.setCategory( null );
        vo.setCategoryUri( null ); // to avoid us counting separately by category.
        vo.setAlreadyPresentInDatabase( true );
        return vo;
    }

    /**
     * given a collection of characteristics add them to the correct List
     */
    private Collection<CharacteristicValueObject> findCharacteristicsFromOntology( String searchQuery,
            boolean useNeuroCartaOntology,
            Map<String, CharacteristicValueObject> characteristicFromDatabaseWithValueUri, long timeoutMs ) throws SearchException {

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
            Collection<OntologyTerm> ontologyTerms = ontologyCache.findTerm( ontologyService, searchQuery );
            Collection<CharacteristicValueObject> characteristicsFromOntology = new HashSet<>();
            for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                // if the ontology term wasnt already found in the database
                if ( characteristicFromDatabaseWithValueUri.get( ontologyTerm.getUri() ) == null ) {
                    if ( ontologyTerm.getLabel() == null ) {
                        log.warn( "Term with null label: " + ontologyTerm.getUri() + "; it cannot be converted to a CharacteristicValueObject" );
                        continue;
                    }
                    CharacteristicValueObject phenotype = new CharacteristicValueObject( ontologyTerm.getLabel().toLowerCase(), ontologyTerm.getUri() );
                    characteristicsFromOntology.add( phenotype );
                }
            }
            return characteristicsFromOntology;
        }, ontologyServicesToUse, "terms matching " + searchQuery, timeoutMs );
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
        Set<OntologyTermSimple> categoryTerms = new HashSet<>();
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
        if ( autoLoadOntologies && !experimentalFactorOntologyService.isEnabled() ) {
            OntologyServiceImpl.log.warn( String.format( "%s is not enabled; using light-weight placeholder for categories.",
                    experimentalFactorOntologyService ) );
        }
    }


    private void initializeRelationTerms() throws IOException {
        Set<OntologyPropertySimple> relationTerms = new HashSet<>();
        Resource resource = new ClassPathResource( "/ubic/gemma/core/ontology/Relation.terms.txt" );
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( resource.getInputStream() ) ) ) {
            String line;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.startsWith( "#" ) || StringUtils.isEmpty( line ) )
                    continue;
                String[] f = StringUtils.split( line, '\t' );
                if ( f.length < 2 ) {
                    continue;
                }
                relationTerms.add( new OntologyPropertySimple( f[0], f[1] ) );
            }
            this.relationTerms = Collections.unmodifiableSet( relationTerms );
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
    private void searchForGenes( String queryString, @Nullable Taxon taxon,
            Map<String, CharacteristicValueObject> searchResults ) throws SearchException {

        SearchSettings ss = SearchSettings.builder()
                .query( queryString )
                .taxon( taxon )
                .resultType( Gene.class )
                .build();
        SearchService.SearchResultMap geneResults = this.searchService.search( ss.withFillResults( true ).withMode( SearchSettings.SearchMode.BALANCED ) );

        for ( SearchResult<Gene> sr : geneResults.getByResultObjectType( Gene.class ) ) {
            if ( sr.getResultObject() == null ) {
                log.warn( String.format( "There is no gene with ID=%d (in response to search for %s) - index out of date?",
                        sr.getResultId(), queryString ) );
                continue;
            }
            GeneValueObject g = this.geneService.loadValueObject( sr.getResultObject() );
            if ( OntologyServiceImpl.log.isDebugEnabled() )
                OntologyServiceImpl.log.debug( "Search for " + queryString + " returned: " + g );
            if ( g != null ) {
                Characteristic c = this.gene2Characteristic( g );
                searchResults.put( c.getValue(), new CharacteristicValueObject( c ) );
            }
        }
    }

    /**
     * Organize the list into 3 parts. Want to get the exact match showing up on top
     */
    static Comparator<CharacteristicValueObject> getCharacteristicComparator( String searchTerm ) {
        return Comparator
                // never show free-text terms
                .comparing( ( CharacteristicValueObject c ) -> c.getValueUri() != null, Comparator.reverseOrder() )
                .thenComparingInt( ( CharacteristicValueObject s ) -> {
                    String uri = s.getValueUri();
                    String value = s.getValue();
                    String q = searchTerm.trim().toLowerCase();
                    if ( ( uri != null && uri.equalsIgnoreCase( q ) )
                            || ( value != null && value.equalsIgnoreCase( q ) ) ) {
                        return 0; // exact match
                    } else if ( value != null && value.toLowerCase().startsWith( q ) ) {
                        return 1; // prefix match
                    } else if ( value != null && value.toLowerCase().contains( q ) ) {
                        return 2; // somewhere
                    } else {
                        return 3; // not mentioned at all!
                    }
                } )
                .thenComparing( CharacteristicValueObject::getNumTimesUsed, Comparator.reverseOrder() )            // most frequently used first
                .thenComparing( CharacteristicValueObject::isAlreadyPresentInDatabase, Comparator.reverseOrder() ) // already used terms first
                .thenComparing( c -> c.getValue() != null ? c.getValue().length() : null, Comparator.nullsLast( Comparator.naturalOrder() ) ); // shorter term first
    }

    /**
     * Find the first non-null result among loaded ontology services.
     */
    @Nullable
    private <T> T findFirst( Function<ubic.basecode.ontology.providers.OntologyService, T> function, String query, long timeoutMs ) throws TimeoutException {
        List<Future<T>> futures = new ArrayList<>( ontologyServices.size() );
        List<Object> objects = new ArrayList<>( ontologyServices.size() );
        ExecutorCompletionService<T> completionService = new ExecutorCompletionService<>( taskExecutor );
        for ( ubic.basecode.ontology.providers.OntologyService service : ontologyServices ) {
            if ( service.isOntologyLoaded() ) {
                futures.add( completionService.submit( () -> function.apply( service ) ) );
                objects.add( service );
            }
        }
        try {
            for ( int i = 0; i < futures.size(); i++ ) {
                T result = pollCompletionService( completionService, "Finding first result for " + query, futures, objects, timeoutMs );
                if ( result != null ) {
                    return result;
                }
            }
            return null;
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            log.warn( "Current thread was interrupted while finding first result for " + query + ", will return null.", e );
            return null;
        } catch ( ExecutionException e ) {
            if ( e.getCause() instanceof RuntimeException ) {
                throw ( RuntimeException ) e.getCause();
            } else {
                throw new RuntimeException( e.getCause() );
            }
        } finally {
            cancelRemainingFutures( futures, objects );
        }
    }

    /**
     * Similar to {@link #combineInThreads(CallableWithOntologyService, String, long)}, but also handles {@link OntologySearchException}.
     */
    private <T> List<T> searchInThreads( CallableWithOntologyService<Collection<T>> function, String query, long timeoutMs ) throws SearchException {
        return searchInThreads( function, ontologyServices, query, timeoutMs );
    }

    private <T> List<T> searchInThreads( CallableWithOntologyService<Collection<T>> function, List<ubic.basecode.ontology.providers.OntologyService> ontologyServices, String query, long timeoutMs ) throws SearchException {
        try {
            return combineInThreads( function, ontologyServices, query, timeoutMs );
        } catch ( TimeoutException e ) {
            throw new SearchTimeoutException( "Ontology search timed out for querying " + query, e );
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof OntologySearchException ) {
                // unwrap the exception
                throw convertBaseCodeOntologySearchExceptionToSearchException( ( OntologySearchException ) e.getCause(), query );
            } else {
                throw e;
            }
        }
    }

    @FunctionalInterface
    private interface CallableWithOntologyService<T> {
        T call( ubic.basecode.ontology.providers.OntologyService service ) throws Exception;
    }

    private <T> List<T> combineInThreads( CallableWithOntologyService<Collection<T>> work, String query, long timeoutMs ) throws TimeoutException {
        return combineInThreads( work, ontologyServices, query, timeoutMs );
    }

    /**
     * Apply a given function to all the loaded ontology service and combine the results in a set.
     * <p>
     * The functions are evaluated using Gemma's short-lived task executor.
     */
    private <T> List<T> combineInThreads( CallableWithOntologyService<Collection<T>> work, List<ubic.basecode.ontology.providers.OntologyService> ontologyServices, String query, long timeoutMs ) throws TimeoutException {
        List<Future<Collection<T>>> futures = new ArrayList<>( ontologyServices.size() );
        List<Object> objects = new ArrayList<>( ontologyServices.size() );
        ExecutorCompletionService<Collection<T>> completionService = new ExecutorCompletionService<>( taskExecutor );
        for ( ubic.basecode.ontology.providers.OntologyService os : ontologyServices ) {
            if ( os.isOntologyLoaded() ) {
                futures.add( completionService.submit( () -> work.call( os ) ) );
                objects.add( os );
            }
        }
        List<T> children = new ArrayList<>();
        try {
            for ( int i = 0; i < futures.size(); i++ ) {
                children.addAll( pollCompletionService( completionService, "Combining all the results for " + query, futures, objects, timeoutMs ) );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            log.warn( "Current thread was interrupted while finding first result for " + query + ", will return nothing.", e );
            return children;
        } catch ( ExecutionException e ) {
            if ( e.getCause() instanceof RuntimeException ) {
                throw ( RuntimeException ) e.getCause();
            } else {
                throw new RuntimeException( e.getCause() );
            }
        } finally {
            cancelRemainingFutures( futures, objects );
        }
        return children;
    }

    /**
     * Poll the next available future from the given completion service.
     *
     * @param completionService  the completion service to poll from
     * @param description        a description of the task being waited for logging purposes
     * @param futures            the list of futures being awaited
     * @param objects            the list of objects corresponding to the futures for logging purposes
     * @param timeoutMs          the amount of time to wait until a {@link TimeoutException} is raised, in milliseconds
     */
    private <T> T pollCompletionService( ExecutorCompletionService<T> completionService, String description, List<Future<T>> futures, List<?> objects, long timeoutMs ) throws InterruptedException, ExecutionException, TimeoutException {
        Assert.isTrue( futures.size() == objects.size(), "The number of futures must match the number of descriptive objects." );
        Assert.isTrue( timeoutMs >= 0, "The timeout must be zero or greater." );
        StopWatch timer = StopWatch.createStarted();
        Future<T> future;
        double recheckMs = Math.min( checkFrequencyMillis, timeoutMs );
        // a fuzz factor to prevent concurrent tasks from all timing out at the same time
        // up to 10% of the initial timeout
        double fuzzyMs = RandomUtils.nextDouble( 0.0, checkFrequencyMillis / 10.0 );
        while ( ( future = completionService.poll( ( long ) recheckMs, TimeUnit.MILLISECONDS ) ) == null ) {
            long remainingTimeMs = Math.max( timeoutMs - timer.getTime(), 0 );
            long i = futures.stream().filter( Future::isDone ).count();
            String message = String.format( "%s is taking too long (%d/%d completed so far, %s elapsed). The following tasks %s:\n\t%s",
                    description, i, futures.size(), timer,
                    remainingTimeMs > 0 ? "are still running" : "will be cancelled",
                    futures.stream()
                            .filter( f -> !f.isDone() )
                            .map( futures::indexOf )
                            .map( objects::get )
                            .map( Object::toString )
                            .collect( Collectors.joining( "\n\t" ) ) );
            if ( remainingTimeMs > 0 ) {
                log.warn( message );
            } else {
                throw new TimeoutException( message );
            }
            recheckMs = Math.min( ( recheckMs + fuzzyMs ) * exponentialBackoff, remainingTimeMs );
        }
        return future.get();
    }

    /**
     * Cancel all the remaining futures, this way if an exception occur, we don't needlessly occupy threads in the pool.
     */
    private <T> void cancelRemainingFutures( List<Future<T>> futures, List<?> objects ) {
        Assert.isTrue( futures.size() == objects.size(), "The number of futures must match the number of descriptive objects." );
        List<String> incompleteTasks = new ArrayList<>( futures.size() );
        for ( Future<?> future : futures ) {
            if ( !future.isDone() ) {
                future.cancel( true );
                incompleteTasks.add( objects.get( futures.indexOf( future ) ).toString() );
            }
        }
        if ( !incompleteTasks.isEmpty() ) {
            log.warn( "The following tasks did not have time to reply and were cancelled:\n\t"
                    + String.join( "\n\t", incompleteTasks ) );
        }
    }

    private SearchException convertBaseCodeOntologySearchExceptionToSearchException( OntologySearchException e, String query ) {
        ParseException pe = ExceptionUtils.throwableOfType( e, ParseException.class );
        if ( pe != null ) {
            return new LuceneParseSearchException( query, pe );
        } else {
            return new BaseCodeOntologySearchException( e );
        }
    }
}