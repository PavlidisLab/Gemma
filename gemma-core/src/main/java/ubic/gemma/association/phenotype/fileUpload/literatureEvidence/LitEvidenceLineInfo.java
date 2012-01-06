package ubic.gemma.association.phenotype.fileUpload.literatureEvidence;

import ubic.gemma.association.phenotype.fileUpload.EvidenceLineInfo;

public class LitEvidenceLineInfo extends EvidenceLineInfo {

    private String primaryReferencePubmed = null;

    public LitEvidenceLineInfo( String line ) {

        String[] tokens = line.split( "\t" );
        this.geneName = tokens[0].trim();
        this.geneID = tokens[1].trim();
        this.primaryReferencePubmed = tokens[2].trim();
        this.evidenceCode = tokens[3].trim();
        this.comment = tokens[4].trim();
        this.associationType = tokens[5].trim();

        if ( tokens[6].trim().equals( "1" ) ) {
            this.isEdivenceNegative = true;
        }

        this.phenotype = trimArray( tokens[7].split( ";" ) );
    }

    public String getPrimaryReferencePubmed() {
        return this.primaryReferencePubmed;
    }

    public void setPrimaryReferencePubmed( String primaryReferencePubmed ) {
        this.primaryReferencePubmed = primaryReferencePubmed;
    }
}
