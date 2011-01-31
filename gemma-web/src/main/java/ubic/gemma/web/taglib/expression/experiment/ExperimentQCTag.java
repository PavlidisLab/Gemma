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
import ubic.gemma.security.SecurityService;

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

    private boolean hasPvalueDistFiles = false;

    private boolean hasNodeDegreeDistFile = false;

    enum Size {
        small, large
    }

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
    public void setHasPvalueDistFiles( boolean value ) {
        this.hasPvalueDistFiles = value;
    }

    public void setHasNodeDegreeDistFile( boolean value ) {
        this.hasNodeDegreeDistFile = value;
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
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        StringBuilder buf = new StringBuilder();

        /*
         * check if the files are available...if not, show something intelligent.
         */

        buf.append( "<div class=\"eeqc\" id=\"eeqc\">" );
        buf.append( "<table border=\"0\" cellspacing=\"4\" style=\"background-color:#DDDDDD\" >" );

        buf.append( "<tr><th valign=\"top\" align=\"center\"><strong>Sample correlation (black &le; "
                + ExpressionDataSampleCorrelation.HI_CONTRAST_COR_THRESH
                + ")</strong></th>"
                // + "<th valign=\"top\" align=\"center\"><strong>PCA</strong></th>"
                // + "<th valign=\"top\" align=\"center\"><strong>Node degree</strong></th>"
                + "<th valign=\"top\" align=\"center\"><strong>Probe correlation</strong</th>"
                + "<th valign=\"top\" align=\"center\"><strong>Pvalue distributions</strong></th>" + "</tr>" );

        buf.append( "<tr>" );

        String placeHolder = "<td  style=\"margin:3px;padding:8px;background-color:#EEEEEE\" valign='top'>Not available</td>";

        if ( hasCorrMatFile ) {

            buf
                    .append( "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><a target=\"_blank\" title=\"Click for larger version (new page)\" href=\"visualizeCorrMat.html?id="
                            + this.eeid
                            + "&nocache="
                            + (int)Math.rint(Math.random()*1000)
                            + "&size=large\"><img src=\"visualizeCorrMat.html?id="
                            + this.eeid
                            + "&size="
                            + this.size
                            + "&nocache="
                            + (int)Math.rint(Math.random()*1000)
                            + "\" alt='Image unavailable'/></a>" );

            // link to lower contrast version
            buf
                    .append( "<ul><li><a class=\"newpage\" target=\"_blank\" title=\"Click for larger lower contrast version\" href=\"visualizeCorrMat.html?id="
                            + this.eeid
                            + "&nocache="
                            + (int)Math.rint(Math.random()*1000)
                            + "&size=large&contr=lo\">View low contrast version (black &le;"
                            + ExpressionDataSampleCorrelation.LO_CONTRAST_COR_THRESH + ")</a></li>" );
            buf
                    .append( "<li><a title=\"Download a file containing the raw correlation matrix data\" class=\"newpage\"  target=\"_blank\"  href=\"visualizeCorrMat.html?id="
                            + this.eeid                            
                            + "&text=1\">Get data</a></li>" );

            /* Need to have a security check before showing this. */
            // buf
            // .append( "<li><a  href=\"/Gemma/expressionExperiment/refreshCorrMatrix.html?id="
            // + this.eeid
            // +
            // "\" ><img src=\"/Gemma/images/icons/arrow_refresh_small.png\"  title=\"refresh\" alt=\"refresh\" /></span>"
            // );
            buf.append( "</ul></td>" );
        } else {
            buf.append( placeHolder );
        }
        //
        // if ( hasPCAFile ) {
        // buf
        // .append(
        // "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img alt='PCA' src=\"visualizePCA.html?id="
        // + this.eeid + "\" /></td>" );
        // } else {
        // buf.append( placeHolder );
        // }
        //
        // if ( hasNodeDegreeDistFile ) {
        // buf
        // .append(
        // "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img alt='Node degree dist' src=\"visualizeNodeDegreeDist.html?id="
        // + this.eeid + "\" /></td>" );
        // } else {
        // buf.append( placeHolder );
        // }

        if ( hasCorrDistFile ) {
            buf
                    .append( " <td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img alt='Correlation distribution' src=\"visualizeProbeCorrDist.html?id="
                            + this.eeid + "\" /></td>" );
        } else {
            buf.append( placeHolder );
        }

        if ( hasPvalueDistFiles ) {
            buf
                    .append( "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img alt='Pvalue distribution' src=\"visualizePvalueDist.html?id="
                            + this.eeid + "\" /></td>" );
        } else {
            buf.append( placeHolder );
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
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

}
