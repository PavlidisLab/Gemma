package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class ValidateEvidenceValueObject {

    boolean sameGeneAnnotated = false;
    
    
    boolean sameGeneAndOnePhenotypeAnnotated = false;
    boolean sameGeneAndPhenotypesAnnotated = false;
    boolean sameGeneAndPhenotypeChildOrParentAnnotated = false;
    boolean invalidPubmedId = false;

    public boolean isSameGeneAnnotated() {
        return this.sameGeneAnnotated;
    }

    public void setSameGeneAnnotated( boolean sameGeneAnnotated ) {
        this.sameGeneAnnotated = sameGeneAnnotated;
    }

    public boolean isSameGeneAndPhenotypesAnnotated() {
        return this.sameGeneAndPhenotypesAnnotated;
    }

    public void setSameGeneAndPhenotypesAnnotated( boolean sameGeneAndPhenotypesAnnotated ) {
        this.sameGeneAndPhenotypesAnnotated = sameGeneAndPhenotypesAnnotated;
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

    public boolean isSameGeneAndOnePhenotypeAnnotated() {
        return this.sameGeneAndOnePhenotypeAnnotated;
    }

    public void setSameGeneAndOnePhenotypeAnnotated( boolean sameGeneAndOnePhenotypeAnnotated ) {
        this.sameGeneAndOnePhenotypeAnnotated = sameGeneAndOnePhenotypeAnnotated;
    }
    
    
}
