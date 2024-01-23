/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * @author nicolas
 */
@Deprecated
public interface PhenotypeAssoOntologyHelper {

    /**
     * Returns the ontology services this helper is using.
     */
    Collection<OntologyService> getOntologyServices();

    /**
     * CharacteristicValueObject to Characteristic with no valueUri given
     *
     * @param  characteristicValueObject characteristic VO
     * @return vocab characteristic
     */
    Characteristic characteristicValueObject2Characteristic( CharacteristicValueObject characteristicValueObject );

    /**
     * For a valueUri return the OntologyTerm found
     *
     * @param  valueUri value uri
     * @return ontology term
     */
    @Nullable
    OntologyTerm findOntologyTermByUri( String valueUri );

    /**
     * search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO
     *
     * @param  searchQuery query
     * @return characteristic VOs
     */
    Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) throws SearchException;

    /**
     * search the disease, hp and mp ontology for OntologyTerm
     *
     * @param  searchQuery free text query?
     * @return terms
     */
    Collection<OntologyTerm> findValueUriInOntology( String searchQuery ) throws SearchException;

    /**
     * Helper method. For a valueUri return the Characteristic (represents a phenotype)
     *
     * @param  valueUri value uri
     * @return Characteristic
     */
    @Nullable
    Characteristic valueUri2Characteristic( String valueUri );

}