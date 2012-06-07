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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
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

    transient private WhatsNew whatsNew;

    private static final int MAX_NEW_ITEMS = 50;

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

    @Override
    public int doStartTag() throws JspException {

        Collection<ExpressionExperiment> newExpressionExperiments = whatsNew.getNewExpressionExperiments();
        Collection<ArrayDesign> newArrayDesigns = whatsNew.getNewArrayDesigns();
        Collection<ExpressionExperiment> updatedExpressionExperiments = whatsNew.getUpdatedExpressionExperiments();
        Collection<ArrayDesign> updatedArrayDesigns = whatsNew.getUpdatedArrayDesigns();
        // don't show things that are "new" as "updated" too (if they were updated after being loaded)
        updatedExpressionExperiments.removeAll( newExpressionExperiments );
        updatedArrayDesigns.removeAll( newArrayDesigns );

        StringBuilder buf = new StringBuilder();

        if ( newArrayDesigns.size() == 0 && newExpressionExperiments.size() == 0
                && updatedExpressionExperiments.size() == 0 && updatedArrayDesigns.size() == 0 ) {
            buf.append( "<input type='hidden' name='nothing new' />" );
        } else {
            buf.append( " <strong>Changes in the" );
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
            buf.append( "</strong> " );
            buf.append( "<p>" );

            int numEEs = newExpressionExperiments.size();
            int numADs = newArrayDesigns.size();
            int updatedAds = updatedArrayDesigns.size();
            int updatedEEs = updatedExpressionExperiments.size();

            if ( numEEs > 0 ) {
                int count = 0;
                boolean tooMany = false;
                List<Long> ids = new ArrayList<Long>();
                for ( ExpressionExperiment ee : newExpressionExperiments ) {
                    ids.add( ee.getId() );
                    if ( ++count > MAX_NEW_ITEMS ) {
                        tooMany = true;
                        break;
                    }
                }
                buf.append( "<a " + ( tooMany ? "title='View the first " + MAX_NEW_ITEMS + "'" : "" )
                        + " href=\"/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=" );
                buf.append( StringUtils.join( ids, "," ) );
                buf.append( "\">" + numEEs + " new data set" + ( numEEs > 1 ? "s" : "" ) + "</a>.<br />" );
            }
            if ( numADs > 0 ) {

                int count = 0;
                boolean tooMany = false;
                List<Long> ids = new ArrayList<Long>();
                for ( ArrayDesign ad : newArrayDesigns ) {
                    // buf.append( ad.getId() + "," );
                    ids.add( ad.getId() );
                    if ( ++count > MAX_NEW_ITEMS ) {
                        tooMany = true;
                        break;
                    }
                }
                buf.append( "<a " + ( tooMany ? "title='View the first " + MAX_NEW_ITEMS + "'" : "" )
                        + " href=\"/Gemma/arrays/showAllArrayDesigns.html?id=" );
                buf.append( StringUtils.join( ids, "," ) );
                buf.append( "\">" + numADs + " new platform" + ( numADs > 1 ? "s" : "" ) + "</a>.<br />" );
            }

            if ( updatedEEs > 0 ) {
                boolean tooMany = false;
                List<Long> ids = new ArrayList<Long>();
                int count = 0;
                for ( ExpressionExperiment ee : updatedExpressionExperiments ) {
                    ids.add( ee.getId() );

                    if ( ++count > MAX_NEW_ITEMS ) {
                        tooMany = true;
                        break;
                    }
                }
                buf.append( "<a " + ( tooMany ? "title='View the first " + MAX_NEW_ITEMS + "'" : "" )
                        + " href=\"/Gemma/expressionExperiment/showAllExpressionExperiments.html?id=" );
                buf.append( StringUtils.join( ids, "," ) );
                buf.append( "\">" + updatedEEs + " updated data set" + ( updatedEEs > 1 ? "s" : "" ) + "</a>.<br />" );

            }

            if ( updatedAds > 0 ) {
                int count = 0;
                boolean tooMany = false;
                List<Long> ids = new ArrayList<Long>();
                for ( ArrayDesign ad : updatedArrayDesigns ) {
                    ids.add( ad.getId() );
                    if ( ++count > MAX_NEW_ITEMS ) {
                        tooMany = true;
                        break;
                    }
                }
                buf.append( "<a " + ( tooMany ? "title='View the first " + MAX_NEW_ITEMS + "'" : "" )
                        + " href=\"/Gemma/arrays/showAllArrayDesigns.html?id=" );
                buf.append( StringUtils.join( ids, "," ) );
                buf.append( "\">" + updatedAds + " updated platform" + ( updatedAds > 1 ? "s" : "" )
                        + "</a>.<br />" );

            }

            buf.append( "</p>" );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ContactTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}
