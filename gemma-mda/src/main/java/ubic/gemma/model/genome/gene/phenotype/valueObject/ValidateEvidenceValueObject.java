package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class ValidateEvidenceValueObject {

    private boolean sameGeneAnnotated = false;
    private boolean lastUpdateDifferent = false;
    private boolean sameGeneAndOnePhenotypeAnnotated = false;
    private boolean sameGeneAndPhenotypesAnnotated = false;
    private boolean sameGeneAndPhenotypeChildOrParentAnnotated = false;
    private boolean pubmedIdInvalid = false;
    private boolean evidenceNotFound = false;
    private boolean accessDenied = false;
    private boolean userNotLoggedIn = false;

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

    public boolean isPubmedIdInvalid() {
        return this.pubmedIdInvalid;
    }

    public void setPubmedIdInvalid( boolean pubmedIdInvalid ) {
        this.pubmedIdInvalid = pubmedIdInvalid;
    }

    public boolean isSameGeneAndOnePhenotypeAnnotated() {
        return this.sameGeneAndOnePhenotypeAnnotated;
    }

    public void setSameGeneAndOnePhenotypeAnnotated( boolean sameGeneAndOnePhenotypeAnnotated ) {
        this.sameGeneAndOnePhenotypeAnnotated = sameGeneAndOnePhenotypeAnnotated;
    }

    public boolean isLastUpdateDifferent() {
        return this.lastUpdateDifferent;
    }

    public void setLastUpdateDifferent( boolean lastUpdateDifferent ) {
        this.lastUpdateDifferent = lastUpdateDifferent;
    }

    public boolean isEvidenceNotFound() {
        return this.evidenceNotFound;
    }

    public void setEvidenceNotFound( boolean evidenceNotFound ) {
        this.evidenceNotFound = evidenceNotFound;
    }

	public boolean isAccessDenied() {
		return accessDenied;
	}

	public void setAccessDenied(boolean accessDenied) {
		this.accessDenied = accessDenied;
	}

	public boolean isUserNotLoggedIn() {
		return userNotLoggedIn;
	}

	public void setUserNotLoggedIn(boolean userNotLoggedIn) {
		this.userNotLoggedIn = userNotLoggedIn;
	}
}
