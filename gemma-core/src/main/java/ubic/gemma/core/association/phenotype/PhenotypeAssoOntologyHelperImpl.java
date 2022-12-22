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
package ubic.gemma.core.association.phenotype;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.*;

/**
 * @author nicolas
 */
@Component
public class PhenotypeAssoOntologyHelperImpl implements InitializingBean, PhenotypeAssoOntologyHelper {

    private static final Log log = LogFactory.getLog( PhenotypeAssoOntologyHelperImpl.class );

    private final List<AbstractOntologyService> ontologies = new ArrayList<>();

    private final OntologyService ontologyService;

    @Autowired
    public PhenotypeAssoOntologyHelperImpl( OntologyService ontologyService ) {
        this.ontologyService = ontologyService;
    }

    @Override
    public void afterPropertiesSet() {

        /*
         * We add them even when they aren't available so we can use unit tests that mock or fake the ontologies.
         */

        DiseaseOntologyService diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        this.ontologies.add( diseaseOntologyService );
        if ( !diseaseOntologyService.isEnabled() ) {
            log.debug( "DO is not enabled, phenotype tools will not work correctly" );
        }

        MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = ontologyService
                .getMammalianPhenotypeOntologyService();
        this.ontologies.add( mammalianPhenotypeOntologyService );
        if ( !mammalianPhenotypeOntologyService.isEnabled() ) {
            log.debug( "MPO is not enabled, phenotype tools will not work correctly" );
        }

        HumanPhenotypeOntologyService humanPhenotypeOntologyService = ontologyService
                .getHumanPhenotypeOntologyService();
        this.ontologies.add( humanPhenotypeOntologyService );

        if ( !humanPhenotypeOntologyService.isEnabled() ) {
            log.debug( "HPO is not enabled, phenotype tools will not work correctly" );
        }
    }

    @Override
    public boolean areOntologiesAllLoaded() {
        /*
         * if these ontologies are not configured, we will never be ready. Check for valid configuration.
         */
        if ( !this.ontologyService.getDiseaseOntologyService().isEnabled() ) {
            log.warn( "DO is not enabled" );
            return false;
        } else if ( !this.ontologyService.getDiseaseOntologyService().isOntologyLoaded() ) {
            log.warn( "DO not loaded" );
            return false;
        }

        if ( !this.ontologyService.getHumanPhenotypeOntologyService().isEnabled() ) {
            log.warn( "HPO is not enabled" );
            return false;
        } else if ( !this.ontologyService.getHumanPhenotypeOntologyService().isOntologyLoaded() ) {
            log.warn( "HPO not loaded" );
            return false;
        }

        if ( !this.ontologyService.getMammalianPhenotypeOntologyService().isEnabled() ) {
            log.warn( "MPO is not enabled" );
            return false;
        } else if ( !this.ontologyService.getMammalianPhenotypeOntologyService().isOntologyLoaded() ) {
            log.warn( "MPO not loaded" );
            return false;
        }

        return true;
    }

