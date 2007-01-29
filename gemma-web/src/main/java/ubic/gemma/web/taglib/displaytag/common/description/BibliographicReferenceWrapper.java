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
package ubic.gemma.web.taglib.displaytag.common.description;

import java.text.SimpleDateFormat;

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.BibliographicReference;

/**
 * Decorator for displaying bibligraphic refereneces in a list view.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceWrapper extends TableDecorator {

    // public String getAuthors() {
    // BibliographicReference ref = ( BibliographicReference ) this.getCurrentRowObject();
    // return StringUtils.abbreviate( ref.getAuthorList(), 20 );
    // }
    //
    // public String getTitle() {
    // BibliographicReference ref = ( BibliographicReference ) this.getCurrentRowObject();
    // return StringUtils.abbreviate( ref.getTitle(), 50 );
    // }

    public String getCitation() {
        // basically copied from the BibliographicReferenceTag.
        StringBuilder buf = new StringBuilder();
        BibliographicReference bibliographicReference = ( BibliographicReference ) this.getCurrentRowObject();
        buf.append( bibliographicReference.getPublication() + " " );

        if ( bibliographicReference.getVolume() != null ) {
            buf.append( "<em>" + bibliographicReference.getVolume() + "</em>: " );
        }
        buf.append( bibliographicReference.getPages() );
        return buf.toString();
    }

    public String getYear() {
        BibliographicReference bibliographicReference = ( BibliographicReference ) this.getCurrentRowObject();
        SimpleDateFormat form = new SimpleDateFormat( "yyyy" );
        return form.format( bibliographicReference.getPublicationDate() );
    }

}
