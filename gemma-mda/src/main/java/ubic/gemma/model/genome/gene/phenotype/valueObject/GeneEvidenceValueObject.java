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
    private Collection<EvidenceValueObject> evidence;

    public GeneEvidenceValueObject( Gene gene ) {
        super( gene );
        this.evidence = EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    public GeneEvidenceValueObject( java.lang.Long id, java.lang.String name, Collection<java.lang.String> aliases,
            Integer ncbiId, java.lang.String officialSymbol, java.lang.String officialName,
            java.lang.String description, Double score, Long taxonId, String taxonScientificName,
            String taxonCommonName, Collection<EvidenceValueObject> evidence ) {
        super( id, name, aliases, ncbiId, officialSymbol, officialName, description, score, taxonId,
                taxonScientificName, taxonCommonName );
        this.evidence = evidence;
    }

    public Collection<EvidenceValueObject> getEvidence() {
        return this.evidence;
    }

    public void setEvidence( Collection<EvidenceValueObject> evidence ) {
        this.evidence = evidence;
    }

    /** Given a geneVO finds all valueRI of phenotypes for that gene */
    public Set<String> findAllPhenotpyesOnGene() {

        Set<String> allPhenotypesOnGene = new HashSet<String>();

        for ( EvidenceValueObject evidenceVO : this.evidence ) {
            for ( CharacteristicValueObject chaVO : evidenceVO.getPhenotypes() ) {
                allPhenotypesOnGene.add( chaVO.getValueUri() );
            }
        }

        return allPhenotypesOnGene;
    }

    public static Collection<GeneEvidenceValueObject> convert2GeneEvidenceValueObjects( Collection<Gene> genes ) {
        Collection<GeneEvidenceValueObject> converted = new HashSet<GeneEvidenceValueObject>();
        if ( genes == null ) return converted;

        for ( Gene g : genes ) {
            if ( g != null ) {

                Collection<EvidenceValueObject> evidenceFromPhenotype = EvidenceValueObject.convert2ValueObjects( g
                        .getPhenotypeAssociations() );

                converted.add( new GeneEvidenceValueObject( g.getId(), g.getName(), getAliasStrings( g ), g
                        .getNcbiGeneId(), g.getOfficialSymbol(), g.getOfficialName(), g.getDescription(), null, g
                        .getTaxon().getId(), g.getTaxon().getScientificName(), g.getTaxon().getCommonName(),
                        evidenceFromPhenotype ) );
            }
        }

        return converted;
    }

}