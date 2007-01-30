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
package ubic.gemma.web.taglib.common.auditAndSecurity;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import ubic.gemma.analysis.report.WhatsNew;

/**
 * @author pavlidis
 * @version $Id$
 */
public class WhatsNewBoxTag extends TagSupport {

    private WhatsNew whatsNew;

    /**
     * @jsp.attribute description="WhatsNew repor" required="true" rtexprvalue="true"
     * @param contact
     */
    public void setWhatsNew( WhatsNew whatsNew ) {
        this.whatsNew = whatsNew;
    }

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    public int doStartTag() throws JspException {
        int numNew = whatsNew.getNewObjects().size();
        int numUpdated = whatsNew.getUpdatedObjects().size();

        StringBuilder buf = new StringBuilder();
        buf.append( "<h3>What's new in Gemma</h3>" );
        if ( numNew > 0 ) {
            int numEEs = whatsNew.getNewExpressionExperiments().size(); // FIXME make a link to see them.
            int numADs = whatsNew.getNewArrayDesigns().size(); // FIXME make a link to see them.
            if ( numEEs > 0 ) {
                buf.append( "<p>" + numEEs + " new data sets.</p>" );
            }
            if ( numADs > 0 ) {
                buf.append( "<p>" + numADs + " new array designs.</p>" );
            }
        } else {
            buf.append( "<p>Nothing new</p>" );
        }

        if ( numUpdated > 0 ) {
            int numEEs = whatsNew.getUpdatedExpressionExperiments().size(); // FIXME make a link to see them.
            int numADs = whatsNew.getUpdatedArrayDesigns().size(); // FIXME make a link to see them.
            if ( numEEs > 0 ) {
                buf.append( "<p>" + numEEs + " updated data sets.</p>" );
            }
            if ( numADs > 0 ) {
                buf.append( "<p>" + numADs + " updated array designs.</p>" );
            }
        } else {
            buf.append( "<p>Nothing updated</p>" );
        }
        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ContactTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

}
