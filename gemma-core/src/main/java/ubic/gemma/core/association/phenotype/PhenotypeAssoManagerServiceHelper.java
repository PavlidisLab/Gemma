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
package ubic.gemma.core.association.phenotype;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

/**
 * Convert all types of EvidenceValueObjects to their corresponding entity
 *
 * @author nicolas
 */
@Deprecated
public interface PhenotypeAssoManagerServiceHelper {

    /**
     * load evidence from the database and populate it with the updated information
     *
     * @param evidenceValueObject  evidence VO
     * @param phenotypeAssociation phenotype association
     */
    void populateModifiedValues( EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject,
            PhenotypeAssociation phenotypeAssociation );

    /**
     * Changes all type of evidenceValueObject to their corresponding entities
     *
     * @param evidence the value object to change in an entity
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    PhenotypeAssociation valueObject2Entity( EvidenceValueObject<? extends PhenotypeAssociation> evidence );

}