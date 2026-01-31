/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.common.description;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.entrez.pubmed.PubMedUtils;

import java.io.Serializable;
import java.net.URL;
import java.util.*;

/**
 * Represents a BibliographicReference as a citation string (which is really super light value object). This object has
 * four fields and is meant to be very simple, before adding anything consider using BibliographicReferenceValueObject.
 * This can't be an inner class of BibliographicReferenceValueObject because it needs dummy constructor for dwr to work
 * with it.
 *
 * @see BibliographicReferenceValueObject for a more comprehensive alternative representation of BibliographicReference
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Used in frontend
public class CitationValueObject implements Comparable<CitationValueObject>, Serializable {

    /**
     * The ID of the {@link BibliographicReference} being represented.
     */
    private Long id;
    private String citation;
    private String pubmedAccession;
    private String pubmedURL;
    private boolean retracted = false;

    /* needed for java bean contract */
    public CitationValueObject() {
        super();
    }

    private CitationValueObject( BibliographicReference ref ) {

        StringBuilder buf = new StringBuilder();

        if ( ref.getAuthorList() != null ) {
            String[] authors = StringUtils.split( ref.getAuthorList(), ";" );
            // if there are multiple authors, only display the first author
            if ( authors.length == 1 ) {
                buf.append( authors[0] ).append( " " );
            } else if ( authors.length > 0 ) {
                buf.append( authors[0] ).append( " et al. " );
            }
        } else {
            buf.append( "[Unknown authors]" );
        }
        // display the publication year
        if ( ref.getPublicationDate() != null ) {
            Calendar pubDate = new GregorianCalendar();
            pubDate.setTime( ref.getPublicationDate() );
            buf.append( "(" ).append( pubDate.get( Calendar.YEAR ) ).append( ") " );
        } else {
            buf.append( "[Unknown date]" );
        }

        String volume = ref.getVolume();
        if ( StringUtils.isBlank( volume ) ) {
            volume = "[no vol.]";
        }

        String pages = ref.getPages();

        if ( StringUtils.isBlank( pages ) ) {
            pages = "[no pages]";
        }

        buf.append( ref.getTitle() ).append( "; " ).append( ref.getPublication() ).append( ", " ).append( volume )
                .append( ": " ).append( pages );

        this.setCitation( buf.toString() );
        if ( ref.getPubAccession() != null ) {
            this.setPubmedAccession( ref.getPubAccession().getAccession() );
            this.setPubmedURL( PubMedUtils.getUrl( ref.getPubAccession().getAccession() ).toString() );
        }
        this.setId( ref.getId() );
        this.retracted = ref.getRetracted();

    }

    /**
     * @param ref ref
     * @return a citation value object constructed from a BibliographicReference or null if the BibliographicReference
     *         param
     *         was null
     */
    public static CitationValueObject convert2CitationValueObject( BibliographicReference ref ) {

        if ( ref == null ) {
            return null;
        }

        return new CitationValueObject( ref );
    }

    /**
     * @param refs refs
     * @return a collection of citation value objects constructed from a collection of BibliographicReference objects
     *         or an empty list if all the BibliographicReference list param was null or empty
     */
    public static List<CitationValueObject> convert2CitationValueObjects( Collection<BibliographicReference> refs ) {

        List<CitationValueObject> results = new ArrayList<>();

        if ( refs != null ) {

            for ( BibliographicReference ref : refs ) {
                results.add( new CitationValueObject( ref ) );
            }
        }

        return results;
    }

    @Override
    public int compareTo( CitationValueObject o ) {
        if ( this.getCitation() != null ) {
            return this.getCitation().toLowerCase().compareTo( o.getCitation().toLowerCase() );
        }
        return -1;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation( String citation ) {
        this.citation = citation;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getPubmedAccession() {
        return pubmedAccession;
    }

    public void setPubmedAccession( String pubmedID ) {
        this.pubmedAccession = pubmedID;
    }

    public String getPubmedURL() {
        return pubmedURL;
    }

    public void setPubmedURL( String pubmedURL ) {
        this.pubmedURL = pubmedURL;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( pubmedAccession == null ) ? 0 : pubmedAccession.hashCode() );
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
        CitationValueObject other = ( CitationValueObject ) obj;
        if ( pubmedAccession == null ) {
            return other.pubmedAccession == null;
        }
        return pubmedAccession.equals( other.pubmedAccession );
    }

    public boolean isRetracted() {
        return retracted;
    }

    public void setRetracted( boolean retracted ) {
        this.retracted = retracted;
    }

}
