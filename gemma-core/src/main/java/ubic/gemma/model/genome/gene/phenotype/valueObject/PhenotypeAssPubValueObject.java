package ubic.gemma.model.genome.gene.phenotype.valueObject;

import ubic.gemma.model.association.phenotype.PhenotypeAssociationPublication;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CitationValueObject;

import java.io.Serializable;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class PhenotypeAssPubValueObject implements Comparable<PhenotypeAssPubValueObject>, Serializable {

    public static final String PRIMARY = "Primary";
    public static final String RELEVANT = "Relevant";
    private String type = null;
    private CitationValueObject citationValueObject = null;

    public PhenotypeAssPubValueObject() {
        super();
    }

    public PhenotypeAssPubValueObject( PhenotypeAssociationPublication assocationPublication ) {
        this.type = assocationPublication.getType();
        if ( assocationPublication.getCitation() != null ) {
            this.citationValueObject = BibliographicReferenceValueObject
                    .constructCitation( assocationPublication.getCitation() );
        }
    }

    public static PhenotypeAssPubValueObject createPrimaryPublication( String accession ) {

        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( accession );
        PhenotypeAssPubValueObject phenotypeAssPubValueObject = new PhenotypeAssPubValueObject();
        phenotypeAssPubValueObject.setType( PhenotypeAssPubValueObject.PRIMARY );
        phenotypeAssPubValueObject.setCitationValueObject( citationValueObject );

        return phenotypeAssPubValueObject;
    }

    public static PhenotypeAssPubValueObject createRelevantPublication( String accession ) {
        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( accession );
        PhenotypeAssPubValueObject phenotypeAssPubValueObject = new PhenotypeAssPubValueObject();
        phenotypeAssPubValueObject.setType( PhenotypeAssPubValueObject.RELEVANT );
        phenotypeAssPubValueObject.setCitationValueObject( citationValueObject );

        return phenotypeAssPubValueObject;
    }

    public String getType() {
        return type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public CitationValueObject getCitationValueObject() {
        return citationValueObject;
    }

    public void setCitationValueObject( CitationValueObject citationValueObject ) {
        this.citationValueObject = citationValueObject;
    }

    @Override
    public int compareTo( PhenotypeAssPubValueObject phenotypeAssociationPublicationValueObject ) {

        int compare = this.type.compareTo( phenotypeAssociationPublicationValueObject.getType() );

        if ( compare == 0 && this.citationValueObject != null
                && phenotypeAssociationPublicationValueObject.getCitationValueObject() != null ) {
            compare = this.citationValueObject
                    .compareTo( phenotypeAssociationPublicationValueObject.getCitationValueObject() );
        }

        return compare;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( citationValueObject == null ) ? 0 : citationValueObject.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        PhenotypeAssPubValueObject other = ( PhenotypeAssPubValueObject ) obj;
        if ( citationValueObject == null ) {
            if ( other.citationValueObject != null )
                return false;
        } else if ( !citationValueObject.equals( other.citationValueObject ) )
            return false;
        if ( type == null ) {
            return other.type == null;
        } else
            return type.equals( other.type );
    }

}
