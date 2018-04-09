package ubic.gemma.model.genome.gene.phenotype.valueObject;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class EvidenceSourceValueObject extends DatabaseEntryValueObject {

    private static final long serialVersionUID = 4085159943613845499L;
    // used by neurocarta to find the url of an evidence source
    private String externalUrl = "";

    /**
     * Required when using the class as a spring bean.
     */
    public EvidenceSourceValueObject() {
    }

    public EvidenceSourceValueObject( DatabaseEntry de ) {
        super( de );
        this.externalUrl = de.getExternalDatabase().getWebUri() + de.getAccession();
    }

    public EvidenceSourceValueObject( String accession, ExternalDatabaseValueObject externalDatabase ) {
        super( -1L ); // does not represent persistent database entry
        this.setAccession( accession );
        this.setExternalDatabase( externalDatabase );
    }

    public String getExternalUrl() {
        return this.externalUrl;
    }

    public void setExternalUrl( String externalUrl ) {
        this.externalUrl = externalUrl;
    }

}
