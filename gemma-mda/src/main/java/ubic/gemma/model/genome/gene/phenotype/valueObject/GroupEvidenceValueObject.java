/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;

public class GroupEvidenceValueObject extends EvidenceValueObject {

    Collection<LiteratureEvidenceValueObject> literatureEvidences = new HashSet<LiteratureEvidenceValueObject>();

    public GroupEvidenceValueObject( Collection<LiteratureEvidenceValueObject> literatureEvidences ) {
        super();
        this.literatureEvidences = literatureEvidences;
        LiteratureEvidenceValueObject litEvidenceValueObject = literatureEvidences.iterator().next();

        this.setClassName( this.getClass().getSimpleName() );
        this.setDescription( litEvidenceValueObject.getDescription() );
        this.setEvidenceCode( litEvidenceValueObject.getEvidenceCode() );
        this.setEvidenceSecurityValueObject( litEvidenceValueObject.getEvidenceSecurityValueObject() );
        this.setEvidenceSource( litEvidenceValueObject.getEvidenceSource() );
        this.setGeneNCBI( litEvidenceValueObject.getGeneNCBI() );
        this.setIsNegativeEvidence( litEvidenceValueObject.getIsNegativeEvidence() );
        this.setPhenotypes( litEvidenceValueObject.getPhenotypes() );
        this.setRelevance( litEvidenceValueObject.getRelevance() );
    }

	public Collection<LiteratureEvidenceValueObject> getLiteratureEvidences() {
		return literatureEvidences;
	}
}
