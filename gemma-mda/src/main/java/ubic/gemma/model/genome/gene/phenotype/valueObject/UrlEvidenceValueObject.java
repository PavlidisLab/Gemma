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

import ubic.gemma.model.association.phenotype.UrlEvidence;

/** Value object representing an url evidence */
public class UrlEvidenceValueObject extends EvidenceValueObject {

    private String url = "";

    public String getUrl() {
        return this.url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    public UrlEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Set<CharacteristicValueObject> phenotypes, String url,
            EvidenceSourceValueObject evidenceSource ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes, evidenceSource );
        this.url = url;
    }

    /** Entity to Value Object */
    public UrlEvidenceValueObject( UrlEvidence urlEvidence ) {
        super( urlEvidence );
        this.url = urlEvidence.getUrl();
    }

}
