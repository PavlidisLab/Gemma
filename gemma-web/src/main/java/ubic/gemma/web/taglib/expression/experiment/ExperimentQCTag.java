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
package ubic.gemma.web.taglib.expression.experiment;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;

/**
 * @jsp.tag name="expressionQC" body-content="empty"
 * @author paul
 * @version $Id$
 */
public class ExperimentQCTag extends TagSupport {
    Long eeid;
    Size size = Size.small;

    enum Size {
        small, large
    };

    /**
     * @jsp.attribute description="The id of the EE to display QC info" required="true" rtexprvalue="true"
     * @param id
     */
    public void setEe( Long id ) {
        this.eeid = id;
    }

    /**
     * @jsp.attribute description="Size of the image {small, large}" required="false" rtexprvalue="true"
     * @param size
     */
    public void setSize( String size ) {
        Size s = Size.valueOf( size.toLowerCase() );
        if ( s == null ) {
            this.size = Size.small;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        StringBuilder buf = new StringBuilder();
        buf.append( "<div class=\"eeqc\" id=\"eeqc\">" );
        buf
                .append( "<table border=\"0\"  ><tr><td valign=\"top\" align=\"left\" style=\"padding-right:30px;\"><strong>Sample correlation (black &le; "
                        + ExpressionDataSampleCorrelation.HI_CONTRAST_COR_THRESH + ")</strong><br />" );
        buf
                .append( "<a target=\"_blank\" title=\"Click for larger version (opens in new window)\" href=\"visualizeCorrMat.html?id="
                        + this.eeid
                        + "&size=large\"><img src=\"visualizeCorrMat.html?id="
                        + this.eeid
                        + "&size="
                        + this.size + "\" /></a>" );

        // link to lower contrast version
        buf
                .append( "<ul class='glassList'><li><a target=\"_blank\" title=\"Click for larger lower contrast version (opens in new window)\" href=\"visualizeCorrMat.html?id="
                        + this.eeid
                        + "&size=large&contr=lo\">lower contrast version (black &le;"
                        + ExpressionDataSampleCorrelation.LO_CONTRAST_COR_THRESH + ")</a></li>" );
        buf
                .append( "<li><a title=\"Download a file containing the raw correlation matrix data\" href=\"visualizeCorrMat.html?id="
                        + this.eeid + "&text=1\">Data</a></li>" );

        buf.append( "</ul></td>" );

        buf.append( "<td valign=\"top\" align=\"center\"><strong>Probe correlation</strong><br />" );
        buf.append( " <img src=\"visualizeProbeCorrDist.html?id=" + this.eeid + "\" /></td>" );

        buf.append( "</tr></table></div>" );
        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "experiment QC tag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

}
