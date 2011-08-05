package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.GenericEvidence;

public class GenericEvidenceValueObject extends EvidenceValueObject {

    public GenericEvidenceValueObject( String name, String description, String characteristic,
            Boolean isNegativeEvidence, GOEvidenceCode evidenceCode, Collection<String> characteristics, String url ) {
        super( name, description, characteristic, isNegativeEvidence, evidenceCode, characteristics );
    }

    /** Entity to Value Object */
    public GenericEvidenceValueObject( GenericEvidence genericEvidence ) {
        super( genericEvidence );
    }

}
