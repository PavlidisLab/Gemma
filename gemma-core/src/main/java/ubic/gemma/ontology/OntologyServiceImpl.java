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
package ubic.gemma.ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.util.concurrent.ConcurrentHashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.basecode.ontology.providers.CellTypeOntologyService;
import ubic.basecode.ontology.providers.ChebiOntologyService;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.HumanDevelopmentOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MouseDevelopmentOntologyService;
import ubic.basecode.ontology.providers.NIFSTDOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.basecode.ontology.providers.SequenceOntologyService;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;

/**
 * Has a static method for finding out which ontologies are loaded into the system and a general purpose find method
 * that delegates to the many ontology services. NOTE: Logging messages from this service are important for tracking
 * changes to annotations.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class OntologyServiceImpl implements OntologyService {

    // Private class for sorting Characteristics
    static class TermComparator implements Comparator<Characteristic>, Serializable {

        private static final long serialVersionUID = 1L;
        String comparator;

        public TermComparator( String comparator ) {
            super();
            this.comparator = comparator;
        }

        @Override
        public int compare( Characteristic o1, Characteristic o2 ) {
            String term1 = o1.getValue();
            String term2 = o2.getValue();

            if ( term1.equals( term2 ) ) return 0;

            if ( term1.equals( comparator ) ) return 1;

            if ( term2.equals( comparator ) ) return -1;

            if ( term1.startsWith( comparator ) ) {
                if ( term2.startsWith( comparator ) ) return 0;
                return 1;
            } else if ( term2.startsWith( comparator ) ) {
                return -1;
            }

            return 0;

        }
    }

    /**
     * Sorts Characteristics in our preferred ordering:
     * <ol>
     * <li>
     * </ol>
     */
    private class CharacteristicComparator implements Comparator<Characteristic> {

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare( Characteristic o1, Characteristic o2 ) {

            if ( o1.getValue().length() == o2.getValue().length() ) {
                if ( o1.getDescription().startsWith( USED ) ) {
                    if ( o2.getDescription().startsWith( USED ) ) {
                        // both are used, break tie.
                        return compareByUri( o1, o2 );
                    }
                    // o1 is used, o2 is not; o1 should be first.
                    return -1;

                } else if ( o2.getDescription().startsWith( USED ) ) {
                    // o2 is used and o1 is not; o2 should be first.
                    return 1;
                } else {
                    // neither is used.
                    return compareByUri( o1, o2 );
                }
            }

            // we don't use the actual text, because we've already declared these matches, so we just put shorter ones
            // first.
            return o1.getValue().length() < o2.getValue().length() ? -1 : 1;
        }

        // break ties by uri and/or value.
        private int compareByUri( Characteristic o1, Characteristic o2 ) {
            // both are used. Break tie based on whether it has a URI
            if ( o1 instanceof VocabCharacteristic && ( ( VocabCharacteristic ) o1 ).getValueUri() != null ) {
                if ( !( o2 instanceof VocabCharacteristic ) || ( ( VocabCharacteristic ) o2 ).getValueUri() == null ) {
                    return -1;
                }
                // both have URIs, sort by value
                return o1.getValue().toLowerCase().compareTo( o2.getValue().toLowerCase() );
            } else if ( o2 instanceof VocabCharacteristic && ( ( VocabCharacteristic ) o2 ).getValueUri() != null ) {
                // we know o1 does not have a uri.
                return 1;
            }
            // both not having uris
            return o1.getValue().toLowerCase().compareTo( o2.getValue().toLowerCase() );
        }
    }

    /**
     * used to indicate a term is already used in the system.
     */
    static final String USED = " -USED- ";

    private static Collection<OntologyTerm> categoryterms = null;

    private static Log log = LogFactory.getLog( OntologyServiceImpl.class.getName() );

    /**
     * Throttle how many ontology terms we retrieve. We search the ontologies in a favored order, so we can stop when we
     * find "enough stuff".
     */
    private static final int MAX_TERMS_TO_FETCH = 200;

    @Autowired
    private BioMaterialService bioMaterialService;

    private CellTypeOntologyService cellTypeOntologyService = new CellTypeOntologyService();

    @Autowired
    private CharacteristicService characteristicService;
    private ChebiOntologyService chebiOntologyService = new ChebiOntologyService();
    private DiseaseOntologyService diseaseOntologyService = new DiseaseOntologyService();
    @Autowired
    private ExpressionExperimentService eeService;
    private ExperimentalFactorOntologyService experimentalFactorOntologyService = new ExperimentalFactorOntologyService();
    private FMAOntologyService fmaOntologyService = new FMAOntologyService();
    @Autowired
    private GeneOntologyService geneOntologyService;
    private HumanDevelopmentOntologyService humanDevelopmentOntologyService = new HumanDevelopmentOntologyService();
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = new HumanPhenotypeOntologyService();
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = new MammalianPhenotypeOntologyService();
    private MouseDevelopmentOntologyService mouseDevelopmentOntologyService = new MouseDevelopmentOntologyService();
    private NIFSTDOntologyService nifstdOntologyService = new NIFSTDOntologyService();

    private ObiService obiService = new ObiService();

    private Collection<AbstractOntologyService> ontologyServices = new ArrayList<>();

    @Autowired
    private SearchService searchService;

    private SequenceOntologyService sequenceOntologyService = new SequenceOntologyService();

    @Override
    public void afterPropertiesSet() {

        // We search in this order.
        this.ontologyServices.add( this.experimentalFactorOntologyService );
        this.ontologyServices.add( this.obiService );
        this.ontologyServices.add( this.nifstdOntologyService );
        this.ontologyServices.add( this.fmaOntologyService );
        this.ontologyServices.add( this.diseaseOntologyService );
        this.ontologyServices.add( this.cellTypeOntologyService );
        this.ontologyServices.add( this.chebiOntologyService );
        this.ontologyServices.add( this.mammalianPhenotypeOntologyService );
        this.ontologyServices.add( this.humanPhenotypeOntologyService );
        this.ontologyServices.add( this.mouseDevelopmentOntologyService );
        this.ontologyServices.add( this.humanDevelopmentOntologyService );
        this.ontologyServices.add( this.sequenceOntologyService );

        for ( AbstractOntologyService serv : this.ontologyServices ) {
            serv.startInitializationThread( false );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#findExactTerm(java.lang.String, java.lang.String,
     * ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Characteristic> findExactTerm( String givenQueryString, String categoryUri, Taxon taxon ) {

        if ( StringUtils.isBlank( givenQueryString ) ) return null;

        StopWatch watch = new StopWatch();
        watch.start();

        String queryString = OntologySearch.stripInvalidCharacters( givenQueryString );
        if ( StringUtils.isBlank( queryString ) ) {
            log.warn( "The query was not valid (ended up being empty): " + givenQueryString );
            return new HashSet<>();
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "starting findExactTerm for " + queryString + ". Timing information begins from here" );
        }

        Collection<? extends OntologyResource> results;
        Collection<Characteristic> searchResults = new HashSet<>();

        Collection<String> foundValues = new HashSet<>();

        Collection<Characteristic> previouslyUsedInSystem = new HashSet<>();

        // this should be very fast.
        Collection<Characteristic> foundChars = characteristicService.findByValue( queryString );

        /*
         * remove duplicates
         */
        if ( foundChars != null ) {
            for ( Characteristic characteristic : foundChars ) {
                if ( foundValues.contains( foundValueKey( characteristic ) ) ) continue;
                /*
                 * Want to flag in the web interface that these are already used by Gemma; value object will turn this
                 * into a proper flag.
                 */
                characteristic.setDescription( USED + characteristic.getDescription() );
                previouslyUsedInSystem.add( characteristic );
                foundValues.add( foundValueKey( characteristic ) );
            }
        }

        if ( log.isDebugEnabled() || ( watch.getTime() > 100 && previouslyUsedInSystem.size() > 0 ) )
            log.info( "found " + previouslyUsedInSystem.size() + " matching characteristics used in the database"
                    + " in " + watch.getTime() + " ms" );

        searchForGenes( queryString, categoryUri, taxon, searchResults );

        for ( AbstractOntologyService serv : this.ontologyServices ) {
            if ( !serv.isOntologyLoaded() ) continue;
            results = serv.findResources( queryString );

            if ( results.isEmpty() ) continue;
            if ( log.isDebugEnabled() )
                log.debug( "found " + results.size() + " from " + serv.getClass().getSimpleName() + " in "
                        + watch.getTime() + " ms" );
            searchResults.addAll( filter( results, queryString ) );

            if ( searchResults.size() > MAX_TERMS_TO_FETCH ) {
                break;
            }
        }

        if ( searchResults.size() < MAX_TERMS_TO_FETCH && geneOntologyService.isReady() ) {
            searchResults.addAll( filter( geneOntologyService.findTerm( queryString ), queryString ) );
        }

        // Sort the individual results.
        Collection<Characteristic> sortedResults = sort( previouslyUsedInSystem, searchResults, queryString,
                foundValues );

        if ( watch.getTime() > 1000 ) {
            log.info( "Ontology term query for: " + givenQueryString + ": " + watch.getTime() + "ms" );
        }

        return sortedResults;

    }

    @Override
    public Collection<CharacteristicValueObject> findExactTermValueObject( String givenQueryString, String categoryUri,
            Taxon taxon ) {
        Collection<Characteristic> terms = findExactTerm( givenQueryString, categoryUri, taxon );
        return CharacteristicValueObject.characteristic2CharacteristicVO( terms );

    }

    /**
     * Using the ontology and values in the database, for a search searchQuery given by the client give an ordered list
     * of possible choices
     */
    @Override
    public Collection<CharacteristicValueObject> findExperimentsCharacteristicTags( String searchQueryString,
            boolean useNeuroCartaOntology ) {

        String searchQuery = OntologySearch.stripInvalidCharacters( searchQueryString );

        if ( searchQuery == null || searchQuery.length() < 3 ) {
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

            if ( characteristicInDatabase.getValueUri() != null && !characteristicInDatabase.getValueUri().equals( "" ) ) {
                characteristicFromDatabaseWithValueUri.put( characteristicInDatabase.getValueUri(),
                        characteristicInDatabase );
            } else {
                // free txt, no value uri
                characteristicFromDatabaseFreeText.add( characteristicInDatabase );
            }
        }

        // search the ontology for the given searchTerm, but if already found in the database dont add it again
        Collection<CharacteristicValueObject> characteristicsFromOntology = findCharacteristicsFromOntology(
                searchQuery, useNeuroCartaOntology, characteristicFromDatabaseWithValueUri );

        // order to show the the term: 1-exactMatch, 2-startWith, 3-substring and 4- no rule
        // order to show values for each List : 1-From database with Uri, 2- from Ontology, 3- from from database with
        // no Uri
        Collection<CharacteristicValueObject> characteristicsWithExactMatch = new ArrayList<>();
        Collection<CharacteristicValueObject> characteristicsStartWithQuery = new ArrayList<>();
        Collection<CharacteristicValueObject> characteristicsSubstring = new ArrayList<>();
        Collection<CharacteristicValueObject> characteristicsNoRuleFound = new ArrayList<>();

        // from the database with a uri
        putCharacteristicsIntoSpecificList( searchQuery, characteristicFromDatabaseWithValueUri.values(),
                characteristicsWithExactMatch, characteristicsStartWithQuery, characteristicsSubstring,
                characteristicsNoRuleFound );
        // from the ontology
        putCharacteristicsIntoSpecificList( searchQuery, characteristicsFromOntology, characteristicsWithExactMatch,
                characteristicsStartWithQuery, characteristicsSubstring, characteristicsNoRuleFound );
        // from the database with no uri
        putCharacteristicsIntoSpecificList( searchQuery, characteristicFromDatabaseFreeText,
                characteristicsWithExactMatch, characteristicsStartWithQuery, characteristicsSubstring,
                characteristicsNoRuleFound );

        List<CharacteristicValueObject> allCharactersticsFound = new ArrayList<>();
        allCharactersticsFound.addAll( characteristicsWithExactMatch );
        allCharactersticsFound.addAll( characteristicsStartWithQuery );
        allCharactersticsFound.addAll( characteristicsSubstring );
        allCharactersticsFound.addAll( characteristicsNoRuleFound );

        // limit the size of the returned phenotypes to 100 terms
        if ( allCharactersticsFound.size() > 100 ) {
            return allCharactersticsFound.subList( 0, 100 );
        }

        return allCharactersticsFound;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#findIndividuals(java.lang.String)
     */
    @Override
    public Collection<OntologyIndividual> findIndividuals( String givenSearch ) {

        String query = OntologySearch.stripInvalidCharacters( givenSearch );
        Collection<OntologyIndividual> results = new HashSet<>();

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyIndividual> found = ontology.findIndividuals( query );
            if ( found != null ) results.addAll( found );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#findTermAsCharacteristic(java.lang.String)
     */
    @Override
    public Collection<VocabCharacteristic> findTermAsCharacteristic( String search ) {

        String query = OntologySearch.stripInvalidCharacters( search );
        Collection<VocabCharacteristic> results = new HashSet<>();

        if ( StringUtils.isBlank( query ) ) {
            return results;
        }

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyTerm> found = ontology.findTerm( query );
            if ( found != null ) results.addAll( convert( new HashSet<OntologyResource>( found ) ) );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#findTerms(java.lang.String)
     */
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
                if ( found != null ) results.addAll( found );
            }
        }

        if ( geneOntologyService.isReady() ) results.addAll( geneOntologyService.findTerm( search ) );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getCategoryTerms()
     */
    @Override
    public Collection<OntologyTerm> getCategoryTerms() {

        if ( !experimentalFactorOntologyService.isOntologyLoaded() ) {
            log.warn( "EFO is not loaded" );
        }

        /*
         * Requires EFO, OBI and SO. If one of them isn't loaded, the terms are filled in with placeholders.
         */

        if ( categoryterms == null || categoryterms.isEmpty() ) {

            initializeCategoryTerms();

        }
        return categoryterms;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getChebiOntologyService()
     */
    @Override
    public ChebiOntologyService getChebiOntologyService() {
        return chebiOntologyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getDiseaseOntologyService()
     */
    @Override
    public DiseaseOntologyService getDiseaseOntologyService() {
        return diseaseOntologyService;
    }

    @Override
    public ExperimentalFactorOntologyService getExperimentalFactorOntologyService() {
        return experimentalFactorOntologyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getFmaOntologyService()
     */
    @Override
    public FMAOntologyService getFmaOntologyService() {
        return fmaOntologyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getHumanPhenotypeOntologyService()
     */
    @Override
    public HumanPhenotypeOntologyService getHumanPhenotypeOntologyService() {
        return humanPhenotypeOntologyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getMammalianPhenotypeOntologyService()
     */
    @Override
    public MammalianPhenotypeOntologyService getMammalianPhenotypeOntologyService() {
        return mammalianPhenotypeOntologyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getNifstfOntologyService()
     */
    @Override
    public NIFSTDOntologyService getNifstfOntologyService() {
        return nifstdOntologyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getObiService()
     */
    @Override
    public ObiService getObiService() {
        return obiService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getResource(java.lang.String)
     */
    @Override
    public OntologyResource getResource( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyResource resource = ontology.getResource( uri );
            if ( resource != null ) return resource;
        }
        return null;
    }

    @Override
    public SequenceOntologyService getSequenceOntologyService() {
        return this.sequenceOntologyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#getTerm(java.lang.String)
     */
    @Override
    public OntologyTerm getTerm( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyTerm term = ontology.getTerm( uri );
            if ( term != null ) return term;
        }
        // TODO: doesn't include GO.
        return null;
    }

    /**
     * @param vc
     * @return true if the valudUri isa ObsoleteClass. This will only work if the ontology in question is loaded.
     */
    @Override
    public boolean isObsolete( String uri ) {
        OntologyTerm t = this.getTerm( uri );
        if ( t != null && t.isTermObsolete() ) return true;

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#reindexAllOntologies()
     */
    @Override
    public void reindexAllOntologies() {
        for ( AbstractOntologyService serv : this.ontologyServices ) {
            if ( serv.isOntologyLoaded() ) {
                log.info( "Reindexing: " + serv );
                try {
                    serv.index( true );
                } catch ( Exception e ) {
                    log.error( "Failed to index " + serv + ": " + e.getMessage(), e );
                }
            } else {
                if ( serv.isEnabled() ) log.info( "Not available for reindexing: " + serv );
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#reinitializeAllOntologies()
     */
    @Override
    public void reinitializeAllOntologies() {
        for ( AbstractOntologyService serv : this.ontologyServices ) {
            serv.startInitializationThread( true );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#removeBioMaterialStatement(java.lang.Long,
     * ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void removeBioMaterialStatement( Long characterId, BioMaterial bm ) {
        Characteristic vc = characteristicService.load( characterId );
        if ( vc == null )
            throw new IllegalArgumentException( "No characteristic with id=" + characterId + " was foundF" );
        bm.getCharacteristics().remove( vc );
        characteristicService.delete( characterId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.ontology.OntologyService#saveBioMaterialStatement(ubic.gemma.model.common.description.Characteristic,
     * ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void saveBioMaterialStatement( Characteristic vc, BioMaterial bm ) {

        log.debug( "Vocab Characteristic: " + vc );

        vc.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        Set<Characteristic> chars = new HashSet<>();
        chars.add( vc );

        Collection<Characteristic> current = bm.getCharacteristics();
        if ( current == null )
            current = new HashSet<>( chars );
        else
            current.addAll( chars );

        for ( Characteristic characteristic : chars ) {
            log.info( "Adding characteristic to " + bm + " : " + characteristic );
        }

        bm.setCharacteristics( current );
        bioMaterialService.update( bm );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#saveExpressionExperimentStatement(ubic.gemma.model.common.description.
     * Characteristic, ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void saveExpressionExperimentStatement( Characteristic vc, ExpressionExperiment ee ) {
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

        if ( vc instanceof VocabCharacteristic && isObsolete( ( ( VocabCharacteristic ) vc ).getValueUri() ) ) {
            log.info( vc + " is obsolete, not saving" );
            return;
        }

        if ( ee == null ) throw new IllegalArgumentException( "Experiment cannot be null" );

        log.info( "Adding characteristic '" + vc.getValue() + "' to " + ee.getShortName() + " (ID=" + ee.getId()
                + ") : " + vc );

        ee.getCharacteristics().add( vc );
        eeService.update( ee );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyService#saveExpressionExperimentStatements(java.util.Collection,
     * ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void saveExpressionExperimentStatements( Collection<Characteristic> vc, ExpressionExperiment ee ) {
        for ( Characteristic characteristic : vc ) {
            // load necessary to make sure we are dealing with the persistent version.
            saveExpressionExperimentStatement( characteristic, eeService.thawLite( eeService.load( ee.getId() ) ) );
        }
    }

    /**
     * @param sortedResultsExact
     */
    @Override
    public void sort( List<Characteristic> sortedResultsExact ) {
        Collections.sort( sortedResultsExact, new CharacteristicComparator() );
    }

    /**
     * Given a collection of ontology terms converts them to a collection of VocabCharacteristics
     * 
     * @param terms
     * @param filterTerm
     * @return
     */
    private Collection<VocabCharacteristic> convert( final Collection<OntologyResource> resources ) {

        Collection<VocabCharacteristic> converted = new HashSet<>();

        if ( ( resources == null ) || ( resources.isEmpty() ) ) return converted;

        for ( OntologyResource res : resources ) {
            VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();

            // If there is no URI we don't want to send it back (ie useless)
            if ( ( res.getUri() == null ) || StringUtils.isEmpty( res.getUri() ) ) continue;

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

    /**
     * Given a collection of ontology terms will filter out all the terms that don't have the filter term in their
     * label.
     * 
     * @param terms
     * @param filterTerm
     * @return
     */
    private Collection<VocabCharacteristic> filter( final Collection<? extends OntologyResource> terms,
            final String filter ) {

        Collection<VocabCharacteristic> filtered = new HashSet<>();

        if ( ( terms == null ) || ( terms.isEmpty() ) ) return filtered;

        String caseInsensitiveFilter = filter.toLowerCase().trim();

        for ( OntologyResource res : terms ) {
            if ( StringUtils.isNotEmpty( res.getLabel() )
                    && res.getLabel().toLowerCase().indexOf( caseInsensitiveFilter ) > -1 ) {
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
                    log.warn( "What is it? " + res );
                    continue;
                }

                filtered.add( vc );
            }
        }
        log.debug( "returning " + filtered.size() + " terms after filter" );

        return filtered;
    }

    /** given a collection of characteristics add them to the correct List */
    private Collection<CharacteristicValueObject> findCharacteristicsFromOntology( String searchQuery,
            boolean useNeuroCartaOntology, Map<String, CharacteristicValueObject> characteristicFromDatabaseWithValueUri ) {

        Collection<CharacteristicValueObject> characteristicsFromOntology = new HashSet<>();

        // in neurocarta we dont need to search all Ontologies
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

                    CharacteristicValueObject phenotype = new CharacteristicValueObject( ontologyTerm.getLabel()
                            .toLowerCase(), ontologyTerm.getUri() );

                    characteristicsFromOntology.add( phenotype );
                }
            }
        }

        return characteristicsFromOntology;
    }

    /**
     * @param c
     * @return
     */
    private String foundValueKey( Characteristic c ) {
        if ( c instanceof VocabCharacteristic ) {
            return ( ( VocabCharacteristic ) c ).getValueUri();
        }
        return c.getValue();
    }

    /**
     * Allow us to store gene information as a characteristic associated with our entities. This doesn't work so well
     * for non-ncbi genes.
     * 
     * @param g
     * @return
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

    /**
     * 
     */
    private synchronized void initializeCategoryTerms() {

        URL termUrl = OntologyServiceImpl.class.getResource( "/ubic/gemma/ontology/EFO.factor.categories.txt" );
        categoryterms = new ConcurrentHashSet<>();
        try (BufferedReader reader = new BufferedReader( new InputStreamReader( termUrl.openStream() ) );) {
            String line;
            boolean warned = false;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.startsWith( "#" ) || StringUtils.isEmpty( line ) ) continue;
                String[] f = StringUtils.split( line, '\t' );
                if ( f.length < 2 ) {
                    continue;
                }
                OntologyTerm t = getTerm( f[0] );
                if ( t == null ) {
                    // this is not great. We might want to let it expire and redo it later if the ontology
                    // becomes
                    // available. Inference will not be available.
                    if ( !warned ) {
                        log.info( "Ontology needed is not loaded? Using light-weight placeholder for " + f[0]
                                + " (further warnings hidden)" );

                        warned = true;
                    }
                    t = new OntologyTermSimple( f[0], f[1] );
                }

                categoryterms.add( t );
            }

        } catch ( IOException ioe ) {
            log.error( "Error reading from term list '" + termUrl + "'; returning general term list", ioe );
            categoryterms = null;
        }

        categoryterms = Collections.unmodifiableCollection( categoryterms );
    }

    /** given a collection of characteristics add them to the correct List */
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
            else if ( cha.getValue().toLowerCase().indexOf( searchQuery.toLowerCase() ) != -1 ) {
                characteristicsSubstring.add( cha );
            } else {
                characteristicsNoRuleFound.add( cha );
            }
        }
    }

    /**
     * Look for genes, but only for certain categoriUris (genotype, etc.)
     * 
     * @param queryString
     * @param categoryUri if null, this method doesn't do anything.
     * @param taxon okay if null, but then all matches returned.
     * @param searchResults added to this
     */
    private void searchForGenes( String queryString, String categoryUri, Taxon taxon,
            Collection<Characteristic> searchResults ) {
        if ( categoryUri == null ) return;

        // genotype, genetic modification, molecular entity.
        if ( categoryUri.equals( "http://www.ebi.ac.uk/efo/EFO_0000510" )
                || categoryUri.equals( "http://www.ebi.ac.uk/efo/EFO_0000513" )
                || categoryUri.equals( "http://purl.org/obo/owl/CHEBI#CHEBI_23367" ) ) {

            SearchSettings ss = SearchSettings.Factory.newInstance();
            ss.setQuery( queryString );
            ss.noSearches();
            ss.setTaxon( taxon );
            ss.setSearchGenes( true );
            Map<Class<?>, List<SearchResult>> geneResults = this.searchService.search( ss, true, false );

            if ( geneResults.containsKey( Gene.class ) ) {
                for ( SearchResult sr : geneResults.get( Gene.class ) ) {
                    Gene g = ( Gene ) sr.getResultObject();
                    log.debug( "Search for " + queryString + " returned: " + g );
                    searchResults.add( gene2Characteristic( g ) );
                }
            }
        }
    }

    /**
     * @param alreadyUsedResults items already in the system
     * @param searchResults
     * @param searchTerm the query
     * @param foundValues
     * @return
     */
    private Collection<Characteristic> sort( Collection<Characteristic> alreadyUsedResults,
            Collection<Characteristic> searchResults, String searchTerm, Collection<String> foundValues ) {

        /*
         * Organize the list into 3 parts. Want to get the exact match showing up on top But close matching
         * individualResults and alreadyUsedResults should get priority over jena's search results. Each result's order
         * should be preserved.
         */

        List<Characteristic> sortedResultsExact = new ArrayList<>();
        List<Characteristic> sortedResultsStartsWith = new ArrayList<>();
        List<Characteristic> sortedResultsBottom = new ArrayList<>();

        for ( Characteristic characteristic : alreadyUsedResults ) {
            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) ) {
                sortedResultsExact.add( characteristic );
            } else if ( characteristic.getValue().startsWith( searchTerm ) ) {
                sortedResultsStartsWith.add( characteristic );
            } else {
                sortedResultsBottom.add( characteristic );
            }
        }

        for ( Characteristic characteristic : searchResults ) {
            String key = foundValueKey( characteristic );
            if ( foundValues.contains( key ) ) continue;
            foundValues.add( key );
            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) ) {
                sortedResultsExact.add( characteristic );
            } else if ( characteristic.getValue().startsWith( searchTerm ) ) {
                sortedResultsStartsWith.add( characteristic );
            } else {
                sortedResultsBottom.add( characteristic );
            }
        }

        sort( sortedResultsExact );
        sort( sortedResultsStartsWith );
        sort( sortedResultsBottom );

        List<Characteristic> sortedTerms = new ArrayList<>( foundValues.size() );
        sortedTerms.addAll( sortedResultsExact );
        sortedTerms.addAll( sortedResultsStartsWith );
        sortedTerms.addAll( sortedResultsBottom );

        return sortedTerms;
    }

}