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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * @author kelsey
 * @version $Id$
 */
public class GeneEvidenceValueObject extends GeneValueObject {

    private static final long serialVersionUID = -3484291071757959936L;

    /** Added field for the Candidate Gene Management System */
    private Collection<EvidenceValueObject> evidence = new HashSet<>();

    private Set<String> phenotypesValueUri = new HashSet<>();

    public GeneEvidenceValueObject() {
        super();
    }

    public GeneEvidenceValueObject( Gene gene, Collection<EvidenceValueObject> evidence ) {
        super( gene );
        this.evidence = evidence;
    }

    public GeneEvidenceValueObject( Long id, String name, Collection<String> aliases, Integer ncbiId,
            String officialSymbol, String officialName, String description, Double score, Long taxonId,
            String taxonScientificName, String taxonCommonName, Collection<EvidenceValueObject> evidence ) {
        super( id, name, aliases, ncbiId, officialSymbol, officialName, description, score, taxonId,
                taxonScientificName, taxonCommonName );
        this.evidence = evidence;
    }

    /** Given a geneVO finds all valueRI of phenotypes for that gene */
    public Set<String> findAllPhenotpyesOnGene() {

        Set<String> allPhenotypesOnGene = new HashSet<>();

        for ( EvidenceValueObject evidenceVO : this.evidence ) {
            for ( CharacteristicValueObject chaVO : evidenceVO.getPhenotypes() ) {
                allPhenotypesOnGene.add( chaVO.getValueUri() );
            }
        }

        return allPhenotypesOnGene;
    }

    public Collection<EvidenceValueObject> getEvidence() {
        return this.evidence;
    }

    public Set<String> getPhenotypesValueUri() {
        return this.phenotypesValueUri;
    }

    public void setEvidence( Collection<EvidenceValueObject> evidence ) {
        this.evidence = evidence;
    }

    public void setPhenotypesValueUri( Set<String> phenotypesValueUri ) {
        this.phenotypesValueUri = phenotypesValueUri;
    }

}