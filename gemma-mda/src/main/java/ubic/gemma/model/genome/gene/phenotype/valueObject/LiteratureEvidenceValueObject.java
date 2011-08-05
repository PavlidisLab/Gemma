package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;

public class LiteratureEvidenceValueObject extends EvidenceValueObject {

    private String pubmedID = "";

    private BibliographicReferenceValueObject bibliographicReferenceValueObject = null;

    public LiteratureEvidenceValueObject( String name, String description, String characteristic,
            Boolean isNegativeEvidence, GOEvidenceCode evidenceCode, Collection<String> characteristics, String pubmedID ) {
        super( name, description, characteristic, isNegativeEvidence, evidenceCode, characteristics );
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
