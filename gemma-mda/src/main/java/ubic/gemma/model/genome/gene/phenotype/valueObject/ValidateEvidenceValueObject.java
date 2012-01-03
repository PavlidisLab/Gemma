package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class ValidateEvidenceValueObject {

    boolean sameGeneAnnotated = false;
    boolean sameGeneAndPhenotypeAnnotated = false;
    boolean sameGeneAndChildOrParent = false;

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

    public boolean isSameGeneAndChildOrParent() {
        return this.sameGeneAndChildOrParent;
    }

    public void setSameGeneAndChildOrParent( boolean sameGeneAndChildOrParent ) {
        this.sameGeneAndChildOrParent = sameGeneAndChildOrParent;
    }
}
