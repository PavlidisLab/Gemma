package ubic.gemma.association.phenotype.fileUpload.genericEvidence;

import ubic.gemma.association.phenotype.fileUpload.EvidenceLineInfo;

public class GenericEvidenceLineInfo extends EvidenceLineInfo {

    private String externalDatabaseName = "";
    private String databaseID = "";

    public GenericEvidenceLineInfo( String line ) {

        String[] tokens = line.split( "\t" );
        this.geneName = tokens[0].trim();
        this.geneID = tokens[1].trim();
        this.evidenceCode = tokens[2].trim();
        this.comment = tokens[3].trim();
        this.associationType = tokens[4].trim();

        if ( tokens[5].trim().equals( "1" ) ) {
            this.isEdivenceNegative = true;
        }

        this.externalDatabaseName = tokens[6].trim();
        this.databaseID = tokens[7].trim();
        this.phenotype = trimArray( tokens[8].split( ";" ) );
    }

    public boolean getIsEdivenceNegative() {
        return this.isEdivenceNegative;
    }

    public void setIsEdivenceNegative( boolean isEdivenceNegative ) {
        this.isEdivenceNegative = isEdivenceNegative;
    }

    public String getExternalDatabaseName() {
        return this.externalDatabaseName;
    }

    public void setExternalDatabaseName( String externalDatabaseName ) {
        this.externalDatabaseName = externalDatabaseName;
    }

    public String getDatabaseID() {
        return this.databaseID;
    }

    public void setDatabaseID( String databaseID ) {
        this.databaseID = databaseID;
    }

}
