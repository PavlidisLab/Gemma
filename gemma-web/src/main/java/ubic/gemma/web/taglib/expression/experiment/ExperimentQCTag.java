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
 * @author paul
 * @version $Id$
 */
public class ExperimentQCTag extends TagSupport {

    private static final long serialVersionUID = -466958848014180520L;
    Long eeid;
    Size size = Size.small;

    private boolean hasCorrMatFile = false;

    private boolean hasCorrDistFile = false;

    private boolean hasPCAFile = false;

    private boolean hasPvalueDistFile = false;

    enum Size {
        small, large
    };

    /**
     * The id of the EE to display QC info required="true" rtexprvalue="true"
     * 
     * @param id
     */
    public void setEe( Long id ) {
        this.eeid = id;
    }

    /**
     * @param value
     */
    public void setHasCorrMatFile( boolean value ) {
        this.hasCorrMatFile = value;
    }

    /**
     * @param value
     */
    public void setHasCorrDistFile( boolean value ) {
        this.hasCorrDistFile = value;
    }

    /**
     * @param value
     */
    public void setHasPCAFile( boolean value ) {
        this.hasPCAFile = value;
    }

    /**
     * @param value
     */
    public void setHasPvalueDistFile( boolean value ) {
        this.hasPvalueDistFile = value;
    }

    /**
     * Size of the image {small, large} required="false" rtexprvalue="true"
     * 
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
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        StringBuilder buf = new StringBuilder();

        /*
         * check if the files are available...if not, show something intelligent.
         */

        buf.append( "<div class=\"eeqc\" id=\"eeqc\">" );
        buf.append( "<table border=\"0\" cellspacing=\"8\"  >" );

        buf
                .append( "<tr><th valign=\"top\" align=\"center\"><strong>Sample correlation (black &le; "
                        + ExpressionDataSampleCorrelation.HI_CONTRAST_COR_THRESH
                        + ")</strong></th><th valign=\"top\" align=\"center\"><strong>Probe correlation</strong</th><th valign=\"top\" align=\"center\"><strong>Pvalue distributions</strong></th></tr>" );

        buf.append( "<tr>" );

        if ( hasCorrMatFile ) {

            buf
                    .append( "<a target=\"_blank\" title=\"Click for larger version (opens in new window)\" href=\"visualizeCorrMat.html?id="
                            + this.eeid
                            + "&size=large\"><img src=\"visualizeCorrMat.html?id="
                            + this.eeid
                            + "&size="
                            + this.size + "\" alt='Image unavailable'/></a>" );

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
        } else {
            buf.append( "<td>Not available</td>" );
        }

        if ( hasCorrDistFile ) {
            buf.append( " <img alt='Image unavailable' src=\"visualizeProbeCorrDist.html?id=" + this.eeid
                    + "\" /></td>" );
        } else {
            buf.append( "<td>Not available</td>" );
        }

        if ( hasPvalueDistFile ) {
            buf.append( " <img alt='Image unavailable' src=\"visualizePvalueDist.html?id=" + this.eeid + "\" /></td>" );
        } else {
            buf.append( "<td>Not available</td>" );
        }

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
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

}
