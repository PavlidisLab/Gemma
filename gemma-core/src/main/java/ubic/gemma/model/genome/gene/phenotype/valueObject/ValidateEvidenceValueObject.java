package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
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
    private boolean descriptionInvalidSymbol = false;

    // this is used to indicate the gene-phenotype conflicts while validating
    private Set<Long> problematicEvidenceIds = new HashSet<>();

    public Set<Long> getProblematicEvidenceIds() {
        return this.problematicEvidenceIds;
    }

    public void setProblematicEvidenceIds( Set<Long> problematicEvidenceIds ) {
        this.problematicEvidenceIds = problematicEvidenceIds;
    }

    public boolean isAccessDenied() {
        return this.accessDenied;
    }

    public void setAccessDenied( boolean accessDenied ) {
        this.accessDenied = accessDenied;
    }

    public boolean isEvidenceNotFound() {
        return this.evidenceNotFound;
    }

    public void setEvidenceNotFound( boolean evidenceNotFound ) {
        this.evidenceNotFound = evidenceNotFound;
    }

    public boolean isLastUpdateDifferent() {
        return this.lastUpdateDifferent;
    }

    public void setLastUpdateDifferent( boolean lastUpdateDifferent ) {
        this.lastUpdateDifferent = lastUpdateDifferent;
    }

    public boolean isPubmedIdInvalid() {
        return this.pubmedIdInvalid;
    }

    public void setPubmedIdInvalid( boolean pubmedIdInvalid ) {
        this.pubmedIdInvalid = pubmedIdInvalid;
    }

    public boolean isSameEvidenceFound() {
        return this.sameEvidenceFound;
    }

    public void setSameEvidenceFound( boolean sameEvidenceFound ) {
        this.sameEvidenceFound = sameEvidenceFound;
    }

    public boolean isSameGeneAndOnePhenotypeAnnotated() {
        return this.sameGeneAndOnePhenotypeAnnotated;
    }

    public void setSameGeneAndOnePhenotypeAnnotated( boolean sameGeneAndOnePhenotypeAnnotated ) {
        this.sameGeneAndOnePhenotypeAnnotated = sameGeneAndOnePhenotypeAnnotated;
    }

    public boolean isSameGeneAndPhenotypeChildOrParentAnnotated() {
        return this.sameGeneAndPhenotypeChildOrParentAnnotated;
    }

    public void setSameGeneAndPhenotypeChildOrParentAnnotated( boolean sameGeneAndPhenotypeChildOrParentAnnotated ) {
        this.sameGeneAndPhenotypeChildOrParentAnnotated = sameGeneAndPhenotypeChildOrParentAnnotated;
    }

    public boolean isSameGeneAndPhenotypesAnnotated() {
        return this.sameGeneAndPhenotypesAnnotated;
    }

    public void setSameGeneAndPhenotypesAnnotated( boolean sameGeneAndPhenotypesAnnotated ) {
        this.sameGeneAndPhenotypesAnnotated = sameGeneAndPhenotypesAnnotated;
    }

    public boolean isSameGeneAnnotated() {
        return this.sameGeneAnnotated;
    }

    public void setSameGeneAnnotated( boolean sameGeneAnnotated ) {
        this.sameGeneAnnotated = sameGeneAnnotated;
    }

    public boolean isUserNotLoggedIn() {
        return this.userNotLoggedIn;
    }

    public void setUserNotLoggedIn( boolean userNotLoggedIn ) {
        this.userNotLoggedIn = userNotLoggedIn;
    }

    public boolean isDescriptionInvalidSymbol() {
        return descriptionInvalidSymbol;
    }

    public void setDescriptionInvalidSymbol( boolean descriptionInvalidSymbol ) {
        this.descriptionInvalidSymbol = descriptionInvalidSymbol;
    }

}
