package ubic.gemma.association.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;

//TODO stgeorgn
public class DifferentialExpressionValueObject extends EvidenceValueObject {

    public DifferentialExpressionValueObject( String name, String description, Boolean isNegativeEvidence,
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
