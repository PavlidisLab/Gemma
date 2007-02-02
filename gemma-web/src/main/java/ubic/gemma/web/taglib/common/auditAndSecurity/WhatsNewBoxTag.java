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

import java.util.Calendar;
import java.util.Date;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.time.DateUtils;

import ubic.gemma.analysis.report.WhatsNew;

/**
 * @jsp.tag name="whatsNew" body-content="empty"
 * @author pavlidis
 * @version $Id$
 */
public class WhatsNewBoxTag extends TagSupport {

    private WhatsNew whatsNew;

    /**
     * @jsp.attribute description="WhatsNew report" required="true" rtexprvalue="true"
     * @param whatsNew
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
        buf.append( "<h2>What's new in Gemma in the " );
        Date date = whatsNew.getDate();
        Date now = Calendar.getInstance().getTime();
        long millis = now.getTime() - date.getTime();
        double days = millis / ( double ) DateUtils.MILLIS_PER_DAY;
        if ( days > 0.9 && days < 2.0 ) {
            buf.append( " last day" );
        } else if ( days < 8 ) {
            buf.append( " last week" );
        } else {
            buf.append( " last " + days + " days" );
        }
        buf.append( "</h2>" );
        buf.append("<p>");
        if ( numNew > 0 ) {
            int numEEs = whatsNew.getNewExpressionExperiments().size(); // FIXME make a link to see them.
            int numADs = whatsNew.getNewArrayDesigns().size(); // FIXME make a link to see them.
            if ( numEEs > 0 ) {
                buf.append( numEEs + " new data set" + ( numEEs > 1 ? "s" : "" ) + ".<br />" );
            }
            if ( numADs > 0 ) {
                buf.append( numADs + " new array design" + ( numADs > 1 ? "s" : "" ) + ".<br />" );
            }
        } else {
            buf.append( "[No new experiments or platforms]<br />" );
        }

        if ( numUpdated > 0 ) {
            int numEEs = whatsNew.getUpdatedExpressionExperiments().size(); // FIXME make a link to see them.
            int numADs = whatsNew.getUpdatedArrayDesigns().size(); // FIXME make a link to see them.
            if ( numEEs > 0 ) {
                buf.append(numEEs + " updated data set" + ( numEEs > 1 ? "s" : "" ) + ".<br />" );
            }
            if ( numADs > 0 ) {
                buf.append( numADs + " updated array design" + ( numADs > 1 ? "s" : "" ) + ".<br />" );
            }
        } else {
            buf.append( "[No experiments or platforms updated]<br />" );
        }
        
        buf.append("</p>");
        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ContactTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}
