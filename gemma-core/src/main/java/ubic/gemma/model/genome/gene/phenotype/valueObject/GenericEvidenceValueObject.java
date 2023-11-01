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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.SortedSet;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class GenericEvidenceValueObject extends EvidenceValueObject<GenericEvidence> {

    private static final long serialVersionUID = 754768748091915831L;

    /**
     * Required when using the class as a spring bean.
     */
    public GenericEvidenceValueObject() {
        super();
    }

    public GenericEvidenceValueObject( Long id ) {
        super( id );
    }

    public GenericEvidenceValueObject( GenericEvidence genericEvidence ) {
        super( genericEvidence );
    }

    public GenericEvidenceValueObject( Long id, Integer geneNCBI, SortedSet<CharacteristicValueObject> phenotypes,
            String description, String evidenceCode, boolean isNegativeEvidence,
            EvidenceSourceValueObject evidenceSource ) {
        super( id, geneNCBI, phenotypes, description, evidenceCode, isNegativeEvidence, evidenceSource );
    }

}
