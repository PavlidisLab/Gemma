package ubic.gemma.association.phenotype.fileUpload.externalDatabaseEvidence;

import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

public class DatabaseEvidenceLineInfo {

    // used the mgedOntologyService to get those values (defined structure)
    public final static String PHENOTYPE = "Phenotype";

    public final static String PHENOTYPE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Phenotype";

    private String geneName = "";
    private String geneID = "";
    private String evidenceCode = "";
    private String comment = "";
    private String associationType = null;
    private Boolean isEdivenceNegative = false;

    private String externalDatabaseName = "";
    private String databaseID = "";

    private String[] phenotype = null;

    // What will populate the Evidence
    private Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

    public DatabaseEvidenceLineInfo( String line ) {

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

    private String[] trimArray( String[] array ) {

        String[] trimmedArray = new String[array.length];

        for ( int i = 0; i < trimmedArray.length; i++ ) {
            trimmedArray[i] = array[i].trim();
        }

        return trimmedArray;
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    public String getGeneID() {
        return geneID;
    }

    public void setGeneID( String geneID ) {
        this.geneID = geneID;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment( String comment ) {
        this.comment = comment;
    }

    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType( String associationType ) {
        this.associationType = associationType;
    }

    public Boolean getIsEdivenceNegative() {
        return isEdivenceNegative;
    }

    public void setIsEdivenceNegative( Boolean isEdivenceNegative ) {
        this.isEdivenceNegative = isEdivenceNegative;
    }

    public String getExternalDatabaseName() {
        return externalDatabaseName;
    }

    public void setExternalDatabaseName( String externalDatabaseName ) {
        this.externalDatabaseName = externalDatabaseName;
    }

    public String getDatabaseID() {
        return databaseID;
    }

    public void setDatabaseID( String databaseID ) {
        this.databaseID = databaseID;
    }

    public String[] getPhenotype() {
        return phenotype;
    }

    public void setPhenotype( String[] phenotype ) {
        this.phenotype = phenotype;
    }

    public Set<CharacteristicValueObject> getPhenotypes() {
        return phenotypes;
    }

    public void setPhenotypes( Set<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public void addPhenotype( CharacteristicValueObject phenotype ) {
        phenotypes.add( phenotype );
    }

}
