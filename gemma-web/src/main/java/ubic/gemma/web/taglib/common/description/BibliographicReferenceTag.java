/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Keyword;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.util.ConfigUtils;

/**
 * Tag to output a bibliographic reference .
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class BibliographicReferenceTag extends TagSupport {

    /**
     * 
     */
    private static final long serialVersionUID = 9123111233311081600L;
    private BibliographicReference bibliographicReference;

    /**
     * @jsp.attribute description="The reference to be formatted" required="true" rtexprvalue="true"
     * @param bibliographicReference The bibliographicReference to set.
     */
    public void setBibliographicReference( BibliographicReference bibliographicReference ) {
        this.bibliographicReference = bibliographicReference;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        StringBuilder buf = new StringBuilder();

        if ( this.bibliographicReference == null ) {
            buf.append( "No publication" );
        }

        else {

            buf.append( "<table><tr><td valign=\"top\"><b>Pubmed</B></td><td>&nbsp;</td><td valign=\"top\">" );
            buf.append( "<a target=\"_blank\" href=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?"
                    + "cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids="
                    + bibliographicReference.getPubAccession().getAccession() + "&query_hl=3\">"
                    + bibliographicReference.getPubAccession().getAccession() + "</a>" );

            buf.append( "</td> </tr> <tr><td valign=\"top\"><b>Authors</B></td><td>&nbsp;</td><td valign=\"top\">" );
            buf.append( bibliographicReference.getAuthorList() );

            buf.append( "</td> </tr> <tr> <td valign=\"top\"><b>Year</B></td><td>&nbsp;</td><td valign=\"top\">" );
            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy" );
            Date publicationDate = bibliographicReference.getPublicationDate();

            if ( publicationDate != null ) {
                buf.append( sdf.format( publicationDate ) );
            } else {
                buf.append( "Publication date is null. " );
            }

            buf.append( "</td> </tr> <tr> <td valign=\"top\"><b>Title</B></td><td>&nbsp;</td><td valign=\"top\">" );
            buf.append( bibliographicReference.getTitle() );

            buf.append( "</td></tr><tr><td valign=\"top\"><b>Citation</B></td><td>&nbsp;</td><td valign=\"top\">" );
            buf.append( bibliographicReference.getPublication() + " " );

            if ( bibliographicReference.getVolume() != null ) {
                buf.append( "<em>" + bibliographicReference.getVolume() + "</em>: " );
            }
            buf.append( bibliographicReference.getPages() );

            buf.append( "</td></tr><tr><td valign=\"top\"><b>Abstract</B></td><td>&nbsp;</td><td valign=\"top\">" );
            if ( bibliographicReference.getAbstractText() != null ) {
                buf.append( bibliographicReference.getAbstractText() );
            } else {
                buf.append( "(No abstract available)" );
            }

            if ( bibliographicReference.getFullTextPdf() != null ) {

                String baseUrl = ConfigUtils.getString( "local.userfile.baseurl" );
                String basePath = ConfigUtils.getString( "local.userfile.basepath" );
                String localUriPath = bibliographicReference.getFullTextPdf().getLocalURL().toString();
                String relativeUrl = StringUtils.remove( localUriPath, "file:/" + basePath );
                String absoluteUrl = baseUrl + relativeUrl;

                buf.append( "</td></tr><tr><td valign=\"top\"><b>PDF</B></td><td>&nbsp;</td><td valign=\"top\">" );
                buf.append( "<a href=\"" + absoluteUrl + "\">" + absoluteUrl + "</a>" );
                buf.append( "&nbsp;(" + bibliographicReference.getFullTextPdf().getSize() + " bytes)" );
            }

            if ( bibliographicReference.getKeywords().size() > 0 ) {
                buf.append( "</td></tr><tr><td valign=\"top\"><b>Keywords</B></td><td>&nbsp;</td><td valign=\"top\">" );
                for ( Keyword o : bibliographicReference.getKeywords() ) {
                    buf.append( o.getTerm() + "<br />" );
                }
            }

            if ( bibliographicReference.getMeshTerms().size() > 0 ) {
                buf.append( "</td></tr><tr><td valign=\"top\"><b>MESH</B></td><td>&nbsp;</td><td valign=\"top\">" );
                for ( MedicalSubjectHeading o : bibliographicReference.getMeshTerms() ) {
                    buf.append( o.getTerm() + "<br />" );
                }
            }

            if ( bibliographicReference.getChemicals().size() > 0 ) {
                buf.append( "</td></tr><tr><td valign=\"top\"><b>Chemicals</B></td><td>&nbsp;</td><td valign=\"top\">" );
                for ( Compound o : bibliographicReference.getChemicals() ) {
                    buf.append( o.getName() + "<br />" );
                }
            }

            buf.append( "</td></tr></table>" );
        }
        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "BibliographicReferenceTag: " + ex.getMessage() );
        }

        return SKIP_BODY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.BodyTagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

}
