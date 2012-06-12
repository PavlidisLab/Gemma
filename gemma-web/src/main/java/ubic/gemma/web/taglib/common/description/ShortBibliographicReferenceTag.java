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
package ubic.gemma.web.taglib.common.description;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.CitationValueObject;

/**
 * @author joseph
 * @version
 */
public class ShortBibliographicReferenceTag extends TagSupport {

    /**
     * 
     */
    private static final long serialVersionUID = -7325678534991860679L;

    private static Log log = LogFactory.getLog( ShortBibliographicReferenceTag.class );

    private BibliographicReference citation;

    /**
     * @param citation
     */
    public void setCitation( BibliographicReference citation ) {
        this.citation = citation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {

        log.debug( "end tag" );

        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        log.debug( "start tag" );

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
                    buf.append( authors[0] + " " );
                } else {
                    buf.append( authors[0] + " et al. " );
                }
            } else {
                buf.append( "Null author list" );
            }

            // display the publication year
            Calendar pubDate = new GregorianCalendar();
            Date publicationDate = citation.getPublicationDate();

            if ( publicationDate != null ) {
                pubDate.setTime( publicationDate );
                buf.append( "(" + pubDate.get( Calendar.YEAR ) + ") " );
            } else {
                buf.append( "Null publication date" );
            }

            // add pubmed link
            if ( citation.getPubAccession() != null ) {
                String pubMedId = citation.getPubAccession().getAccession();
                CitationValueObject citationVO = CitationValueObject.convert2CitationValueObject( citation );
                if ( StringUtils.isNotBlank( pubMedId ) ) {
                    String link = citationVO.getPubmedURL();

                    buf.append( "<a target='_blank' href='" + link
                            + "' ><img src='/Gemma/images/pubmed.gif' /> </a>&nbsp;" );

                    /*
                     * Add link to edit page within Gemma
                     */

                    buf.append( "<a target='_blank' href='/Gemma/bibRef/bibRefView.html?accession=" + pubMedId
                            + "'><img src='/Gemma/images/magnifier.png' /></a>" );

                }
            }

        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( this.getClass().getName() + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}