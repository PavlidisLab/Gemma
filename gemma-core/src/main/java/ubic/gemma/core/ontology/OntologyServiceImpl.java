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
import org.compass.core.util.concurrent.ConcurrentHashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Has a static method for finding out which ontologies are loaded into the system and a general purpose find method
 * that delegates to the many ontology services. NOTE: Logging messages from this service are important for tracking
 * changes to annotations.
 *
 * @author pavlidis
 */
@Service
public class OntologyServiceImpl implements OntologyService {
    /**
     * Throttle how many ontology terms we retrieve. We search the ontologies in a favored order, so we can stop when we
     * find "enough stuff".
     */
    private static final int MAX_TERMS_TO_FETCH = 200;
    private static final Log log = LogFactory.getLog( OntologyServiceImpl.class.getName() );
    private static Collection<OntologyTerm> categoryTerms = null;
    private final CellLineOntologyService cellLineOntologyService = new CellLineOntologyService();
    private final CellTypeOntologyService cellTypeOntologyService = new CellTypeOntologyService();
    private final ChebiOntologyService chebiOntologyService = new ChebiOntologyService();
    private final DiseaseOntologyService diseaseOntologyService = new DiseaseOntologyService();
    private final ExperimentalFactorOntologyService experimentalFactorOntologyService = new ExperimentalFactorOntologyService();
    private final FMAOntologyService fmaOntologyService = new FMAOntologyService();
    private final GemmaOntologyService gemmaOntologyService = new GemmaOntologyService();
    private final HumanDevelopmentOntologyService humanDevelopmentOntologyService = new HumanDevelopmentOntologyService();
    private final HumanPhenotypeOntologyService humanPhenotypeOntologyService = new HumanPhenotypeOntologyService();
    private final MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = new MammalianPhenotypeOntologyService();
    private final MouseDevelopmentOntologyService mouseDevelopmentOntologyService = new MouseDevelopmentOntologyService();
    private final NIFSTDOntologyService nifstdOntologyService = new NIFSTDOntologyService();
    private final ObiService obiService = new ObiService();
    private final Collection<AbstractOntologyService> ontologyServices = new ArrayList<>();
    private final SequenceOntologyService sequenceOntologyService = new SequenceOntologyService();
    private final UberonOntologyService uberonOntologyService = new UberonOntologyService();

    private BioMaterialService bioMaterialService;
    private CharacteristicService characteristicService;
    private SearchService searchService;
    private GeneOntologyService geneOntologyService;

    @Autowired
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    @Autowired
    public void setCharacteristicService( CharacteristicService characteristicService ) {
        this.characteristicService = characteristicService;
    }

