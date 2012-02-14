package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class EvidenceErrorValueObject {

    private boolean lastUpdateDifferent = false;

    private boolean evidenceAlreadyInDatabase = false;

    public boolean isLastUpdateDifferent() {
        return this.lastUpdateDifferent;
    }

    public void setLastUpdateDifferent( boolean lastUpdateDifferent ) {
        this.lastUpdateDifferent = lastUpdateDifferent;
    }

    public boolean isEvidenceAlreadyInDatabase() {
        return this.evidenceAlreadyInDatabase;
    }

    public void setEvidenceAlreadyInDatabase( boolean evidenceAlreadyInDatabase ) {
        this.evidenceAlreadyInDatabase = evidenceAlreadyInDatabase;
    }

}
