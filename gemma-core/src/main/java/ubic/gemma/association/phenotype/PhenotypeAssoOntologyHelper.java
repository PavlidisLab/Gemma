/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.association.phenotype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.gemma.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;

/**
 * @author nicolas
 * @version $Id$
 */
public class PhenotypeAssoOntologyHelper {

    private List<AbstractOntologyService> ontologies = new ArrayList<AbstractOntologyService>();
    private OntologyService ontologyService = null;

    /** used to set the Ontology terms */
    public PhenotypeAssoOntologyHelper( OntologyService ontologyService ) throws Exception {
        this.ontologyService = ontologyService;
        AbstractOntologyService diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        if ( diseaseOntologyService != null ) {
            this.ontologies.add( diseaseOntologyService );
        }
        AbstractOntologyService mammalianPhenotypeOntologyService = ontologyService
                .getMammalianPhenotypeOntologyService();
        if ( mammalianPhenotypeOntologyService != null ) {
            this.ontologies.add( mammalianPhenotypeOntologyService );
        }
        AbstractOntologyService humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();
        if ( humanPhenotypeOntologyService != null ) {
            this.ontologies.add( humanPhenotypeOntologyService );
        }
    }

    /** search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO */
    public Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) {

        HashMap<String, OntologyTerm> uniqueValueTerm = new HashMap<String, OntologyTerm>();

        for ( AbstractOntologyService ontology : this.ontologies ) {
            Collection<OntologyTerm> hits = ontology.findTerm( searchQuery );

            for ( OntologyTerm ontologyTerm : hits ) {
                if ( uniqueValueTerm.get( ontologyTerm.getLabel().toLowerCase() ) == null ) {
                    uniqueValueTerm.put( ontologyTerm.getLabel().toLowerCase(), ontologyTerm );
                }
            }
        }

        return ontology2CharacteristicValueObject( uniqueValueTerm.values() );
    }

    /** search the disease, hp and mp ontology for OntologyTerm */
    public Collection<OntologyTerm> findValueUriInOntology( String searchQuery ) {

        Collection<OntologyTerm> ontologyFound = new TreeSet<OntologyTerm>();
        for ( AbstractOntologyService ontology : this.ontologies ) {
            ontologyFound.addAll( ontology.findTerm( searchQuery ) );
        }

        return ontologyFound;
    }

    /** Giving some Ontology terms return all valueUri of Ontology Terms + children */
    public Set<String> findAllChildrenAndParent( Collection<OntologyTerm> ontologyTerms ) {

        Set<String> phenotypesFoundAndChildren = new HashSet<String>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {
            // add the parent term found
            phenotypesFoundAndChildren.add( ontologyTerm.getUri() );

            // add all children of the term
            for ( OntologyTerm ontologyTermChildren : ontologyTerm.getChildren( false ) ) {
                phenotypesFoundAndChildren.add( ontologyTermChildren.getUri() );
            }
        }
        return phenotypesFoundAndChildren;
    }

    /** For a valueUri return the OntologyTerm found */
    public OntologyTerm findOntologyTermByUri( String valueUri ) {

        if ( valueUri.isEmpty() ) {
            throw new IllegalArgumentException( "URI to load was blank." );
        }

        OntologyTerm ontologyTerm = null;
        for ( AbstractOntologyService ontology : this.ontologies ) {
            ontologyTerm = ontology.getTerm( valueUri );
            if ( ontologyTerm != null ) return ontologyTerm;
        }

        throw new EntityNotFoundException( valueUri );
    }

    /** For a valueUri return the Characteristic (represents a phenotype) */
    public Characteristic valueUri2Characteristic( String valueUri ) {

        OntologyTerm o = findOntologyTermByUri( valueUri );

        VocabCharacteristic myPhenotype = VocabCharacteristic.Factory.newInstance();

        myPhenotype.setValueUri( o.getUri() );
        myPhenotype.setValue( o.getLabel() );
        myPhenotype.setCategory( PhenotypeAssociationConstants.PHENOTYPE );
        myPhenotype.setCategoryUri( PhenotypeAssociationConstants.PHENOTYPE_CATEGORY_URI );

        return myPhenotype;
    }

    /** Ontology term to CharacteristicValueObject */
    private Set<CharacteristicValueObject> ontology2CharacteristicValueObject( Collection<OntologyTerm> ontologyTerms ) {

        Set<CharacteristicValueObject> characteristicsVO = new HashSet<CharacteristicValueObject>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {
            CharacteristicValueObject phenotype = new CharacteristicValueObject( ontologyTerm.getLabel().toLowerCase(),
                    ontologyTerm.getUri() );
            characteristicsVO.add( phenotype );
        }
        return characteristicsVO;
    }

    /** CharacteristicValueObject to Characteristic with no valueUri given */
    public VocabCharacteristic characteristicValueObject2Characteristic(
            CharacteristicValueObject characteristicValueObject ) {

        VocabCharacteristic characteristic = VocabCharacteristic.Factory.newInstance();
        characteristic.setCategory( characteristicValueObject.getCategory() );
        characteristic.setCategoryUri( characteristicValueObject.getCategoryUri() );
        characteristic.setValue( characteristicValueObject.getValue() );

        if ( characteristic.getValueUri() != null && !characteristic.getValueUri().equals( "" ) ) {
            characteristic.setValueUri( characteristicValueObject.getValueUri() );
        } else {

            // format the query for lucene to look for ontology terms with an exact match for the value
            String value = "\"" + StringUtils.join( characteristicValueObject.getValue().trim().split( " " ), " AND " )
                    + "\"";

            Collection<OntologyTerm> ontologyTerms = this.ontologyService.findTerms( value );

            for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                if ( ontologyTerm.getLabel().equalsIgnoreCase( characteristicValueObject.getValue() ) ) {
                    characteristic.setValueUri( ontologyTerm.getUri() );
                    break;
                }
            }
        }
        return characteristic;
    }
}
