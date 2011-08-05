package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;

public class ExternalDatabaseEvidenceValueObject extends EvidenceValueObject {

    private DatabaseEntryValueObject databaseEntryValueObject = null;

    public ExternalDatabaseEvidenceValueObject( String name, String description, String characteristic,
            Boolean isNegativeEvidence, GOEvidenceCode evidenceCode, Collection<String> characteristics,
            DatabaseEntryValueObject databaseEntryValueObject ) {
        super( name, description, characteristic, isNegativeEvidence, evidenceCode, characteristics );
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
