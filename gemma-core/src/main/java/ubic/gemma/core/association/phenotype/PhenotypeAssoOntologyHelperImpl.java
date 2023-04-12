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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
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
public class PhenotypeAssoOntologyHelperImpl implements PhenotypeAssoOntologyHelper {

    private static final Log log = LogFactory.getLog( PhenotypeAssoOntologyHelperImpl.class );

    private final OntologyService ontologyService;

    private final List<ubic.basecode.ontology.providers.OntologyService> ontologies;

    @Autowired
    public PhenotypeAssoOntologyHelperImpl( OntologyService ontologyService, DiseaseOntologyService diseaseOntologyService, MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService, HumanPhenotypeOntologyService humanPhenotypeOntologyService ) {
        this.ontologyService = ontologyService;
        //  We add them even when they aren't available so we can use unit tests that mock or fake the ontologies.
        this.ontologies = Arrays.asList( diseaseOntologyService, mammalianPhenotypeOntologyService, humanPhenotypeOntologyService );
        if ( !diseaseOntologyService.isEnabled() ) {
            log.debug( "DO is not enabled, phenotype tools will not work correctly" );
        }
        if ( !mammalianPhenotypeOntologyService.isEnabled() ) {
            log.debug( "MPO is not enabled, phenotype tools will not work correctly" );
        }
        if ( !humanPhenotypeOntologyService.isEnabled() ) {
            log.debug( "HPO is not enabled, phenotype tools will not work correctly" );
        }
    }

    @Override
    public boolean areOntologiesAllLoaded() {
        // if these ontologies are not configured, we will never be ready. Check for valid configuration.
        return ontologies.stream().allMatch( ubic.basecode.ontology.providers.OntologyService::isOntologyLoaded );
    }

    @Override
    public Characteristic characteristicValueObject2Characteristic(
            CharacteristicValueObject characteristicValueObject ) {

        Characteristic characteristic = Characteristic.Factory.newInstance();
        characteristic.setCategory( characteristicValueObject.getCategory() );
        characteristic.setCategoryUri( characteristicValueObject.getCategoryUri() );
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
    public OntologyTerm findOntologyTermByUri( String valueUri ) throws EntityNotFoundException {

        if ( valueUri == null || valueUri.isEmpty() ) {
            throw new IllegalArgumentException( "URI to load was blank." );
        }

        OntologyTerm ontologyTerm;
        for ( ubic.basecode.ontology.providers.OntologyService ontology : this.ontologies ) {
            ontologyTerm = ontology.getTerm( valueUri );
            if ( ontologyTerm != null )
                return ontologyTerm;
        }

        throw new EntityNotFoundException( valueUri + " - term not found" );
    }

    @Override
    public Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) throws OntologySearchException {
        Map<String, OntologyTerm> uniqueValueTerm = new HashMap<>();

        for ( ubic.basecode.ontology.providers.OntologyService ontology : this.ontologies ) {
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
        for ( ubic.basecode.ontology.providers.OntologyService ontology : this.ontologies ) {
            if ( ontology.isOntologyLoaded() ) {
                Collection<OntologyTerm> found = ontology.findTerm( searchQuery );
                if ( found != null && !found.isEmpty() )
                    results.addAll( found );
            }
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
