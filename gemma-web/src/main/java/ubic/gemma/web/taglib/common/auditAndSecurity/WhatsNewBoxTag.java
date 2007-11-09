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

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.time.DateUtils;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Tag to show 'what's new in Gemma' on home page.
 * 
 * @jsp.tag name="whatsNew" body-content="empty"
 * @author pavlidis
 * @author joseph
 * @version $Id$
 */
public class WhatsNewBoxTag extends TagSupport {

    /**
     * 
     */
    private static final long serialVersionUID = 4743861628500226664L;

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
            NumberFormat nf = NumberFormat.getIntegerInstance();
            buf.append( " last " + nf.format( days ) + " days" );
        }
        buf.append( "</h2>" );
        buf.append( "<p>" );
        
        int numNew = whatsNew.getNewObjects().size();
        Collection<ExpressionExperiment> newExpressionExperiments = whatsNew.getNewExpressionExperiments();
        Collection<ArrayDesign> newArrayDesigns = whatsNew.getNewArrayDesigns();
        
        if ( numNew > 0 ) {
           
            int numEEs = newExpressionExperiments.size();
          
            int numADs = newArrayDesigns.size();
            if ( numEEs > 0 ) {
                buf.append( "<a href=\"/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=" );
                for ( ExpressionExperiment ee : newExpressionExperiments ) {
                    buf.append( ee.getId() + "," );
                }
                buf.append( "\">" + numEEs + " new data set" + ( numEEs > 1 ? "s" : "" ) + "</a>.<br />" );
            }
            if ( numADs > 0 ) {
                buf.append( "<a href=\"/Gemma/arrays/showAllArrayDesigns.html?id=" );
                for ( ArrayDesign ad : newArrayDesigns ) {
                    buf.append( ad.getId() + "," );
                }
                buf.append( "\">" + numADs + " new array design" + ( numADs > 1 ? "s" : "" ) + "</a>.<br />" );
            }
        } else {
            buf.append( "Nothing new<br />" );
        }

        Collection<ExpressionExperiment> updatedExpressionExperiments = whatsNew.getUpdatedExpressionExperiments();
        Collection<ArrayDesign> updatedArrayDesigns = whatsNew.getUpdatedArrayDesigns();
        // don't show things that are "new" as "updated" too.
        updatedExpressionExperiments.removeAll( newExpressionExperiments );
        updatedArrayDesigns.removeAll(newArrayDesigns);
        if ( newArrayDesigns.size() + newExpressionExperiments.size() > 0 ) {
            
            int numEEs = updatedExpressionExperiments.size();
            
            int numADs = updatedArrayDesigns.size();
            if ( numEEs > 0 ) {
                buf.append( "<a href=\"/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=" );
                for ( ExpressionExperiment ee : updatedExpressionExperiments ) {
                    buf.append( ee.getId() + "," );
                }
                buf.append( "\">" + numEEs + " updated data set" + ( numEEs > 1 ? "s" : "" ) + "</a>.<br />" );
            }
            if ( numADs > 0 ) {
                buf.append( "<a href=\"/Gemma/arrays/showAllArrayDesigns.html?id=" );
                for ( ArrayDesign ad : updatedArrayDesigns ) {
                    buf.append( ad.getId() + "," );
                }
                buf.append( "\">" + numADs + " updated array design" + ( numADs > 1 ? "s" : "" ) + "</a>.<br />" );
            }
        } else {
            buf.append( "No updates<br />" );
        }

        buf.append( "</p>" );
        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ContactTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}
