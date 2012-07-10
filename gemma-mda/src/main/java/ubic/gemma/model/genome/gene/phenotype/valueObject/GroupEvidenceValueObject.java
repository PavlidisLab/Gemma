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
import java.util.TreeSet;

public class GroupEvidenceValueObject extends EvidenceValueObject {

    Collection<LiteratureEvidenceValueObject> literatureEvidences = new TreeSet<LiteratureEvidenceValueObject>();

    public GroupEvidenceValueObject( Collection<LiteratureEvidenceValueObject> literatureEvidences ) {
        super();
        for ( LiteratureEvidenceValueObject lit : literatureEvidences ) {
            this.literatureEvidences.add( lit );
        }

        LiteratureEvidenceValueObject litEvidenceValueObject = literatureEvidences.iterator().next();

        this.setId( litEvidenceValueObject.getId() );
        this.setClassName( this.getClass().getSimpleName() );
        this.setDescription( litEvidenceValueObject.getDescription() );
        this.setEvidenceCode( litEvidenceValueObject.getEvidenceCode() );
        this.setEvidenceSecurityValueObject( litEvidenceValueObject.getEvidenceSecurityValueObject() );
        this.setEvidenceSource( litEvidenceValueObject.getEvidenceSource() );
        this.setGeneId( litEvidenceValueObject.getGeneId() );
        this.setGeneNCBI( litEvidenceValueObject.getGeneNCBI() );
        this.setGeneOfficialSymbol( litEvidenceValueObject.getGeneOfficialSymbol() );
        this.setHomologueEvidence( litEvidenceValueObject.isHomologueEvidence() );
        this.setIsNegativeEvidence( litEvidenceValueObject.getIsNegativeEvidence() );
        this.setPhenotypes( litEvidenceValueObject.getPhenotypes() );
        this.setScoreValueObject( litEvidenceValueObject.getScoreValueObject() );
        this.setTaxonCommonName( litEvidenceValueObject.getTaxonCommonName() );
        this.setContainQueryPhenotype( litEvidenceValueObject.isContainQueryPhenotype() );
    }

    public Collection<LiteratureEvidenceValueObject> getLiteratureEvidences() {
        return this.literatureEvidences;
    }
}