    @Override
    public Characteristic characteristicValueObject2Characteristic(
            CharacteristicValueObject characteristicValueObject ) {

        Characteristic characteristic = Characteristic.Factory.newInstance();
        characteristic.setCategory( characteristicValueObject.getCategory() );
        characteristic.setCategoryUri( StringUtils.stripToNull( characteristicValueObject.getCategoryUri() ) );
        characteristic.setValue( characteristicValueObject.getValue() );

        if ( StringUtils.isNotBlank( characteristicValueObject.getValueUri() ) ) {
            characteristic.setValueUri( characteristicValueObject.getValueUri() );
        } else {

            // format the query for lucene to look for ontology terms with an exact match for the value
            String value = "\"" + StringUtils.join( characteristicValueObject.getValue().trim().split( " " ), " AND " ) + "\"";

            Collection<OntologyTerm> ontologyTerms = null;
            try {
                ontologyTerms = this.ontologyService.findTerms( value );
                for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                    if ( ontologyTerm.getLabel().equalsIgnoreCase( characteristicValueObject.getValue() ) ) {
                        characteristic.setValueUri( ontologyTerm.getUri() );
                        break;
                    }
                }
            } catch ( OntologySearchException e ) {
                log.error( "Failed to retrieve ontology terms for " + value + " when converting to VO. The value URI will not be set.", e );
            }

        }
        return characteristic;
    }

    @Override
    public Set<String> findAllChildrenAndParent( Collection<OntologyTerm> ontologyTerms ) {

        Set<String> phenotypesFoundAndChildren = new HashSet<>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {
            // add the parent term found
            assert ontologyTerm.getUri() != null;
            phenotypesFoundAndChildren.add( ontologyTerm.getUri() );

            // add all children of the term
            for ( OntologyTerm ontologyTermChildren : ontologyTerm.getChildren( false ) ) {
                assert ontologyTermChildren.getUri() != null;
                phenotypesFoundAndChildren.add( ontologyTermChildren.getUri() );
            }
        }
        return phenotypesFoundAndChildren;
    }

    @Override
    public OntologyTerm findOntologyTermByUri( String valueUri ) throws EntityNotFoundException {

        if ( valueUri == null || valueUri.isEmpty() ) {
            throw new IllegalArgumentException( "URI to load was blank." );
        }

        OntologyTerm ontologyTerm;
        for ( AbstractOntologyService ontology : this.ontologies ) {
            ontologyTerm = ontology.getTerm( valueUri );
            if ( ontologyTerm != null )
                return ontologyTerm;
        }

        throw new EntityNotFoundException( valueUri + " - term not found" );
    }

    @Override
    public Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) throws OntologySearchException {
        Map<String, OntologyTerm> uniqueValueTerm = new HashMap<>();

        for ( AbstractOntologyService ontology : this.ontologies ) {
            Collection<OntologyTerm> hits = ontology.findTerm( searchQuery );

            for ( OntologyTerm ontologyTerm : hits ) {
                if ( ontologyTerm.getLabel() != null && uniqueValueTerm.get( ontologyTerm.getLabel().toLowerCase() ) == null ) {
                    uniqueValueTerm.put( ontologyTerm.getLabel().toLowerCase(), ontologyTerm );
                }
            }
        }

        return ontology2CharacteristicValueObject( uniqueValueTerm.values() );
    }

    @Override
    public Collection<OntologyTerm> findValueUriInOntology( String searchQuery ) throws OntologySearchException {

        Collection<OntologyTerm> results = new TreeSet<>();
        for ( AbstractOntologyService ontology : this.ontologies ) {
            assert ontology != null;
            Collection<OntologyTerm> found = ontology.findTerm( searchQuery );
            if ( found != null && !found.isEmpty() )
                results.addAll( found );
        }

        return results;
    }

    @Override
    public Characteristic valueUri2Characteristic( String valueUri ) {

        try {
            OntologyTerm o = findOntologyTermByUri( valueUri );
            if ( o == null )
                return null;
            Characteristic myPhenotype = Characteristic.Factory.newInstance();
            myPhenotype.setValueUri( o.getUri() );
            myPhenotype.setValue( o.getLabel() );
            myPhenotype.setCategory( PhenotypeAssociationConstants.PHENOTYPE );
            myPhenotype.setCategoryUri( PhenotypeAssociationConstants.PHENOTYPE_CATEGORY_URI );
            return myPhenotype;
        } catch ( EntityNotFoundException e ) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Ontology term to CharacteristicValueObject
     *
     * @return collection of the input, converted to 'shell' CharacteristicValueObjects (which have other slots we want
     *         to use)
     */
    private Set<CharacteristicValueObject> ontology2CharacteristicValueObject(
            Collection<OntologyTerm> ontologyTerms ) {

        Set<CharacteristicValueObject> characteristicsVO = new HashSet<>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {
            CharacteristicValueObject phenotype = new CharacteristicValueObject( -1L,
                    ontologyTerm.getLabel().toLowerCase(), ontologyTerm.getUri() );
            characteristicsVO.add( phenotype );
        }
        return characteristicsVO;
    }
}
