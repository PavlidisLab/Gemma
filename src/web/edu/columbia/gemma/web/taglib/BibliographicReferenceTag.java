/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.taglib;

import java.text.SimpleDateFormat;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import edu.columbia.gemma.common.description.BibliographicReference;

/**
 * Tag to output a bibliographic reference .
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @jsp.tag name="bibref" body-content="empty"
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceTag extends TagSupport {

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

        buf.append( "<table><tr><td><b>Pubmed ID</B></td><td>" );
        buf.append( bibliographicReference.getPubAccession().getAccession() );

        buf.append( "</td> </tr> <tr> <td><b>Authors</B></td> <td>" );
        buf.append( bibliographicReference.getAuthorList() );

        buf.append( "</td> </tr> <tr> <td><b>Year</B></td>  <td>" );
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy" );
        buf.append( sdf.format( bibliographicReference.getPublicationDate() ) );

        buf.append( "</td> </tr> <tr> <td><b>Title</B></td> <td>" );
        buf.append( bibliographicReference.getTitle() );

        buf.append( "</td></tr><tr><td><b>Publication</B></td><td>" );
        buf.append( bibliographicReference.getPublication() );

        buf.append( "</td></tr><tr> <td><b>Volume</B></td><td>" );
        buf.append( bibliographicReference.getVolume() );

        buf.append( "</td></tr><tr><td><b>Pages</B></td> <td>" );
        buf.append( bibliographicReference.getPages() );

        buf.append( "</td></tr><tr><td><b>Abstract Text</B></td><td>" );
        buf.append( bibliographicReference.getAbstractText() );

        buf.append( "</td></tr></table>" );

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
    @SuppressWarnings("unused")
    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

}
