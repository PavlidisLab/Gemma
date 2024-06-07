/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2008 University of British Columbia
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
package ubic.gemma.web.taglib;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.CitationValueObject;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author joseph
 */
public class ShortBibliographicReferenceTag extends TagSupport {

    private static final long serialVersionUID = -7325678534991860679L;

    private static Log log = LogFactory.getLog( ShortBibliographicReferenceTag.class );

    private BibliographicReference citation;

    @Override
    public int doStartTag() throws JspException {

        ShortBibliographicReferenceTag.log.debug( "start tag" );

        String contextPath = pageContext.getServletContext().getContextPath();

        StringBuilder buf = new StringBuilder();
        if ( this.citation == null ) {
            buf.append( "No accession" );
        } else {

            String authorList = citation.getAuthorList();

            if ( authorList != null ) {
                String[] authors = StringUtils.split( authorList, ";" );
                // if there are authors, only display the first author
                if ( authors.length == 0 ) {

                } else if ( authors.length == 1 ) {
                    buf.append( authors[0] ).append( " " );
                } else {
                    buf.append( authors[0] ).append( " et al. " );
                }
            } else {
                buf.append( "Null author list" );
            }

            // display the publication year
            Calendar pubDate = new GregorianCalendar();
            Date publicationDate = citation.getPublicationDate();

            if ( publicationDate != null ) {
                pubDate.setTime( publicationDate );
                buf.append( "(" ).append( pubDate.get( Calendar.YEAR ) ).append( ") " );
            } else {
                buf.append( "Null publication date" );
            }

            // add pubmed link
            if ( citation.getPubAccession() != null ) {
                String pubMedId = citation.getPubAccession().getAccession();
                CitationValueObject citationVO = CitationValueObject.convert2CitationValueObject( citation );
                if ( StringUtils.isNotBlank( pubMedId ) ) {
                    String link = citationVO.getPubmedURL();

                    buf.append( "<a target='_blank' href='" ).append( link ).append( "' ><img src='" )
                            .append( contextPath ).append( "/images/pubmed.gif' /> </a>&nbsp;" );

                    /*
                     * Add link to edit page within Gemma
                     */

                    buf.append( "<a target='_blank' href='" ).append( contextPath )
                            .append( "/bibRef/bibRefView.html?accession=" ).append( pubMedId ).append( "'><img src='" )
                            .append( contextPath ).append( "/images/magnifier.png' /></a>" );

                }
            }

        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( this.getClass().getName() + ex.getMessage() );
        }
        return Tag.SKIP_BODY;
    }

    @Override
    public int doEndTag() {

        ShortBibliographicReferenceTag.log.debug( "end tag" );

        return Tag.EVAL_PAGE;
    }

    public void setCitation( BibliographicReference citation ) {
        this.citation = citation;
    }
}