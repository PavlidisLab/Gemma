package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;

// TODO stgeorgn
public class LiteratureEvidenceValueObject extends EvidenceValueObject {

    public LiteratureEvidenceValueObject( String name, String description, Boolean isNegativeEvidence,
            GOEvidenceCode evidenceCode, Collection<String> phenotypes ) {
        super( name, description, isNegativeEvidence, evidenceCode, phenotypes );
        // TODO Auto-generated constructor stub
    }

    @Override
    public PhenotypeAssociation createEntity() {
        // TODO Auto-generated method stub
        return null;
    }

}
