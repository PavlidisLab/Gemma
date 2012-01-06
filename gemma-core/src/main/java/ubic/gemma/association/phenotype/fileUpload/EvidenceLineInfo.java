package ubic.gemma.association.phenotype.fileUpload;

import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

public class EvidenceLineInfo {

    protected String geneName = "";
    protected String geneID = "";
    protected String evidenceCode = "";
    protected String comment = "";
    protected String associationType = null;
    protected boolean isEdivenceNegative = false;
    protected String[] phenotype = null;
    protected Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

    protected String[] trimArray( String[] array ) {

        String[] trimmedArray = new String[array.length];

        for ( int i = 0; i < trimmedArray.length; i++ ) {
            trimmedArray[i] = array[i].trim();
        }

        return trimmedArray;
    }

    public String getGeneName() {
        return this.geneName;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    public String getGeneID() {
        return this.geneID;
    }

    public void setGeneID( String geneID ) {
        this.geneID = geneID;
    }

    public String getEvidenceCode() {
        return this.evidenceCode;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment( String comment ) {
        this.comment = comment;
    }

    public String getAssociationType() {
        return this.associationType;
    }

    public void setAssociationType( String associationType ) {
        this.associationType = associationType;
    }

    public boolean isEdivenceNegative() {
        return this.isEdivenceNegative;
    }

    public void setEdivenceNegative( boolean isEdivenceNegative ) {
        this.isEdivenceNegative = isEdivenceNegative;
    }

    public String[] getPhenotype() {
        return this.phenotype;
    }

    public void setPhenotype( String[] phenotype ) {
        this.phenotype = phenotype;
    }

    public Set<CharacteristicValueObject> getPhenotypes() {
        return this.phenotypes;
    }

    public void setPhenotypes( Set<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public void addPhenotype( CharacteristicValueObject phenotypeToAdd ) {
        this.phenotypes.add( phenotypeToAdd );
    }
}
