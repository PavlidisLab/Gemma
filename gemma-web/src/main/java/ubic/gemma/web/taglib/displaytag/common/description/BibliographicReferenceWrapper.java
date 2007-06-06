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

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceValueObject;

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
        Object obj = this.getCurrentRowObject();

        BibliographicReferenceValueObject bibliographicReference = ( BibliographicReferenceValueObject ) obj;
        buf.append( bibliographicReference.getPublication() + " " );

        if ( bibliographicReference.getVolume() != null ) {
            buf.append( "<em>" + bibliographicReference.getVolume() + "</em>: " );
        }
        buf.append( bibliographicReference.getPages() );

        return buf.toString();
    }

    public String getYear() {
        Object obj = this.getCurrentRowObject();

        BibliographicReferenceValueObject bibliographicReference = ( BibliographicReferenceValueObject ) obj;
        SimpleDateFormat form = new SimpleDateFormat( "yyyy" );
        Date publicationDate = bibliographicReference.getPublicationDate();
        if ( publicationDate != null ) {
            return form.format( publicationDate );
        } else {
            return "?";
        }

    }

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
            for ( ExpressionExperiment ee : br.getExperiments() ) {
                buf.append( ee.getId() + "," );
            }
            buf.append( "'>[" + br.getExperiments().size() + " datasets]</a>" );
        }
        return buf.toString();
    }

    public String getUpdate() {
        BibliographicReferenceValueObject br = ( BibliographicReferenceValueObject ) this.getCurrentRowObject();
        return "<input id='" + br.getId() + "' type=\"button\"  value=\"Update\" onClick='doUpdate(" + br.getId()
                + ");' />";
    }

}
