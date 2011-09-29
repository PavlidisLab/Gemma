package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Set;

import ubic.gemma.model.association.phenotype.LiteratureEvidence;

public class LiteratureEvidenceValueObject extends EvidenceValueObject {

    private String pubmedID = "";

    private BibliographicReferenceValueObject bibliographicReferenceValueObject = null;

    public LiteratureEvidenceValueObject( String description, CharacteristicValueObject associationType,
            Boolean isNegativeEvidence, String evidenceCode, Set<CharacteristicValueObject> phenotypes, String pubmedID ) {
        super( description, associationType, isNegativeEvidence, evidenceCode, phenotypes );
        this.pubmedID = pubmedID;
    }

    /** Entity to Value Object */
    public LiteratureEvidenceValueObject( LiteratureEvidence literatureEvidence ) {
        super( literatureEvidence );

        this.bibliographicReferenceValueObject = new BibliographicReferenceValueObject(
                literatureEvidence.getCitation() );
    }

    public String getPubmedID() {
        return pubmedID;
    }

    public BibliographicReferenceValueObject getBibliographicReferenceValueObject() {
        return bibliographicReferenceValueObject;
    }

}