    @Autowired
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    @Autowired
    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    @Override
    public void afterPropertiesSet() {

        // We search in this order.
        this.ontologyServices.add( this.gemmaOntologyService );
        this.ontologyServices.add( this.experimentalFactorOntologyService );
        this.ontologyServices.add( this.obiService );
        this.ontologyServices.add( this.nifstdOntologyService ); // DEPRECATED?
        this.ontologyServices.add( this.fmaOntologyService ); // DEPRECATED
        this.ontologyServices.add( this.diseaseOntologyService );
        this.ontologyServices.add( this.cellTypeOntologyService );
        this.ontologyServices.add( this.chebiOntologyService );
        this.ontologyServices.add( this.mammalianPhenotypeOntologyService );
        this.ontologyServices.add( this.humanPhenotypeOntologyService );
        this.ontologyServices.add( this.mouseDevelopmentOntologyService );
        this.ontologyServices.add( this.humanDevelopmentOntologyService );
        this.ontologyServices.add( this.sequenceOntologyService );
        this.ontologyServices.add( this.cellLineOntologyService );
        this.ontologyServices.add( this.uberonOntologyService );

        for ( AbstractOntologyService serv : this.ontologyServices ) {
            serv.startInitializationThread( false );
        }

    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void countOccurrences( Collection<CharacteristicValueObject> searchResults,
            Map<String, CharacteristicValueObject> previouslyUsedInSystem ) {
        StopWatch watch = new StopWatch();
        watch.start();
        Set<String> uris = new HashSet<>();
        for ( CharacteristicValueObject cvo : searchResults ) {
            uris.add( cvo.getValueUri() );
        }

        Collection<Characteristic> existingCharacteristicsUsingTheseTerms = characteristicService.findByUri( uris );
        for ( Characteristic c : existingCharacteristicsUsingTheseTerms ) {
            // count up number of usages; see bug 3897
            String key = this.foundValueKey( c );
            if ( previouslyUsedInSystem.containsKey( key ) ) {
                previouslyUsedInSystem.get( key ).incrementOccurrenceCount();
                continue;
            }
            if ( OntologyServiceImpl.log.isDebugEnabled() )
                OntologyServiceImpl.log.debug( "saw " + key + " (" + key + ")" );
            CharacteristicValueObject vo = new CharacteristicValueObject( c );
            vo.setCategory( null );
            vo.setCategoryUri( null ); // to avoid us counting separately by category.
            vo.setAlreadyPresentInDatabase( true );
            vo.incrementOccurrenceCount();
            previouslyUsedInSystem.put( key, vo );
        }

        if ( OntologyServiceImpl.log.isDebugEnabled() || ( watch.getTime() > 100
                && previouslyUsedInSystem.size() > 0 ) )
            OntologyServiceImpl.log
                    .info( "found " + previouslyUsedInSystem.size() + " matching characteristics used in the database"
                            + " in " + watch.getTime() + " ms " + " Filtered from initial set of "
                            + existingCharacteristicsUsingTheseTerms.size() );
    }

    /**
     * Using the ontology and values in the database, for a search searchQuery given by the client give an ordered list
     * of possible choices
     */
    @Override
    public Collection<CharacteristicValueObject> findExperimentsCharacteristicTags( String searchQueryString,
            boolean useNeuroCartaOntology ) {

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
    public Collection<OntologyIndividual> findIndividuals( String givenSearch ) {

        String query = OntologySearch.stripInvalidCharacters( givenSearch );
        Collection<OntologyIndividual> results = new HashSet<>();

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyIndividual> found = ontology.findIndividuals( query );
            if ( found != null )
                results.addAll( found );
        }

        return results;
    }

    @Override
    public Collection<VocabCharacteristic> findTermAsCharacteristic( String search ) {

        String query = OntologySearch.stripInvalidCharacters( search );
        Collection<VocabCharacteristic> results = new HashSet<>();

        if ( StringUtils.isBlank( query ) ) {
            return results;
        }

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyTerm> found = ontology.findTerm( query );
            if ( found != null )
                results.addAll( this.convert( new HashSet<OntologyResource>( found ) ) );
        }

        return results;
    }

    @Override
    public Collection<OntologyTerm> findTerms( String search ) {

        String query = OntologySearch.stripInvalidCharacters( search );

        Collection<OntologyTerm> results = new HashSet<>();

        if ( StringUtils.isBlank( query ) ) {
            return results;
        }

        for ( AbstractOntologyService ontology : ontologyServices ) {
            if ( ontology.isOntologyLoaded() ) {
                Collection<OntologyTerm> found = ontology.findTerm( query );
                if ( found != null )
                    results.addAll( found );
            }
        }

        if ( geneOntologyService.isReady() )
            results.addAll( geneOntologyService.findTerm( search ) );

        return results;
    }

    @Override
    public Collection<CharacteristicValueObject> findTermsInexact( String givenQueryString, Taxon taxon ) {

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

        Collection<? extends OntologyResource> results = null;
        Collection<CharacteristicValueObject> searchResults = new HashSet<>();

        Map<String, CharacteristicValueObject> previouslyUsedInSystem = new HashMap<>();

        this.countOccurrences( queryString, previouslyUsedInSystem );
        this.searchForGenes( queryString, taxon, searchResults );

        for ( AbstractOntologyService service : this.ontologyServices ) {
            if ( !service.isOntologyLoaded() )
                continue;
            
            try {
                results = service.findResources( queryString );
            } catch ( Exception e ) {
                log.warn( e.getMessage() ); // parse errors, etc.
            }
            if ( results == null || results.isEmpty() )
                continue;
            
            if ( OntologyServiceImpl.log.isDebugEnabled() )
                OntologyServiceImpl.log
                        .debug( "found " + results.size() + " from " + service.getClass().getSimpleName() + " in "
                                + watch.getTime() + " ms" );
            searchResults.addAll( CharacteristicValueObject
                    .characteristic2CharacteristicVO( this.termsToCharacteristics( results ) ) );

            if ( searchResults.size() > OntologyServiceImpl.MAX_TERMS_TO_FETCH ) {
                break;
            }
        }

        this.countOccurrences( searchResults, previouslyUsedInSystem );

        // get GO terms, if we don't already have a lot of possibilities. (might have to adjust this)
        if ( searchResults.size() < OntologyServiceImpl.MAX_TERMS_TO_FETCH && geneOntologyService.isReady() ) {
            searchResults.addAll( CharacteristicValueObject.characteristic2CharacteristicVO(
                    this.termsToCharacteristics( geneOntologyService.findTerm( queryString ) ) ) );
        }

        // Sort the results rather elaborately.
        Collection<CharacteristicValueObject> sortedResults = this
                .sort( previouslyUsedInSystem, searchResults, queryString );

        if ( watch.getTime() > 1000 ) {
            OntologyServiceImpl.log
                    .info( "Ontology term query for: " + givenQueryString + ": " + watch.getTime() + "ms" );
        }

        return sortedResults;

    }

