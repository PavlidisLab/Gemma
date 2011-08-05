package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.UrlEvidence;

/** Value object representing an url evidence */
public class UrlEvidenceValueObject extends EvidenceValueObject {

    private String url = "";

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    public UrlEvidenceValueObject( String name, String description, String characteristic, Boolean isNegativeEvidence,
            GOEvidenceCode evidenceCode, Collection<String> characteristics, String url ) {
        super( name, description, characteristic, isNegativeEvidence, evidenceCode, characteristics );
        this.url = url;
    }

    /** Entity to Value Object */
    public UrlEvidenceValueObject( UrlEvidence urlEvidence ) {
        super( urlEvidence );
        this.url = urlEvidence.getUrl();
    }

}
