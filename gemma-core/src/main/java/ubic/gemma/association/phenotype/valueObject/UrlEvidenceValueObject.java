package ubic.gemma.association.phenotype.valueObject;

import java.util.Collection;

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
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

    public UrlEvidenceValueObject( String name, String description, Boolean isNegativeEvidence,
            GOEvidenceCode evidenceCode, Collection<String> characteristics, String url ) {
        super( name, description, isNegativeEvidence, evidenceCode, characteristics );
        this.url = url;
    }

    public UrlEvidenceValueObject( UrlEvidence urlEvidence ) {
        super( urlEvidence.getName(), urlEvidence.getDescription(), urlEvidence.getEvidenceCode(), urlEvidence
                .getIsNegativeEvidence(), urlEvidence.getPhenotypes(), urlEvidence.getId() );
        this.url = urlEvidence.getUrl();
    }

    /** Change the value object to an entity */
    @Override
    public PhenotypeAssociation createEntity() {

        // Create the entity with no values
        UrlEvidence urlEvidence = UrlEvidence.Factory.newInstance();
        // set fields common to all evidence
        populatePhenotypeAssociation( urlEvidence );
        // set specific field unique to this evidence
        urlEvidence.setUrl( url );

        return urlEvidence;
    }

}
