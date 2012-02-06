package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class EvidenceStatusValueObject {

    private Boolean lastUpdateDateDifferent = false;

    private Boolean evidenceAlreadyInDatabase = false;

    public Boolean getLastUpdateDateDifferent() {
        return this.lastUpdateDateDifferent;
    }

    public void setLastUpdateDateDifferent( Boolean lastUpdateDateDifferent ) {
        this.lastUpdateDateDifferent = lastUpdateDateDifferent;
    }

    public Boolean getEvidenceAlreadyInDatabase() {
        return this.evidenceAlreadyInDatabase;
    }

    public void setEvidenceAlreadyInDatabase( Boolean evidenceAlreadyInDatabase ) {
        this.evidenceAlreadyInDatabase = evidenceAlreadyInDatabase;
    }
}
