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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.StringUtil;
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
 * @spring.property name="oboDiseaseOntologyService" ref ="oboDiseaseOntologyService"
 * @spring.property name="mgedOntologyService" ref ="mgedOntologyService"
 * @spring.property name="bioMaterialService" ref ="bioMaterialService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="characteristicService" ref="characteristicService"
 */

public class OntologyService {

    private static Log log = LogFactory.getLog( OntologyService.class.getName() );

    private BirnLexOntologyService birnLexOntologyService;
    private OBODiseaseOntologyService oboDiseaseOntologyService;
    private FMAOntologyService fmaOntologyService;
    private MgedOntologyService mgedOntologyService;
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
     * @return the OntologyTerm for the specified URI
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
            }

            converted.add( vc );
        }

        return converted;
    }

    /**
     * Given a search string will look through the birnlex, obo Disease Ontology and FMA Ontology for terms that match
     * the search term. this a lucene backed search, is inexact and for general terms can return alot of results.
     * 
     * @param search
     * @return
     */
    public Collection<VocabCharacteristic> findTerm( String search ) {

        Collection<VocabCharacteristic> terms = new HashSet<VocabCharacteristic>();
        Collection<OntologyTerm> results;

        for ( AbstractOntologyService ontology : ontologyServices ) {
            results = ontology.findTerm( search );
            if ( results != null ) terms.addAll( convert( new HashSet<OntologyResource>( results ) ) );
        }

        return terms;
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

        for ( OntologyResource res : terms ) {
            if ( ( res.getUri() != null ) && ( StringUtils.isNotEmpty( res.getUri() ) )
                    && ( res.getLabel().startsWith( filter ) ) ) {
                VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
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
                }

                filtered.add( vc );
            }
        }

        return filtered;
    }

    /**
     * Given a search string will first look through the characterisc database for any entries that have a match. If a
     * ontologyTermURI is given it will add all the individuals from that URI that match the search term criteria to the
     * returned list also. Then will search the birnlex, obo Disease Ontology and FMA Ontology for OntologyResources
     * (Terms and Individuals) that match the search term exactly
     * 
     * @param search
     * @return
     */
    public Collection<Characteristic> findExactTerm( String search, String categoryUri ) {

        if ( search == null ) return null;

        // TODO: this is poorly named. changed to findExactResource, add findExactIndividual Factor out common code
        Collection<Characteristic> terms = new HashSet<Characteristic>();

        Collection<OntologyResource> results;

        // Add the matching individuals 1st
        if ( categoryUri != null ) {
            results = new HashSet<OntologyResource>( mgedOntologyService.getTermIndividuals( categoryUri ) );
            if ( results != null ) terms.addAll( convert( results ) );
        }
        
        Collection<String> foundValues = new HashSet<String>();
        Collection<Characteristic> foundChars = characteristicService.findByValue( search );

        // remove duplicates, don't want to redefine == operator for Characteristics
        // for this use consider if the value = then its a duplicate.
        if ( foundChars != null ) {
            for ( Characteristic characteristic : foundChars ) {
                if ( !foundValues.contains( characteristic.getValue().toLowerCase() ) ) {
                    terms.add( characteristic );
                    foundValues.add( characteristic.getValue().toLowerCase() );
                }
            }
        }

        results = birnLexOntologyService.findResources( search );
        if ( results != null ) terms.addAll( filter( results, search ) );

        results = oboDiseaseOntologyService.findResources( search );
        if ( results != null ) terms.addAll( filter( results, search ) );

        results = fmaOntologyService.findResources( search );
        if ( results != null ) terms.addAll( filter( results, search ) );

        return terms;
    }

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list
     * 
     * @param vc
     * @param bioMaterialIdList
     */
    public void saveBioMaterialStatement( VocabCharacteristic vc, Collection<Long> bioMaterialIdList ) {

        log.debug( "Vocab Characteristic: " + vc.getDescription() );
        log.debug( "Biomaterial ID List: " + bioMaterialIdList );

        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( ( Characteristic ) vc );
        Collection<BioMaterial> biomaterials = bioMaterialService.loadMultiple( bioMaterialIdList );

        for ( BioMaterial bioM : biomaterials ) {
            bioM.setCharacteristics( chars );
            bioMaterialService.update( bioM );

        }

    }

    /**
     * Will persist the give vocab characteristic to each expression experiment id supplied in the list
     * 
     * @param vc
     * @param eeIdList
     */
    public void saveExpressionExperimentStatment( VocabCharacteristic vc, Collection<Long> eeIdList ) {

        log.info( "Vocab Characteristic: " + vc.getDescription() );
        log.info( "Expression Experiment ID List: " + eeIdList );

        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( ( Characteristic ) vc );
        Collection<ExpressionExperiment> ees = eeService.loadMultiple( eeIdList );

        for ( ExpressionExperiment ee : ees ) {

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
     * @param oboDiseaseOntologyService the oboDiseaseOntologyService to set
     */
    public void setOboDiseaseOntologyService( OBODiseaseOntologyService oboDiseaseOntologyService ) {
        this.oboDiseaseOntologyService = oboDiseaseOntologyService;
        ontologyServices.add( oboDiseaseOntologyService );
    }

    /**
     * @param mgedDiseaseOntologyService the mgedDiseaseOntologyService to set
     */
    public void setMgedOntologyService( MgedOntologyService mgedOntologyService ) {
        this.mgedOntologyService = mgedOntologyService;
        ontologyServices.add( mgedOntologyService );
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