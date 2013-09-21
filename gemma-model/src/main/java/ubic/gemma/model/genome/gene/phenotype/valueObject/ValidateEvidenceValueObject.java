package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.HashSet;
import java.util.Set;

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
    private boolean sameEvidenceFound = false;

    // this is used to indicate the gene-phenotype conflicts while validating
    private Set<Long> problematicEvidenceIds = new HashSet<Long>();

    public Set<Long> getProblematicEvidenceIds() {
        return this.problematicEvidenceIds;
    }

    public boolean isAccessDenied() {
        return this.accessDenied;
    }

    public boolean isEvidenceNotFound() {
        return this.evidenceNotFound;
    }

    public boolean isLastUpdateDifferent() {
        return this.lastUpdateDifferent;
    }

    public boolean isPubmedIdInvalid() {
        return this.pubmedIdInvalid;
    }

    public boolean isSameEvidenceFound() {
        return this.sameEvidenceFound;
    }

    public boolean isSameGeneAndOnePhenotypeAnnotated() {
        return this.sameGeneAndOnePhenotypeAnnotated;
    }

    public boolean isSameGeneAndPhenotypeChildOrParentAnnotated() {
        return this.sameGeneAndPhenotypeChildOrParentAnnotated;
    }

    public boolean isSameGeneAndPhenotypesAnnotated() {
        return this.sameGeneAndPhenotypesAnnotated;
    }

    public boolean isSameGeneAnnotated() {
        return this.sameGeneAnnotated;
    }

    public boolean isUserNotLoggedIn() {
        return this.userNotLoggedIn;
    }

    public void setAccessDenied( boolean accessDenied ) {
        this.accessDenied = accessDenied;
    }

    public void setEvidenceNotFound( boolean evidenceNotFound ) {
        this.evidenceNotFound = evidenceNotFound;
    }

    public void setLastUpdateDifferent( boolean lastUpdateDifferent ) {
        this.lastUpdateDifferent = lastUpdateDifferent;
    }

    public void setProblematicEvidenceIds( Set<Long> problematicEvidenceIds ) {
        this.problematicEvidenceIds = problematicEvidenceIds;
    }

    public void setPubmedIdInvalid( boolean pubmedIdInvalid ) {
        this.pubmedIdInvalid = pubmedIdInvalid;
    }

    public void setSameEvidenceFound( boolean sameEvidenceFound ) {
        this.sameEvidenceFound = sameEvidenceFound;
    }

    public void setSameGeneAndOnePhenotypeAnnotated( boolean sameGeneAndOnePhenotypeAnnotated ) {
        this.sameGeneAndOnePhenotypeAnnotated = sameGeneAndOnePhenotypeAnnotated;
    }

    public void setSameGeneAndPhenotypeChildOrParentAnnotated( boolean sameGeneAndPhenotypeChildOrParentAnnotated ) {
        this.sameGeneAndPhenotypeChildOrParentAnnotated = sameGeneAndPhenotypeChildOrParentAnnotated;
    }

    public void setSameGeneAndPhenotypesAnnotated( boolean sameGeneAndPhenotypesAnnotated ) {
        this.sameGeneAndPhenotypesAnnotated = sameGeneAndPhenotypesAnnotated;
    }

    public void setSameGeneAnnotated( boolean sameGeneAnnotated ) {
        this.sameGeneAnnotated = sameGeneAnnotated;
    }

    public void setUserNotLoggedIn( boolean userNotLoggedIn ) {
        this.userNotLoggedIn = userNotLoggedIn;
    }

}
