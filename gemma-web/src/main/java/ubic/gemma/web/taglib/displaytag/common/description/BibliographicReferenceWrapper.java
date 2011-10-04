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
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Decorator for displaying bibligraphic refereneces in a list view.
 * <p>
 * Note: To use this class, the collection of objects viewed in the table should be of type
 * BibliographicReferenceValueObject, not BibliographicReference. A typical place to check for this is in the
 * controller. That is, a controller should return Collection<BibliographicReferenceValueObject> and not
 * Collection<BibliographicReference>.
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated we aren't using this any more
 */
@Deprecated
public class BibliographicReferenceWrapper extends TableDecorator {
    private static final String UNAVAILABLE = "Unavailable";
    private Log log = LogFactory.getLog( this.getClass() );

    // public String getAuthors() {
    // BibliographicReference ref = ( BibliographicReference ) this.getCurrentRowObject();
    // return StringUtils.abbreviate( ref.getAuthorList(), 20 );
    // }
    //
    // public String getTitle() {
    // BibliographicReference ref = ( BibliographicReference ) this.getCurrentRowObject();
    // return StringUtils.abbreviate( ref.getTitle(), 50 );
    // }

    /**
     * @return String
     */
    public String getCitation() {
        // basically copied from the BibliographicReferenceTag.
        StringBuilder buf = new StringBuilder();
        Object obj = this.getCurrentRowObject();

        BibliographicReferenceValueObject bibliographicReference = ( BibliographicReferenceValueObject ) obj;
        buf.append( bibliographicReference.getPublication() + " " );

        if ( bibliographicReference.getVolume() != null ) {
            buf.append( "<em>" + bibliographicReference.getVolume() + "</em>: " );
        }
        buf.append( bibliographicReference.getPages() );

        return buf.toString();
    }

    /**
     * @return
     */
    public String getYear() {
        Object obj = this.getCurrentRowObject();

        BibliographicReferenceValueObject bibliographicReference = ( BibliographicReferenceValueObject ) obj;
        SimpleDateFormat form = new SimpleDateFormat( "yyyy" );
        Date publicationDate = bibliographicReference.getPublicationDate();
        if ( publicationDate != null ) {
            return form.format( publicationDate );
        }
        return "?";

    }

    /**
     * @return
     */
    public String getExperiments() {
        BibliographicReferenceValueObject br = ( BibliographicReferenceValueObject ) this.getCurrentRowObject();
        if ( br.getExperiments().size() == 0 ) return "";
        StringBuilder buf = new StringBuilder();
        if ( br.getExperiments().size() == 1 ) {
            buf.append( "<a href='/Gemma/expressionExperiment/showExpressionExperiment.html?id=" );
            buf.append( br.getExperiments().iterator().next().getId() );
            buf.append( "'>" + br.getExperiments().iterator().next().getShortName() + "</a>" );
        } else {
            buf.append( "<a href='/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=" );
            for ( ExpressionExperimentValueObject ee : br.getExperiments() ) {
                buf.append( ee.getId() + "," );
            }
            buf.append( "'>[" + br.getExperiments().size() + " datasets]</a>" );
        }
        return buf.toString();
    }

    /**
     * @return
     */
    public String getUpdate() {
        BibliographicReferenceValueObject br = ( BibliographicReferenceValueObject ) this.getCurrentRowObject();
        return "<input id='" + br.getId() + "' type=\"button\"  value=\"Update\" onClick='doUpdate(" + br.getId()
                + ");' />";
    }

    /**
     * @return
     */
    public String getAccessionLink() {
        BibliographicReferenceValueObject br = ( BibliographicReferenceValueObject ) this.getCurrentRowObject();

        Long id = br.getId();

        StringBuilder buf = new StringBuilder();

        String pubAccession = br.getPubAccession();

        if ( pubAccession == null ) {
            log.warn( "No accession for bibliographic reference with id = " + id );
            buf.append( UNAVAILABLE );
            return buf.toString();
        }

        if ( StringUtils.isEmpty( pubAccession ) ) {
            log.warn( "Pubmed accession for bibliographic reference with id = " + id
                    + "exists, but accesion is not filled in." );
            buf.append( UNAVAILABLE );
            return buf.toString();
        }

        buf.append( "<a href='/Gemma/bibRef/bibRefView.html?accession=" );
        buf.append( pubAccession );
        buf.append( "'>" );
        buf.append( "<img src=\'/Gemma/images/magnifier.png\' />" );
        buf.append( "</a>" );

        log.info( buf.toString() );
        return buf.toString();
    }
}
