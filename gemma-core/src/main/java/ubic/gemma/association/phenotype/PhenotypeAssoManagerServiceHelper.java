/*
 * The Gemma project
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
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

/**
 * Convert all types of EvidenceValueObjects to their corresponding entity
 * 
 * @author nicolas
 * @version $Id$
 */
public interface PhenotypeAssoManagerServiceHelper {

    /** Ontology term to CharacteristicValueObject */
    public abstract Set<CharacteristicValueObject> ontology2CharacteristicValueObject(
            Collection<OntologyTerm> ontologyTerms, String ontologyUsed );

    /**
     * load evidence from the database and populate it with the updated information
     */
    public abstract void populateModifiedValues( EvidenceValueObject evidenceValueObject,
            PhenotypeAssociation phenotypeAssociation );

    /**
     * Sets the fields that are the same for any evidence. Doesn't populate phenotypes
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    public abstract void populatePheAssoWithoutPhenotypes( PhenotypeAssociation phe,
            EvidenceValueObject evidenceValueObject );

    /**
     * Sets the fields that are the same for any evidence.
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    public abstract void populatePhenotypeAssociation( PhenotypeAssociation phe, EvidenceValueObject evidenceValueObject );

    /**
     * Changes all type of evidenceValueObject to their corresponding entities
     * 
     * @param evidence the value object to change in an entity
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    public abstract PhenotypeAssociation valueObject2Entity( EvidenceValueObject evidence );

}