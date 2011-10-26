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

import java.util.Set;

import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CitationValueObject;

public class LiteratureEvidenceValueObject extends EvidenceValueObject {

    private CitationValueObject citationValueObject = null;

    public LiteratureEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Set<CharacteristicValueObject> phenotypes, String pubmedID ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );

        citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( pubmedID );
    }

    /** Entity to Value Object */
    public LiteratureEvidenceValueObject( LiteratureEvidence literatureEvidence ) {
        super( literatureEvidence );

        this.citationValueObject = BibliographicReferenceValueObject.constructCitation( literatureEvidence
                .getCitation() );
    }

    public CitationValueObject getCitationValueObject() {
        return citationValueObject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( citationValueObject == null ) ? 0 : citationValueObject.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;
        LiteratureEvidenceValueObject other = ( LiteratureEvidenceValueObject ) obj;
        if ( citationValueObject == null ) {
            if ( other.citationValueObject != null ) return false;
        } else if ( !citationValueObject.equals( other.citationValueObject ) ) return false;
        return true;
    }

}
