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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Has a static method for finding out which ontologies are loaded into the system and a general purpose find method
 * that delegates to the many ontology services
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="ontologyService"
 * @spring.property name="birnLexOntologyService" ref ="birnLexOntologyService"
 * @spring.property name="fmaOntologyService" ref ="fmaOntologyService"
 * @spring.property name="diseaseOntologyService" ref ="diseaseOntologyService"
 * @spring.property name="mgedOntologyService" ref ="mgedOntologyService"
 * @spring.property name="bioMaterialService" ref ="bioMaterialService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="characteristicService" ref="characteristicService"
 * @spring.property name="chebiOntologyService" ref="chebiOntologyService"
 */
public class OntologyService {

    private static final String USED = " -USED- ";

    private static Log log = LogFactory.getLog( OntologyService.class.getName() );

    private BirnLexOntologyService birnLexOntologyService;
    private HumanDiseaseOntologyService diseaseOntologyService;
    private FMAOntologyService fmaOntologyService;
    private MgedOntologyService mgedOntologyService;
    private ChebiOntologyService chebiOntologyService;
    private BioMaterialService bioMaterialService;
    private ExpressionExperimentService eeService;
    private Collection<AbstractOntologyService> ontologyServices = new HashSet<AbstractOntologyService>();

    private CharacteristicService characteristicService;

    /**
     * List the ontologies that are available in the jena database.
     * 
     * @return
     */
    public static Collection<ubic.gemma.ontology.Ontology> listAvailableOntologies() {

        Collection<ubic.gemma.ontology.Ontology> ontologies = new HashSet<ubic.gemma.ontology.Ontology>();
        ModelMaker maker = OntologyLoader.getRDBMaker();
        ExtendedIterator iterator = maker.listModels();
        while ( iterator.hasNext() ) {
            String name = ( String ) iterator.next();
            ExternalDatabase database = OntologyLoader.ontologyAsExternalDatabase( name );
            ubic.gemma.ontology.Ontology o = new ubic.gemma.ontology.Ontology( database );
            ontologies.add( o );
        }
        return ontologies;

    }

