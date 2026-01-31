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

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.loader.entrez.pubmed.PubMedUtils;
import ubic.gemma.model.common.description.BibliographicReference;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static ubic.gemma.core.util.StringUtils.urlEncode;

/**
 * @author joseph
 */
public class ShortBibliographicReferenceTag extends AbstractHtmlElementTag {

    @Setter
    @Nullable
    private BibliographicReference citation;

    @Override
    public int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );

        if ( citation == null ) {
            tagWriter.startTag( "i" );
            tagWriter.appendValue( "No bibliographic reference" );
            tagWriter.endTag(); // </i>
            return Tag.SKIP_BODY;
        }

        tagWriter.startTag( "span" );
        writeOptionalAttributes( tagWriter );


        String authorList = citation.getAuthorList();

        if ( authorList != null ) {
            String[] authors = StringUtils.split( authorList, ";" );
            // if there are authors, only display the first author
            tagWriter.appendValue( htmlEscape( authors[0] ) );
            if ( authors.length > 1 ) {
                tagWriter.appendValue( " et al." );
            }
        } else {
            tagWriter.startTag( "i" );
            tagWriter.appendValue( "No authors" );
            tagWriter.endTag(); // </i>
        }

        // display the publication year
        Date publicationDate = citation.getPublicationDate();
        if ( publicationDate != null ) {
            Calendar pubDate = new GregorianCalendar();
            pubDate.setTime( publicationDate );
            tagWriter.appendValue( " (" + pubDate.get( Calendar.YEAR ) + ")" );
        }

        // add pubmed link
        if ( citation.getPubAccession() != null ) {
            String pubMedId = citation.getPubAccession().getAccession();
            if ( StringUtils.isNotBlank( pubMedId ) ) {
                String contextPath = pageContext.getServletContext().getContextPath();

                tagWriter.appendValue( " " );

                String link = PubMedUtils.getUrl( pubMedId ).toString();

                tagWriter.startTag( "a" );
                tagWriter.writeAttribute( "href", link );
                tagWriter.writeAttribute( "target", "_blank" );
                tagWriter.writeAttribute( "rel", "noopener noreferrer" );
                tagWriter.startTag( "img" );
                tagWriter.writeAttribute( "src", contextPath + "/images/logo/pubmed-logo-blue.svg" );
                tagWriter.writeAttribute( "height", "16" );
                tagWriter.writeAttribute( "alt", "PubMed Link" );
                tagWriter.endTag(); // </img>
                tagWriter.endTag(); // </a>

                tagWriter.appendValue( " " );

                /*
                 * Add link to edit page within Gemma
                 */
                tagWriter.startTag( "a" );
                tagWriter.writeAttribute( "href", contextPath + "/bibRef/bibRefView.html?accession=" + urlEncode( pubMedId ) );
                tagWriter.writeAttribute( "target", "_blank" );
                tagWriter.startTag( "img" );
                tagWriter.writeAttribute( "src", contextPath + "/images/magnifier.png" );
                tagWriter.endTag(); // </img>
                tagWriter.endTag(); // </a>
            }
        }

        tagWriter.endTag(); // </span>

        return Tag.SKIP_BODY;
    }
}