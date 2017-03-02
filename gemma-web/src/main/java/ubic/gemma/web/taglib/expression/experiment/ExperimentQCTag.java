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

import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentQCController;

/**
 * @author paul
 * @version $Id$
 */
public class ExperimentQCTag extends TagSupport {

    enum Size {
        small, large
    }

    private static final int NUM_PCS_TO_DISPLAY = 3;
    private static final long serialVersionUID = -466958848014180520L;

    private Long eeid;

    private boolean hasCorrMat = false;

    private boolean hasCorrDist = false;

    private boolean hasPCA = false;

    private boolean hasMeanVariance = false;

    private String eeManagerId = "";

    @SuppressWarnings("unused")
    private boolean hasNodeDegreeDist = false;

    private int numFactors = 2;

    private int numOutliersRemoved = 0;

    private int numPossibleOutliers = 0;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        try {
            pageContext.getOut().print( getQChtml() );
        } catch ( Exception ex ) {
            throw new JspException( "experiment QC tag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    public String getQChtml() {
        StringBuilder buf = new StringBuilder();

        /*
         * check if the files are available...if not, show something intelligent.
         */

        buf.append( "<div class=\"eeqc\" id=\"eeqc\">" );
        buf.append( "<table border=\"0\" cellspacing=\"4\" style=\"background-color:#DDDDDD\" >" );

        buf.append( "<tr><th valign=\"top\" align=\"center\"><strong>Sample correlation</strong></th>"
                + "<th valign=\"top\" align=\"center\"><strong>PCA Scree</strong></th>"
                + "<th valign=\"top\" align=\"center\"><strong>PCA+Factors</strong></th>"
                // + "<th valign=\"top\" align=\"center\"><strong>Node degree</strong></th>"
                + "<th valign=\"top\" align=\"center\"><strong>Gene correlation</strong</th>"
                + "<th valign=\"top\" align=\"center\"><strong>Mean-Variance</strong</th>" + "</tr>" );

        buf.append( "<tr>" );

        String placeHolder = "<td  style=\"margin:3px;padding:8px;background-color:#EEEEEE\" valign='top'>Not available</td>";

        if ( hasCorrMat ) {

            /*
             * popupImage is defined in ExpressinExperimentDetails.js
             */
            int width = 400;
            int height = 400;
            String bigImageUrl = "visualizeCorrMat.html?id=" + this.eeid + "&size=4&forceShowLabels=1";
            buf.append( "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><a style='cursor:pointer' "
                    + "onClick=\"popupImage('"
                    + bigImageUrl
                    + "',"
                    + width
                    + ","
                    + height
                    + ")"
                    + ";return 1\"; "
                    + "title=\"Assay correlations (bright=higher); click for larger version\" >"
                    + "<img src=\"/Gemma/expressionExperiment/visualizeCorrMat.html?id="
                    + this.eeid
                    + "&size=1\" alt='Image unavailable' width='"
                    + ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX
                    + "' height='"
                    + ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX + "' /></a>" );

            buf.append( "<li><a title=\"Download a file containing the raw correlation matrix data\" class=\"newpage\"  target=\"_blank\"  href=\"/Gemma/expressionExperiment/visualizeCorrMat.html?id="
                    + this.eeid + "&text=1\">Get data</a></li>" );

            if ( this.numOutliersRemoved > 0 ) {
                buf.append( "<li><a title=\"Download a file containing the list of outlier samples that were removed\" class=\"newpage\"  target=\"_blank\"  href=\"/Gemma/expressionExperiment/outliersRemoved.html?id="
                        + this.eeid + "&text=1\">" + this.numOutliersRemoved + " outliers removed</a></li>" );
            } else {
                buf.append( "<li>No outliers removed</li>" );
            }

            if ( this.numPossibleOutliers > 0 ) {
                buf.append( "<li><a title=\"Download a file containing the list of possible outlier samples\" class=\"newpage\"  target=\"_blank\"  href=\"/Gemma/expressionExperiment/possibleOutliers.html?id="
                        + this.eeid + "&text=1\">" + this.numPossibleOutliers + " possible outliers</a></li>" );
            } else {
                buf.append( "<li>No outliers detected</li>" );
            }

            buf.append( "</ul></td>" );
        } else {
            buf.append( placeHolder );
        }

        if ( hasPCA ) {
            buf.append( "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img title='PCA Scree' src=\"/Gemma/expressionExperiment/pcaScree.html?id="
                    + this.eeid + "\" />" );
            buf.append( "<br/>" );
            for ( int i = 0; i < NUM_PCS_TO_DISPLAY; i++ ) {
                // id : 'ee-details-panel - declared in ExpressionExperimentPage.js
                String linkText = "<span style='cursor:pointer' onClick=\"Ext.getCmp('" + eeManagerId
                        + "').visualizePcaHandler(" + this.eeid + "," + ( i + 1 ) + "," + 100
                        + ")\" title=\"Click to visualize top loaded probes for component " + ( i + 1 )
                        + "\"><img src=\"/Gemma/images/icons/chart_curve.png\"></span>";
                buf.append( linkText + "&nbsp;" );
            }

            buf.append( "&nbsp;&nbsp;&nbsp;<span><a title=\"Download a file containing the raw eigengenes\" class=\"newpage\"  target=\"_blank\"  href=\"/Gemma/expressionExperiment/eigenGenes.html?eeid="
                    + this.eeid + "\">Get data</a></span>" );

            buf.append( "</td>" );
            /*
             * popupImage is defined in ExpressinExperimentPage.js
             */
            String detailsUrl = "detailedFactorAnalysis.html?id=" + this.eeid;

            int width = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * numFactors;
            int height = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * NUM_PCS_TO_DISPLAY;

            buf.append( "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'>"
                    + "<a style='cursor:pointer' onClick=\"popupImage('"
                    + detailsUrl
                    + "',"
                    + width
                    + ","
                    + height
                    + ");return 1\" >"
                    + "<img title='Correlations of PCs with experimental factors, click for details' src=\"/Gemma/expressionExperiment/pcaFactors.html?id="
                    + this.eeid + "\" /></a></td>" );

        } else {
            /*
             * Two panels for PCA, so two placeholders.
             */
            buf.append( placeHolder );
            buf.append( placeHolder );
        }

        // if ( hasNodeDegreeDistFile ) {
        // buf
        // .append(
        // "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img title='Gene network node degree distribution' src=\"/Gemma/expressionExperiment/visualizeNodeDegreeDist.html?id="
        // + this.eeid + "\" /></td>" );
        // } else {
        // buf.append( placeHolder );
        // }

        if ( hasCorrDist ) {
            buf.append( " <td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img title='Correlation distribution' src=\"/Gemma/expressionExperiment/visualizeProbeCorrDist.html?id="
                    + this.eeid + "\" /></td>" );
        } else {
            buf.append( placeHolder );
        }

        if ( hasMeanVariance ) {
            /*
             * popupImage is defined in ExpressinExperimentDetails.js
             */
            int scaleLarge = 3;
            int width = scaleLarge * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX + 50;
            int height = scaleLarge * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX + 50;
            String bigImageUrl = "visualizeMeanVariance.html?id=" + this.eeid + "&size=" + scaleLarge;
            buf.append( "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><a style='cursor:pointer' "
                    + "onClick=\"popupImage('"
                    + bigImageUrl
                    + "',"
                    + width
                    + ","
                    + height
                    + ")"
                    + ";return 1\"; "
                    + "title=\"Mean-variance relationhip; click for larger version\" >"
                    + "<img src=\"/Gemma/expressionExperiment/visualizeMeanVariance.html?id="
                    + this.eeid
                    + "&size=1\" alt='Image unavailable' width='"
                    + ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX
                    + "' height='"
                    + ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX + "' /></a>" );

            // buf.append(
            // "<li><a title=\"Download a file containing the data means and variances \" class=\"newpage\"  target=\"_blank\"  href=\"/Gemma/expressionExperiment/visualizeMeanVariance.html?id="
            // + this.eeid + "&text=1\">Get data</a></li>" );

            buf.append( "</ul></td>" );
        } else {
            buf.append( placeHolder );
        }

        buf.append( "</tr></table></div>" );
        return buf.toString();
    }

    /**
     * The id of the EE to display QC info
     * 
     * @param id
     */
    public void setEe( Long id ) {
        this.eeid = id;
    }

    public void setEeManagerId( String eeManagerId ) {
        this.eeManagerId = eeManagerId;
    }

    /**
     * @param value
     */
    public void setHasCorrDist( boolean value ) {
        this.hasCorrDist = value;
    }

    /**
     * @param value
     */
    public void setHasCorrMat( boolean value ) {
        this.hasCorrMat = value;
    }

    /**
     * @param value
     */
    public void setHasMeanVariance( boolean value ) {
        this.hasMeanVariance = value;
    }

    public void setHasNodeDegreeDist( boolean value ) {
        this.hasNodeDegreeDist = value;
    }

    /**
     * @param value
     */
    public void setHasPCA( boolean value ) {
        this.hasPCA = value;
    }

    /**
     * How many factors (including batch etc) are available for PCA display?
     * 
     * @param value
     */
    public void setNumFactors( int value ) {
        this.numFactors = value;
    }

    /**
     * @param value
     */
    public void setNumPossibleOutliers( int value ) {
        this.numPossibleOutliers = value;
    }

    /**
     * @param value
     */
    public void setNumOutliersRemoved( int value ) {
        this.numOutliersRemoved = value;
    }
}
