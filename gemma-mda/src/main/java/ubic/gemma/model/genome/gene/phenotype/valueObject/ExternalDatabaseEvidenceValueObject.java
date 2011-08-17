package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;

public class ExternalDatabaseEvidenceValueObject extends EvidenceValueObject {

    private DatabaseEntryValueObject databaseEntryValueObject = null;

    public ExternalDatabaseEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Collection<CharacteristicValueObject> phenotypes,
            DatabaseEntryValueObject databaseEntryValueObject ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );
        this.databaseEntryValueObject = databaseEntryValueObject;
    }

    /** Entity to Value Object */
    public ExternalDatabaseEvidenceValueObject( ExternalDatabaseEvidence externalDatabaseEvidence ) {
        super( externalDatabaseEvidence );
        this.databaseEntryValueObject = DatabaseEntryValueObject.fromEntity( externalDatabaseEvidence
                .getEvidenceSource() );
    }

    public DatabaseEntryValueObject getDatabaseEntryValueObject() {
        return databaseEntryValueObject;
    }

}
