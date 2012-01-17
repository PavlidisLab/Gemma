package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class ValidateEvidenceValueObject {

    boolean sameGeneAnnotated = false;
    boolean sameGeneAndPhenotypeAnnotated = false;
    boolean sameGeneAndPhenotypeChildOrParentAnnotated = false;
    boolean invalidPubmedId = false;

    public boolean isSameGeneAnnotated() {
        return this.sameGeneAnnotated;
    }

    public void setSameGeneAnnotated( boolean sameGeneAnnotated ) {
        this.sameGeneAnnotated = sameGeneAnnotated;
    }

    public boolean isSameGeneAndPhenotypeAnnotated() {
        return this.sameGeneAndPhenotypeAnnotated;
    }

    public void setSameGeneAndPhenotypeAnnotated( boolean sameGeneAndPhenotypeAnnotated ) {
        this.sameGeneAndPhenotypeAnnotated = sameGeneAndPhenotypeAnnotated;
    }

    public boolean isSameGeneAndPhenotypeChildOrParentAnnotated() {
        return this.sameGeneAndPhenotypeChildOrParentAnnotated;
    }

    public void setSameGeneAndPhenotypeChildOrParentAnnotated( boolean sameGeneAndPhenotypeChildOrParentAnnotated ) {
        this.sameGeneAndPhenotypeChildOrParentAnnotated = sameGeneAndPhenotypeChildOrParentAnnotated;
    }

    public boolean isInvalidPubmedId() {
        return this.invalidPubmedId;
    }

    public void setInvalidPubmedId( boolean invalidPubmedId ) {
        this.invalidPubmedId = invalidPubmedId;
    }
}
