package ubic.gemma.association.phenotype.fileUpload.literatureEvidence;

import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

public class LitEvidenceLineInfo {

    // used the mgedOntologyService to get those values (defined structure)
    public final static String PHENOTYPE = "Phenotype";

    public final static String PHENOTYPE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Phenotype";

    private String geneName = "";
    private String geneID = "";
    private String primaryReferencePubmed = null;
    private String evidenceCode = "";
    private String comment = "";
    private String associationType = null;
    private Boolean isEdivenceNegative = false;
    private String[] phenotype = null;

    // What will populate the Evidence
    private Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

    public LitEvidenceLineInfo( String line ) {

        String[] tokens = line.split( "\t" );
        this.geneName = tokens[0].trim();
        this.geneID = tokens[1].trim();
        this.primaryReferencePubmed = tokens[2].trim();
        this.evidenceCode = tokens[3].trim();
        this.comment = tokens[4].trim();
        this.associationType = tokens[5].trim();

        if ( tokens[6].trim().equals( "1" ) ) {
            this.isEdivenceNegative = true;
        }

        this.phenotype = trimArray( tokens[7].split( ";" ) );
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

    public String getPrimaryReferencePubmed() {
        return primaryReferencePubmed;
    }

    public void setPrimaryReferencePubmed( String primaryReferencePubmed ) {
        this.primaryReferencePubmed = primaryReferencePubmed;
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
