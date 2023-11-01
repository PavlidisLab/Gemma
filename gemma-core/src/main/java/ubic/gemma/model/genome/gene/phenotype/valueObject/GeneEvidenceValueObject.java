/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author kelsey
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Deprecated
public class GeneEvidenceValueObject extends GeneValueObject {

    private static final long serialVersionUID = -3484291071757959936L;

    /**
     * Added field for the Candidate Gene Management System
     */
    private Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidence = new HashSet<>();
    private Set<String> phenotypesValueUri = new HashSet<>();

    /**
     * Required when using the class as a spring bean.
     */
    public GeneEvidenceValueObject() {
        super();
    }

    public GeneEvidenceValueObject( Long id ) {
        super( id );
    }

    public GeneEvidenceValueObject( Gene gene,
            Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidence ) {
        super( gene );
        this.evidence = evidence;
    }

    /**
     * @return Given a geneVO finds all valueRI of phenotypes for that gene
     */
    public Set<String> findAllPhenotpyesOnGene() {

        Set<String> allPhenotypesOnGene = new HashSet<>();

        for ( EvidenceValueObject<? extends PhenotypeAssociation> evidenceVO : this.evidence ) {
            for ( CharacteristicValueObject chaVO : evidenceVO.getPhenotypes() ) {
                allPhenotypesOnGene.add( chaVO.getValueUri() );
            }
        }

        return allPhenotypesOnGene;
    }

    public Collection<EvidenceValueObject<? extends PhenotypeAssociation>> getEvidence() {
        return this.evidence;
    }

    public void setEvidence( Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidence ) {
        this.evidence = evidence;
    }

    public Set<String> getPhenotypesValueUri() {
        return this.phenotypesValueUri;
    }

    public void setPhenotypesValueUri( Set<String> phenotypesValueUri ) {
        this.phenotypesValueUri = phenotypesValueUri;
    }

}