    @Override
    public Collection<OntologyTerm> getCategoryTerms() {

        if ( !experimentalFactorOntologyService.isOntologyLoaded() ) {
            OntologyServiceImpl.log.warn( "EFO is not loaded" );
        }

        /*
         * Requires EFO, OBI and SO. If one of them isn't loaded, the terms are filled in with placeholders.
         */

        if ( OntologyServiceImpl.categoryTerms == null || OntologyServiceImpl.categoryTerms.isEmpty() ) {

            this.initializeCategoryTerms();

        }
        return OntologyServiceImpl.categoryTerms;

    }

    @Override
    public CellLineOntologyService getCellLineOntologyService() {
        return cellLineOntologyService;
    }

    @Override
    public ChebiOntologyService getChebiOntologyService() {
        return chebiOntologyService;
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
    public FMAOntologyService getFmaOntologyService() {
        return fmaOntologyService;
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
    public NIFSTDOntologyService getNifstfOntologyService() {
        return nifstdOntologyService;
    }

    @Override
    public ObiService getObiService() {
        return obiService;
    }

    @Override
    public OntologyResource getResource( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyResource resource = ontology.getResource( uri );
            if ( resource != null )
                return resource;
        }
        return null;
    }

    @Override
    public SequenceOntologyService getSequenceOntologyService() {
        return this.sequenceOntologyService;
    }

    @Override
    public OntologyTerm getTerm( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyTerm term = ontology.getTerm( uri );
            if ( term != null )
                return term;
        }
        // TODO: doesn't include GO.
        return null;
    }

    /**
     * @return true if the Uri is an ObsoleteClass. This will only work if the ontology in question is loaded.
     */
    @Override
    public boolean isObsolete( String uri ) {
        if ( uri == null )
            return false;
        OntologyTerm t = this.getTerm( uri );
        return t != null && t.isTermObsolete();
    }

    @Override
    public void reindexAllOntologies() {
        for ( AbstractOntologyService serv : this.ontologyServices ) {
            if ( serv.isOntologyLoaded() ) {
                OntologyServiceImpl.log.info( "Reindexing: " + serv );
                try {
                    serv.index( true );
                } catch ( Exception e ) {
                    OntologyServiceImpl.log.error( "Failed to index " + serv + ": " + e.getMessage(), e );
                }
            } else {
                if ( serv.isEnabled() )
                    OntologyServiceImpl.log.info( "Not available for reindexing: " + serv );
            }
        }
    }

    @Override
    public void reinitializeAllOntologies() {
        for ( AbstractOntologyService serv : this.ontologyServices ) {
            serv.startInitializationThread( true );
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

        Collection<Characteristic> current = bm.getCharacteristics();
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
            vc.setEvidenceCode( GOEvidenceCode.IC ); // assume: manually added
            // characteristic
        }

        if ( vc instanceof VocabCharacteristic && this.isObsolete( vc.getValueUri() ) ) {
            OntologyServiceImpl.log.info( vc + " is obsolete, not saving" );
            return;
        }

        if ( ee == null )
            throw new IllegalArgumentException( "Experiment cannot be null" );

        OntologyServiceImpl.log
                .info( "Adding characteristic '" + vc.getValue() + "' to " + ee.getShortName() + " (ID=" + ee.getId()
                        + ") : " + vc );

        ee.getCharacteristics().add( vc );
    }

    @Override
    public void sort( List<CharacteristicValueObject> characteristics ) {
        Collections.sort( characteristics, new CharacteristicComparator() );
    }

