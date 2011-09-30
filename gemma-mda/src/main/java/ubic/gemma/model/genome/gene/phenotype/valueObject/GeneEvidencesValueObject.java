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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * @author kelsey
 * @version $Id$
 */
public class GeneEvidencesValueObject extends GeneValueObject {

    /**
     * 
     */
    private static final long serialVersionUID = -3484291071757959936L;
    /**
     * 
     */
    /** Added field for the Candidate Gene Management System */
    private Collection<EvidenceValueObject> evidences;

    public static Collection<GeneEvidencesValueObject> convert2GeneEvidencesValueObjects( Collection<Gene> genes ) {
        Collection<GeneEvidencesValueObject> converted = new HashSet<GeneEvidencesValueObject>();
        if ( genes == null ) return converted;

        for ( Gene g : genes ) {
            if ( g != null ) {

                Collection<EvidenceValueObject> evidencesFromPhenotype = EvidenceValueObject.convert2ValueObjects( g
                        .getPhenotypeAssociations() );

                converted.add( new GeneEvidencesValueObject( g.getId(), g.getName(), getAliasStrings( g ), g
                        .getNcbiId(), g.getOfficialSymbol(), g.getOfficialName(), g.getDescription(), null, g
                        .getTaxon().getId(), g.getTaxon().getScientificName(), g.getTaxon().getCommonName(),
                        evidencesFromPhenotype ) );
            }
        }

        return converted;
    }

    public GeneEvidencesValueObject( Gene gene ) {
        super( gene );
        this.evidences = EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    public GeneEvidencesValueObject( java.lang.Long id, java.lang.String name, Collection<java.lang.String> aliases,
            java.lang.String ncbiId, java.lang.String officialSymbol, java.lang.String officialName,
            java.lang.String description, Double score, Long taxonId, String taxonScientificName,
            String taxonCommonName, Collection<EvidenceValueObject> evidences ) {
        super( id, name, aliases, ncbiId, officialSymbol, officialName, description, score, taxonId,
                taxonScientificName, taxonCommonName );
        this.evidences = evidences;
    }

    public Collection<EvidenceValueObject> getEvidences() {
        return evidences;
    }

    public void setEvidences( Collection<EvidenceValueObject> evidences ) {
        this.evidences = evidences;
    }

}