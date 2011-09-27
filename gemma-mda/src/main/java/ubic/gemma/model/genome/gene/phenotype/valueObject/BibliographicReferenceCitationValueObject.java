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
 * represents a BibliographicReference as a citation string (which is really super light value object)
 * 
 * 
 * @author pavlidis
 * @version
 */
public class BibliographicReferenceCitationValueObject {

    public static List<BibliographicReferenceValueObject> convert2ValueObjects( Collection<BibliographicReference> refs ) {
        if ( refs == null || refs.size() == 0 ) {
            return null;
        }

        List<BibliographicReferenceValueObject> results = new ArrayList<BibliographicReferenceValueObject>();

        for ( BibliographicReference ref : refs ) {
            results.add( new BibliographicReferenceValueObject( ref ) );
        }

        return results;
    }

    private String citation;

    public BibliographicReferenceCitationValueObject() {
        super();
    }

    public BibliographicReferenceCitationValueObject( BibliographicReference ref ) {
        
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
        
    }

    /**
     * @param citation the citation to set
     */
    public void setCitation( String citation ) {
        this.citation = citation;
    }

    /**
     * @return the citation
     */
    public String getCitation() {
        return citation;
    }

}