    /**
     * Convert raw ontology resources into VocabCharacteristics.
     */
    @Override
    public Collection<VocabCharacteristic> termsToCharacteristics(
            final Collection<? extends OntologyResource> terms ) {

        Collection<VocabCharacteristic> results = new HashSet<>();

        if ( ( terms == null ) || ( terms.isEmpty() ) )
            return results;

        for ( OntologyResource res : terms ) {

            if ( res == null )
                continue;

            VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
            if ( res instanceof OntologyTerm ) {
                OntologyTerm term = ( OntologyTerm ) res;
                vc.setValue( term.getTerm() );
                vc.setValueUri( term.getUri() );
                vc.setDescription( term.getComment() );
            } else if ( res instanceof OntologyIndividual ) {
                OntologyIndividual indi = ( OntologyIndividual ) res;
                vc.setValue( indi.getLabel() );
                vc.setValueUri( indi.getUri() );
                vc.setDescription( "Individual" );
            } else {
                OntologyServiceImpl.log.warn( "What is it? " + res );
                continue;
            }

            if ( vc.getValue() == null )
                continue;
            results.add( vc );

        }
        OntologyServiceImpl.log.debug( "returning " + results.size() + " terms after filter" );

        return results;
    }

    /**
     * Given a collection of ontology terms converts them to a collection of VocabCharacteristics
     */
    private Collection<VocabCharacteristic> convert( final Collection<OntologyResource> resources ) {

        Collection<VocabCharacteristic> converted = new HashSet<>();

        if ( ( resources == null ) || ( resources.isEmpty() ) )
            return converted;

        for ( OntologyResource res : resources ) {
            VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();

            // If there is no URI we don't want to send it back (ie useless)
            if ( ( res.getUri() == null ) || StringUtils.isEmpty( res.getUri() ) )
                continue;

            if ( res instanceof OntologyTerm ) {
                OntologyTerm term = ( OntologyTerm ) res;
                vc.setValue( term.getTerm() );
                vc.setValueUri( term.getUri() );
                vc.setDescription( term.getComment() );
            }
            if ( res instanceof OntologyIndividual ) {
                OntologyIndividual indi = ( OntologyIndividual ) res;
                vc.setValue( indi.getLabel() );
                vc.setValueUri( indi.getUri() );
                vc.setDescription( "Individual" );
            }

            converted.add( vc );
        }

        return converted;
    }

    private void countOccurrences( String queryString, Map<String, CharacteristicValueObject> previouslyUsedInSystem ) {
        StopWatch watch = new StopWatch();
        watch.start();

        Collection<Characteristic> foundChars = characteristicService.findByValue( queryString );

        /*
         * Want to flag in the web interface that these are already used by Gemma (also ignore capitalization; category
         * is always ignored; remove duplicates.)
         */
        for ( Characteristic characteristic : foundChars ) {
            // count up number of usages; see bug 3897
            String key = this.foundValueKey( characteristic );
            if ( previouslyUsedInSystem.containsKey( key ) ) {
                previouslyUsedInSystem.get( key ).incrementOccurrenceCount();
                continue;
            }
            if ( OntologyServiceImpl.log.isDebugEnabled() )
                OntologyServiceImpl.log.debug( "saw " + key + " (" + key + ") for " + characteristic );
            CharacteristicValueObject vo = new CharacteristicValueObject( characteristic );
            vo.setCategory( null );
            vo.setCategoryUri( null ); // to avoid us counting separately by category.
            vo.setAlreadyPresentInDatabase( true );
            vo.incrementOccurrenceCount();
            previouslyUsedInSystem.put( key, vo );
        }

        if ( OntologyServiceImpl.log.isDebugEnabled() || ( watch.getTime() > 100
                && previouslyUsedInSystem.size() > 0 ) )
            OntologyServiceImpl.log
                    .info( "found " + previouslyUsedInSystem.size() + " matching characteristics used in the database"
                            + " in " + watch.getTime() + " ms " + " Filtered from initial set of " + foundChars
                                    .size() );

    }

    /**
     * given a collection of characteristics add them to the correct List
     */
    private Collection<CharacteristicValueObject> findCharacteristicsFromOntology( String searchQuery,
            boolean useNeuroCartaOntology,
            Map<String, CharacteristicValueObject> characteristicFromDatabaseWithValueUri ) {

        Collection<CharacteristicValueObject> characteristicsFromOntology = new HashSet<>();

        // in neurocarta we don't need to search all Ontologies
        Collection<AbstractOntologyService> ontologyServicesToUse = new HashSet<>();

        if ( useNeuroCartaOntology ) {
            ontologyServicesToUse.add( this.nifstdOntologyService );
            ontologyServicesToUse.add( this.fmaOntologyService );
            ontologyServicesToUse.add( this.obiService );

        } else {
            ontologyServicesToUse = this.ontologyServices;
        }

        // search all Ontology
        for ( AbstractOntologyService ontologyService : ontologyServicesToUse ) {

            Collection<OntologyTerm> ontologyTerms = ontologyService.findTerm( searchQuery );

            for ( OntologyTerm ontologyTerm : ontologyTerms ) {

                // if the ontology term wasnt already found in the database
                if ( characteristicFromDatabaseWithValueUri.get( ontologyTerm.getUri() ) == null ) {

                    CharacteristicValueObject phenotype = new CharacteristicValueObject( -1L,
                            ontologyTerm.getLabel().toLowerCase(), ontologyTerm.getUri() );

                    characteristicsFromOntology.add( phenotype );
                }
            }
        }

        return characteristicsFromOntology;
    }

