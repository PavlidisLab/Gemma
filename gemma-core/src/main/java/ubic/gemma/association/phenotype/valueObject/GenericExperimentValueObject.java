package ubic.gemma.association.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;

//TODO stgeorgn
public class GenericExperimentValueObject extends EvidenceValueObject {

    public GenericExperimentValueObject( String name, String description, GOEvidenceCode evidenceCode,
            Boolean isNegativeEvidence, Collection<Characteristic> characteristics, Long databaseId ) {
        super( name, description, evidenceCode, isNegativeEvidence, characteristics, databaseId );
        // TODO Auto-generated constructor stub
    }

    @Override
    public PhenotypeAssociation createEntity() {
        // TODO Auto-generated method stub
        return null;
    }


    
  
    
}
