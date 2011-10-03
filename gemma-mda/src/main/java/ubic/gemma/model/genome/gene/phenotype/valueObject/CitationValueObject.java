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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.description.BibliographicReference;

/**
 * represents a BibliographicReference as a citation string (which is really super light value object). This object has
 * four fields and is meant to be very simple, before adding anything consider using BibliographicReferenceValueObject.
 * This can't be an inner class of BibliographicReferenceValueObject because it needs dummy constructor for dwr to work
 * with it.
 * 
 * @see ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicReferenceValueObject
 *      BibliographicReferenceValueObject for a more comprehensive alternative representation of BibliographicReference
 * @version
 */
public class CitationValueObject {

    /**
     * construct a citation value object from a BibliographicReference returns null if the BibliographicReference param
     * was null
     * 
     * @param ref
     * @return
     */
    public static CitationValueObject convert2CitationValueObject( BibliographicReference ref ) {

        if ( ref == null ) {
            return null;
        }

        return new CitationValueObject( ref );
    }

    /**
     * construct a collection of citation value objects from a collection of BibliographicReference objects returns an
     * empty list if all the BibliographicReference list param was null or empty
     * 
     * @param ref
     * @return
     */
    public static List<CitationValueObject> convert2CitationValueObjects( Collection<BibliographicReference> refs ) {

        List<CitationValueObject> results = new ArrayList<CitationValueObject>();

        if ( refs != null ) {

            for ( BibliographicReference ref : refs ) {
                results.add( new CitationValueObject( ref ) );
            }
        }

        return results;
    }

    private String citation;
    /**
     * the DB id of the BibliographicReference being represented
     */
    private Long id;
    private String pubmedAccession;
    private String pubmedURL;

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
                buf.append( authors[0] + " " );
            } else if ( authors.length > 0 ) {
                buf.append( authors[0] + " et al. " );
            }
        } else {
            buf.append( "[Unknown authors]" );
        }
        // display the publication year
        if ( ref.getPublicationDate() != null ) {
            Calendar pubDate = new GregorianCalendar();
            pubDate.setTime( ref.getPublicationDate() );
            buf.append( "(" + pubDate.get( Calendar.YEAR ) + ") " );
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

        buf.append( ref.getTitle() + "; " + ref.getPublication() + ", " + volume + ": " + pages );

        this.setCitation( buf.toString() );
        this.setPubmedAccession( ref.getPubAccession().getAccession() );
        this.setPubmedURL( BibliographicReferenceValueObject.PUBMED_URL_ROOT + ref.getId() );
        this.setId( ref.getId() );

    }

    /**
     * @return the citation
     */
    public String getCitation() {
        return citation;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the pubmedID
     */
    public String getPubmedAccession() {
        return pubmedAccession;
    }

    /**
     * @return the pubmedURL
     */
    public String getPubmedURL() {
        return pubmedURL;
    }

    /**
     * @param citation the citation to set
     */
    public void setCitation( String citation ) {
        this.citation = citation;
    }

    /**
     * @param id the id to set
     */
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @param pubmedID the pubmedID to set
     */
    public void setPubmedAccession( String pubmedID ) {
        this.pubmedAccession = pubmedID;
    }

    /**
     * @param pubmedURL the pubmedURL to set
     */
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
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CitationValueObject other = ( CitationValueObject ) obj;
        if ( pubmedAccession == null ) {
            if ( other.pubmedAccession != null ) return false;
        } else if ( !pubmedAccession.equals( other.pubmedAccession ) ) return false;
        return true;
    }

}
