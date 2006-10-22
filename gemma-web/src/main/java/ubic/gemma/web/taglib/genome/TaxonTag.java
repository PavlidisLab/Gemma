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
package ubic.gemma.web.taglib.genome;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Taxon;

/**
 * @jsp.tag name="taxon" body-content="empty"
 * @author Kiran Keshav
 * @version $Id$
 */
public class TaxonTag extends TagSupport {
    /**
     * 
     */
    private static final long serialVersionUID = -1308354625783322070L;

    private Log log = LogFactory.getLog( this.getClass() );

    private Taxon taxon;

    /**
     * @jsp.attribute description="The taxon" required="true" rtexprvalue="true"
     * @param taxon
     */
    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
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
        if ( this.taxon == null ) {
            buf.append( "No taxons" );
        } else {
            buf.append( taxon.getScientificName() );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( this.getClass().getName() + ex.getMessage() );
        }
        return SKIP_BODY;
    }

}
