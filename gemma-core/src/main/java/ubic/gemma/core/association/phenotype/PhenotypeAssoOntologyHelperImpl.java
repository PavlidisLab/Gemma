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
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.search.BaseCodeOntologySearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.*;

/**
 * @author nicolas
 */
@Component
@Deprecated
public class PhenotypeAssoOntologyHelperImpl implements PhenotypeAssoOntologyHelper {

    private static final Log log = LogFactory.getLog( PhenotypeAssoOntologyHelperImpl.class );

    private final OntologyService ontologyService;

    private final List<ubic.basecode.ontology.providers.OntologyService> ontologies;

    @Autowired
    public PhenotypeAssoOntologyHelperImpl( OntologyService ontologyService, MondoOntologyService diseaseOntologyService, MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService, HumanPhenotypeOntologyService humanPhenotypeOntologyService ) {
        this.ontologyService = ontologyService;
        this.ontologies = Collections.unmodifiableList( Arrays.asList( diseaseOntologyService, mammalianPhenotypeOntologyService, humanPhenotypeOntologyService ) );
    }

    @Override
    public Collection<ubic.basecode.ontology.providers.OntologyService> getOntologyServices() {
        return ontologies;
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

            Collection<OntologyTerm> ontologyTerms;
            try {
                ontologyTerms = this.ontologyService.findTerms( value );
                for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                    if ( StringUtils.equalsIgnoreCase( ontologyTerm.getLabel(), characteristicValueObject.getValue() ) ) {
                        characteristic.setValueUri( ontologyTerm.getUri() );
                        break;
                    }
                }
            } catch ( SearchException e ) {
                log.error( "Failed to retrieve ontology terms for " + value + " when converting to VO. The value URI will not be set.", e );
            }

        }
        return characteristic;
    }

    @Override
    public OntologyTerm findOntologyTermByUri( String valueUri ) {

        if ( valueUri == null || valueUri.isEmpty() ) {
            throw new IllegalArgumentException( "URI to load was blank." );
        }

        OntologyTerm ontologyTerm;
        for ( ubic.basecode.ontology.providers.OntologyService ontology : this.ontologies ) {
            ontologyTerm = ontology.getTerm( valueUri );
            if ( ontologyTerm != null )
                return ontologyTerm;
        }

        return null;
    }

    @Override
    public Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) throws SearchException {
        Map<String, OntologyTerm> uniqueValueTerm = new HashMap<>();

        for ( ubic.basecode.ontology.providers.OntologyService ontology : this.ontologies ) {
            Collection<OntologyTerm> hits;
            try {
                hits = ontology.findTerm( searchQuery );
            } catch ( OntologySearchException e ) {
                throw new BaseCodeOntologySearchException( e );
            }

            for ( OntologyTerm ontologyTerm : hits ) {
                if ( ontologyTerm.getLabel() != null && uniqueValueTerm.get( ontologyTerm.getLabel().toLowerCase() ) == null ) {
                    uniqueValueTerm.put( ontologyTerm.getLabel().toLowerCase(), ontologyTerm );
                }
            }
        }

        return ontology2CharacteristicValueObject( uniqueValueTerm.values() );
    }

    @Override
    public Collection<OntologyTerm> findValueUriInOntology( String searchQuery ) throws SearchException {

        Collection<OntologyTerm> results = new TreeSet<>();
        for ( ubic.basecode.ontology.providers.OntologyService ontology : this.ontologies ) {
            if ( ontology.isOntologyLoaded() ) {
                Collection<OntologyTerm> found;
                try {
                    found = ontology.findTerm( searchQuery );
                } catch ( OntologySearchException e ) {
                    throw new BaseCodeOntologySearchException( e );
                }
                if ( found != null && !found.isEmpty() )
                    results.addAll( found );
            }
        }

        return results;
    }

    @Override
    public Characteristic valueUri2Characteristic( String valueUri ) {
        OntologyTerm o = findOntologyTermByUri( valueUri );
        if ( o == null )
            return null;
        Characteristic myPhenotype = Characteristic.Factory.newInstance();
        myPhenotype.setValueUri( o.getUri() );
        myPhenotype.setValue( o.getLabel() );
        myPhenotype.setCategory( PhenotypeAssociationConstants.PHENOTYPE );
        myPhenotype.setCategoryUri( PhenotypeAssociationConstants.PHENOTYPE_CATEGORY_URI );
        return myPhenotype;
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
            if ( ontologyTerm.getLabel() == null ) {
                log.warn( "Term with null label: " + ontologyTerm.getUri() + "; it cannot be converted to a CharacteristicValueObject" );
                continue;
            }
            characteristicsVO.add( new CharacteristicValueObject( ontologyTerm.getLabel().toLowerCase(), ontologyTerm.getUri() ) );
        }
        return characteristicsVO;
    }
}
