/*
 * The gemma-core project
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
package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.Set;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

/**
 * TODO Document Me
 * 
 * @author nicolas
 * @version $Id$
 */
public interface PhenotypeAssoOntologyHelper {

    /**
     * Gemma might be ready but the ontology thread not finish loading
     */
    public abstract boolean areOntologiesAllLoaded();

    /**
     * CharacteristicValueObject to Characteristic with no valueUri given
     */
    public abstract VocabCharacteristic characteristicValueObject2Characteristic(
            CharacteristicValueObject characteristicValueObject );

    /**
     * Giving some Ontology terms return all valueUri of Ontology Terms + children
     */
    public abstract Set<String> findAllChildrenAndParent( Collection<OntologyTerm> ontologyTerms );

    /**
     * For a valueUri return the OntologyTerm found
     */
    public abstract OntologyTerm findOntologyTermByUri( String valueUri ) throws EntityNotFoundException;

    /**
     * search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO
     */
    public abstract Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery );

    /**
     * search the disease, hp and mp ontology for OntologyTerm
     * 
     * @param searchQuery free text query?
     */
    public abstract Collection<OntologyTerm> findValueUriInOntology( String searchQuery );

    /**
     * Helper method. For a valueUri return the Characteristic (represents a phenotype)
     */
    public abstract Characteristic valueUri2Characteristic( String valueUri );

}