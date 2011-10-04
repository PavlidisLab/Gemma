package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Set;

import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.association.phenotype.ExternalDatabaseEvidence;

public class ExternalDatabaseEvidenceValueObject extends EvidenceValueObject {

    private DatabaseEntryValueObject databaseEntryValueObject = null;
    private String externalUrl = "";

    public ExternalDatabaseEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Set<CharacteristicValueObject> phenotypes,
            DatabaseEntryValueObject databaseEntryValueObject ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );
        this.databaseEntryValueObject = databaseEntryValueObject;
    }

    /** Entity to Value Object */
    public ExternalDatabaseEvidenceValueObject( ExternalDatabaseEvidence externalDatabaseEvidence ) {
        super( externalDatabaseEvidence );
        this.databaseEntryValueObject = DatabaseEntryValueObject.fromEntity( externalDatabaseEvidence
                .getEvidenceSource() );
        this.externalUrl = externalDatabaseEvidence.getEvidenceSource().getExternalDatabase().getWebUri()
                + externalDatabaseEvidence.getEvidenceSource().getAccession();
    }

    public DatabaseEntryValueObject getDatabaseEntryValueObject() {
        return databaseEntryValueObject;
    }

    public void setDatabaseEntryValueObject( DatabaseEntryValueObject databaseEntryValueObject ) {
        this.databaseEntryValueObject = databaseEntryValueObject;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl( String externalUrl ) {
        this.externalUrl = externalUrl;
    }

}