    private String foundValueKey( Characteristic c ) {
        if ( c instanceof VocabCharacteristic && ( StringUtils.isNotBlank( c.getValueUri() ) ) ) {
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
    private Characteristic gene2Characteristic( Gene g ) {
        VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
        vc.setCategory( "gene" );
        vc.setCategoryUri( "http://purl.org/commons/hcls/gene" );
        vc.setValue( g.getOfficialSymbol() + " [" + g.getTaxon().getCommonName() + "]" + " " + g.getOfficialName() );
        vc.setDescription( g.toString() );
        if ( g.getNcbiGeneId() != null ) {
            vc.setValueUri( "http://purl.org/commons/record/ncbi_gene/" + g.getNcbiGeneId() );
        }
        return vc;
    }

    private synchronized void initializeCategoryTerms() {

        URL termUrl = OntologyServiceImpl.class.getResource( "/ubic/gemma/core/ontology/EFO.factor.categories.txt" );
        OntologyServiceImpl.categoryTerms = new ConcurrentHashSet<>();
        try (BufferedReader reader = new BufferedReader( new InputStreamReader( termUrl.openStream() ) )) {
            String line;
            boolean warned = false;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.startsWith( "#" ) || StringUtils.isEmpty( line ) )
                    continue;
                String[] f = StringUtils.split( line, '\t' );
                if ( f.length < 2 ) {
                    continue;
                }
                OntologyTerm t = this.getTerm( f[0] );
                if ( t == null ) {
                    // this is not great. We might want to let it expire and redo it later if the ontology
                    // becomes
                    // available. Inference will not be available.
                    if ( !warned ) {
                        OntologyServiceImpl.log
                                .info( "Ontology needed is not loaded? Using light-weight placeholder for " + f[0]
                                        + " (further warnings hidden)" );

                        warned = true;
                    }
                    t = new OntologyTermSimple( f[0], f[1] );
                }

                OntologyServiceImpl.categoryTerms.add( t );
            }

        } catch ( IOException ioe ) {
            OntologyServiceImpl.log
                    .error( "Error reading from term list '" + termUrl + "'; returning general term list", ioe );
            OntologyServiceImpl.categoryTerms = null;
        }

        OntologyServiceImpl.categoryTerms = Collections.unmodifiableCollection( OntologyServiceImpl.categoryTerms );
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
     * @param taxon okay if null, but then all matches returned.
     * @param searchResults added to this
     */
    private void searchForGenes( String queryString, Taxon taxon,
            Collection<CharacteristicValueObject> searchResults ) {

        SearchSettings ss = SearchSettings.Factory.newInstance();
        ss.setQuery( queryString );
        ss.noSearches();
        ss.setTaxon( taxon );
        ss.setSearchGenes( true );
        Map<Class<?>, List<SearchResult>> geneResults = this.searchService.search( ss, true, false );

        if ( geneResults.containsKey( Gene.class ) ) {
            for ( SearchResult sr : geneResults.get( Gene.class ) ) {
                Gene g = ( Gene ) sr.getResultObject();
                if ( OntologyServiceImpl.log.isDebugEnabled() )
                    OntologyServiceImpl.log.debug( "Search for " + queryString + " returned: " + g );
                searchResults.add( new CharacteristicValueObject( this.gene2Characteristic( g ) ) );
            }
        }
    }

    /**
     * @param alreadyUsedResults items already in the system; remove singleton free-text terms.
     * @param otherResults other results
     * @param searchTerm the query
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

        this.sort( sortedResultsExact );
        this.sort( sortedResultsStartsWith );
        this.sort( sortedResultsBottom );

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
     * Sorts Characteristics in our preferred ordering
     */
    private class CharacteristicComparator implements Comparator<CharacteristicValueObject> {

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