    /**
     * @return the OntologyTerm for the specified URI. Exposed for AJAX.
     */
    public OntologyTerm getTerm( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyTerm term = ontology.getTerm( uri );
            if ( term != null ) return term;
        }
        return null;
    }

    /**
     * @return the OntologyResource for the specified URI
     */
    public OntologyResource getResource( String uri ) {
        for ( AbstractOntologyService ontology : ontologyServices ) {
            OntologyResource resource = ontology.getResource( uri );
            if ( resource != null ) return resource;
        }
        return null;
    }

    /**
     * Given a collection of ontology terms converts them to a collection of VocabCharacteristics
     * 
     * @param terms
     * @param filterTerm
     * @return
     */
    private Collection<VocabCharacteristic> convert( final Collection<OntologyResource> resources ) {

        Collection<VocabCharacteristic> converted = new HashSet<VocabCharacteristic>();

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
     * Given a search string will look through the Mged, birnlex, obo Disease Ontology and FMA Ontology for terms that
     * match the search term. this a lucene backed search, is inexact and for general terms can return alot of results.
     * 
     * @param search
     * @return a collection of VocabCharacteristics that are backed by the corresponding found OntologyTerm
     */
    public Collection<VocabCharacteristic> findTermAsCharacteristic( String search ) {

        Collection<VocabCharacteristic> terms = new HashSet<VocabCharacteristic>();
        Collection<OntologyTerm> results;

        for ( AbstractOntologyService ontology : ontologyServices ) {
            results = ontology.findTerm( search );
            if ( results != null ) terms.addAll( convert( new HashSet<OntologyResource>( results ) ) );
        }

        return terms;
    }

    /**
     * Given a search string will look through the loaded ontologies for terms that match the search term. this a lucene
     * backed search, is inexact and for general terms can return a lot of results.
     * 
     * @param search
     * @return returns a collection of ontologyTerm's
     */
    public Collection<OntologyTerm> findTerms( String search ) {

        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyTerm> found = ontology.findTerm( search );
            if ( found != null ) results.addAll( found );
        }

        return results;
    }

    /**
     * @param search
     * @return
     */
    public Collection<OntologyIndividual> findIndividuals( String search ) {

        Collection<OntologyIndividual> results = new HashSet<OntologyIndividual>();

        for ( AbstractOntologyService ontology : ontologyServices ) {
            Collection<OntologyIndividual> found = ontology.findIndividuals( search );
            if ( found != null ) results.addAll( found );
        }

        return results;
    }

    /**
     * Given a collection of ontology terms will filter out all the terms that don't have the filter term in their
     * label.
     * 
     * @param terms
     * @param filterTerm
     * @return
     */
    private Collection<VocabCharacteristic> filter( final Collection<OntologyResource> terms, final String filter ) {

        Collection<VocabCharacteristic> filtered = new HashSet<VocabCharacteristic>();

        if ( ( terms == null ) || ( terms.isEmpty() ) ) return filtered;

        String caseInsensitiveFilter = filter.toLowerCase();

        for ( OntologyResource res : terms ) {
            if ( StringUtils.isNotEmpty( res.getLabel() )
                    && res.getLabel().toLowerCase().startsWith( caseInsensitiveFilter ) ) {
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
                }

                filtered.add( vc );
            }
        }
        log.debug( "returning " + filtered.size() + " terms after filter" );

        return filtered;
    }

    /**
     * Given a search string will first look through the characterisc database for any entries that have a match. If a
     * ontologyTermURI is given it will add all the individuals from that URI that match the search term criteria to the
     * returned list also. Then will search the birnlex, obo Disease Ontology and FMA Ontology for OntologyResources
     * (Terms and Individuals) that match the search term exactly
     * 
     * @param queryString
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<Characteristic> findExactTerm( String queryString, String categoryUri ) {

        StopWatch watch = new StopWatch();
        watch.start();
        log.debug( "starting findExactTerm for " + queryString + ". Timining information begins from here" );

        if ( queryString == null ) return null;

        // TODO: this is poorly named. changed to findExactResource, add findExactIndividual Factor out common code

        Collection<OntologyResource> results;

        // Add the matching individuals
        List<Characteristic> individualResults = new ArrayList<Characteristic>();
        if ( StringUtils.isNotBlank( categoryUri ) && !categoryUri.equals( "{}" ) ) {
            results = new HashSet<OntologyResource>( mgedOntologyService.getTermIndividuals( categoryUri ) );
            if ( results.size() > 0 ) individualResults.addAll( filter( results, queryString ) );
        }
        log.debug( "found " + individualResults.size() + " individuals from ontology term " + categoryUri + " in "
                + watch.getTime() + " ms" );

        List<Characteristic> alreadyUsedResults = new ArrayList<Characteristic>();
        Collection<Characteristic> foundChars = characteristicService.findByValue( queryString );

        // remove duplicates, don't want to redefine == operator for Characteristics
        // for this use consider if the value = then its a duplicate.
        Collection<String> foundValues = new HashSet<String>();
        if ( foundChars != null ) {
            for ( Characteristic characteristic : foundChars ) {
                if ( !foundValues.contains( foundValueKey( characteristic ) ) ) {
                    // Want to flag in the web interface that these are alrady used by Gemma
                    // Didn't want to make a characteristic value object just to hold a boolean flag for used....
                    characteristic.setDescription( USED + characteristic.getDescription() );
                    alreadyUsedResults.add( characteristic );
                    foundValues.add( foundValueKey( characteristic ) );
                }
            }
        }
        log.debug( "found " + alreadyUsedResults.size() + " matching characteristics used in the database" + " in "
                + watch.getTime() + " ms" );

        List<Characteristic> searchResults = new ArrayList<Characteristic>();

        // FIXME hard-coding of ontologies to search
        results = mgedOntologyService.findResources( queryString );
        log.debug( "found " + results.size() + " terms from mged in " + watch.getTime() + " ms" );
        searchResults.addAll( filter( results, queryString ) );

        results = birnLexOntologyService.findResources( queryString );
        log.debug( "found " + results.size() + " terms from birnLex in " + watch.getTime() + " ms" );
        searchResults.addAll( filter( results, queryString ) );

        results = diseaseOntologyService.findResources( queryString );
        log.debug( "found " + results.size() + " terms from obo in " + watch.getTime() + " ms" );
        searchResults.addAll( filter( results, queryString ) );

        results = fmaOntologyService.findResources( queryString );
        log.debug( "found " + results.size() + " terms from fma in " + watch.getTime() + " ms" );
        searchResults.addAll( filter( results, queryString ) );

        results = chebiOntologyService.findResources( queryString );
        log.debug( "found " + results.size() + " terms from chebi in " + watch.getTime() + " ms" );
        searchResults.addAll( filter( results, queryString ) );

        // Sort the individual results.
        Collection<Characteristic> sortedResults = sort( individualResults, alreadyUsedResults, searchResults,
                queryString, foundValues );
        log.debug( "sorted " + sortedResults.size() + " in " + watch.getTime() + " ms" );

        return sortedResults;

    }

    private String foundValueKey( Characteristic c ) {
        StringBuffer buf = new StringBuffer( c.getValue() );
        if ( c instanceof VocabCharacteristic ) buf.append( ( ( VocabCharacteristic ) c ).getValueUri() );
        return buf.toString();
    }

    private Collection<Characteristic> sort( List<Characteristic> individualResults,
            List<Characteristic> alreadyUsedResults, List<Characteristic> searchResults, String searchTerm,
            Collection<String> foundValues ) {

        // Organize the list into 3 parts.
        // Want to get the exact match showing up on top
        // But close matching individualResults and alreadyUsedResults should get
        // priority over jena's search results.
        // Each reasults shoulds order should be preserved.

        List<Characteristic> sortedResultsExact = new ArrayList<Characteristic>();
        List<Characteristic> sortedResultsStartsWith = new ArrayList<Characteristic>();
        List<Characteristic> sortedResultsBottem = new ArrayList<Characteristic>();

        for ( Characteristic characteristic : alreadyUsedResults ) {
            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) )
                sortedResultsExact.add( characteristic );
            else if ( characteristic.getValue().startsWith( searchTerm ) )
                sortedResultsStartsWith.add( characteristic );
            else
                sortedResultsBottem.add( characteristic );
        }

        for ( Characteristic characteristic : individualResults ) {
            String key = foundValueKey( characteristic );
            if ( foundValues.contains( key ) ) continue;
            foundValues.add( key );
            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) )
                sortedResultsExact.add( characteristic );
            else if ( characteristic.getValue().startsWith( searchTerm ) )
                sortedResultsStartsWith.add( characteristic );
            else
                sortedResultsBottem.add( characteristic );
        }

        for ( Characteristic characteristic : searchResults ) {
            String key = foundValueKey( characteristic );
            if ( foundValues.contains( key ) ) continue;
            foundValues.add( key );
            if ( characteristic.getValue().equalsIgnoreCase( searchTerm ) )
                sortedResultsExact.add( characteristic );
            else if ( characteristic.getValue().startsWith( searchTerm ) )
                sortedResultsStartsWith.add( characteristic );
            else
                sortedResultsBottem.add( characteristic );
        }

        // Collections.sort( sortedResultsExact, compare );
        // Collections.reverse( sortedResultsExact );

        Collection<Characteristic> sortedTerms = new ArrayList<Characteristic>( foundValues.size() );
        sortedTerms.addAll( sortedResultsExact );
        sortedTerms.addAll( sortedResultsStartsWith );
        sortedTerms.addAll( sortedResultsBottem );

        return sortedTerms;
    }

    // Private class for sorting Characteristics
    class TermComparator implements Comparator {

        String comparator;

        public TermComparator( String comparator ) {
            super();
            this.comparator = comparator;
        }

        public int compare( Object o1, Object o2 ) {
            String term1 = ( ( Characteristic ) o1 ).getValue();
            String term2 = ( ( Characteristic ) o2 ).getValue();

            if ( term1.equals( term2 ) ) return 0;

            if ( term1.equals( comparator ) ) return 1;

            if ( term2.equals( comparator ) ) return -1;

            if ( term1.startsWith( comparator ) ) {
                if ( term2.startsWith( comparator ) )
                    return 0;
                else
                    return 1;
            } else if ( term2.startsWith( comparator ) ) {
                return -1;
            }

            return 0;

        }
    }

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list. Exposed for AJAX.
     * 
     * @param vc
     * @param bioMaterialIdList
     */
    @SuppressWarnings("unchecked")
    public void saveBioMaterialStatement( Characteristic vc, Collection<Long> bioMaterialIdList ) {

        log.debug( "Vocab Characteristic: " + vc );
        log.debug( "Biomaterial ID List: " + bioMaterialIdList );

        vc.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( vc );
        Collection<BioMaterial> biomaterials = bioMaterialService.loadMultiple( bioMaterialIdList );

        for ( BioMaterial bioM : biomaterials ) {

            Collection<Characteristic> current = bioM.getCharacteristics();
            if ( current == null )
                current = new HashSet<Characteristic>( chars );
            else
                current.addAll( chars );

            bioM.setCharacteristics( current );
            bioMaterialService.update( bioM );

        }

    }

    /**
     * Will persist the give vocab characteristic to each expression experiment id supplied in the list.
     * 
     * @param vc
     * @param bmIdList
     */
    @SuppressWarnings("unchecked")
    public void saveExpressionExperimentStatement( Characteristic vc, Collection<Long> bmIdList ) {

        log.debug( "Vocab Characteristic: " + vc );
        log.debug( "Expression Experiment ID List: " + bmIdList );

        vc.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( vc );
        Collection<ExpressionExperiment> ees = eeService.loadMultiple( bmIdList );

        for ( ExpressionExperiment ee : ees ) {
            eeService.thawLite( ee );
            Collection<Characteristic> current = ee.getCharacteristics();
            if ( current == null )
                current = new HashSet<Characteristic>( chars );
            else
                current.addAll( chars );

            ee.setCharacteristics( current );
            eeService.update( ee );

        }
    }

    /**
     * Will persist the give vocab characteristic to each expression experiment id supplied in the list.
     * 
     * @param vc
     * @param bmIdList
     */
    @SuppressWarnings("unchecked")
    public void removeExpressionExperimentStatement( Collection<Long> characterIds, Collection<Long> eeIdList ) {

        log.debug( "Vocab Characteristic: " + characterIds );
        log.debug( "Expression Experiment ID List: " + eeIdList );

        Collection<ExpressionExperiment> ees = eeService.loadMultiple( eeIdList );

        for ( ExpressionExperiment ee : ees ) {
            eeService.thawLite( ee );
            Collection<Characteristic> current = ee.getCharacteristics();
            if ( current == null ) continue;

            Collection<Characteristic> found = new HashSet<Characteristic>();

            for ( Characteristic characteristic : current ) {
                if ( characterIds.contains( characteristic.getId() ) ) found.add( characteristic );

            }
            if ( found.size() == 0 ) continue;

            current.removeAll( found );
            ee.setCharacteristics( current );
            eeService.update( ee );

        }

        for ( Long id : characterIds ) {
            characteristicService.delete( id );
        }
    }

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list.
     * 
     * @param vc
     * @param bmIdList
     */
    @SuppressWarnings("unchecked")
    public void removeBioMaterialStatement( Collection<Long> characterIds, Collection<Long> bmIdList ) {

        log.debug( "Vocab Characteristic: " + characterIds );
        log.debug( "biomaterial ID List: " + bmIdList );

        Collection<BioMaterial> bms = bioMaterialService.loadMultiple( bmIdList );

        for ( BioMaterial bm : bms ) {

            Collection<Characteristic> current = bm.getCharacteristics();
            if ( current == null ) continue;

            Collection<Characteristic> found = new HashSet<Characteristic>();

            for ( Characteristic characteristic : current ) {
                if ( characterIds.contains( characteristic.getId() ) ) found.add( characteristic );

            }
            if ( found.size() == 0 ) continue;

            current.removeAll( found );
            bm.setCharacteristics( current );
            bioMaterialService.update( bm );

        }

        for ( Long id : characterIds ) {
            characteristicService.delete( id );
        }
    }

    /**
     * @param birnLexOntologyService the birnLexOntologyService to set
     */
    public void setBirnLexOntologyService( BirnLexOntologyService birnLexOntologyService ) {
        this.birnLexOntologyService = birnLexOntologyService;
        ontologyServices.add( birnLexOntologyService );
    }

    /**
     * @param fmaOntologyService the fmaOntologyService to set
     */
    public void setFmaOntologyService( FMAOntologyService fmaOntologyService ) {
        this.fmaOntologyService = fmaOntologyService;
        ontologyServices.add( fmaOntologyService );
    }

    /**
     * @param diseaseOntologyService the diseaseOntologyService to set
     */
    public void setDiseaseOntologyService( HumanDiseaseOntologyService diseaseOntologyService ) {
        this.diseaseOntologyService = diseaseOntologyService;
        ontologyServices.add( diseaseOntologyService );
    }

    /**
     * @param mgedDiseaseOntologyService the mgedDiseaseOntologyService to set
     */
    public void setMgedOntologyService( MgedOntologyService mgedOntologyService ) {
        this.mgedOntologyService = mgedOntologyService;
        ontologyServices.add( mgedOntologyService );
    }

    /**
     * @param chebiOntologyService the chebiOntologyService to set
     */
    public void setChebiOntologyService( ChebiOntologyService chebiOntologyService ) {
        this.chebiOntologyService = chebiOntologyService;
        ontologyServices.add( chebiOntologyService );
    }

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.eeService = expressionExperimentService;
    }

    /**
     * @param characteristicService the characteristicService to set
     */
    public void setCharacteristicService( CharacteristicService characteristicService ) {
        this.characteristicService = characteristicService;
    }

}