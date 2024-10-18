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

import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentQCController;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author paul
 */
public class ExperimentQCTag extends TagSupport {

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

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

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

        String contextPath = pageContext.getServletContext().getContextPath();

        /*
         * check if the files are available...if not, show something intelligent.
         */

        buf.append( "<div class=\"eeqc\" id=\"eeqc\">" );
        buf.append( "<table border=\"0\" cellspacing=\"4\" style=\"background-color:#DDDDDD\" class=\"smaller\" >" );

        buf.append( "<tr><th valign=\"top\" align=\"center\"><strong>Sample correlation</strong></th>"
                + "<th valign=\"top\" align=\"center\"><strong>PCA Scree</strong></th>"
                + "<th valign=\"top\" align=\"center\"><strong>PCA+Factors</strong></th>"
                // + "<th valign=\"top\" align=\"center\"><strong>Node degree</strong></th>"
               // + "<th valign=\"top\" align=\"center\"><strong>Gene correlation</strong</th>"
                + "<th valign=\"top\" align=\"center\"><strong>Mean-Variance</strong</th>" + "</tr>" );

        buf.append( "<tr>" );

        String placeHolder = "<td  style=\"margin:3px;padding:8px;background-color:#EEEEEE\" valign='top'>Not available</td>";

        if ( hasCorrMat ) {

            /*
             * popupImage is defined in ExpressionExperimentDetails.js
             */
            int width = 400;
            int height = 400;
            String bigImageUrl = "visualizeCorrMat.html?id=" + this.eeid + "&size=4&forceShowLabels=1";
            buf.append(
                    "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><a style='cursor:pointer' "
                            + "onClick=\"popupImage('" ).append( bigImageUrl ).append( "'," ).append( width )
                    .append( "," ).append( height ).append( ")" ).append( ";return 1\"; " )
                    .append( "title=\"Assay correlations (bright=higher); click for larger version\" >" )
                    .append( "<img src=\"" + contextPath + "/expressionExperiment/visualizeCorrMat.html?id=" ).append( this.eeid )
                    .append( "&size=1\" alt='Image unavailable' width='" )
                    .append( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ).append( "' height='" )
                    .append( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ).append( "' /></a>" );

            buf.append(
                    "<li><a title=\"Download a file containing the raw correlation matrix data\" class=\"newpage\"  target=\"_blank\"  href=\"" + contextPath + "/expressionExperiment/visualizeCorrMat.html?id=" )
                    .append( this.eeid ).append( "&text=1\">Get data</a></li>" );

            if ( this.numOutliersRemoved > 0 ) {
                buf.append(
                        "<li><a title=\"Download a file containing the list of outlier samples that were removed\" class=\"newpage\"  target=\"_blank\"  href=\"" + contextPath + "/expressionExperiment/outliersRemoved.html?id=" )
                        .append( this.eeid ).append( "&text=1\">" ).append( this.numOutliersRemoved )
                        .append( " outliers removed</a></li>" );
            } else {
                buf.append( "<li>No outliers removed</li>" );
            }

            if ( this.numPossibleOutliers > 0 ) {
                buf.append(
                        "<li><a title=\"Download a file containing the list of possible outlier samples\" class=\"newpage\"  target=\"_blank\"  href=\"" + contextPath + "/expressionExperiment/possibleOutliers.html?id=" )
                        .append( this.eeid ).append( "&text=1\">" ).append( this.numPossibleOutliers )
                        .append( " possible outliers</a></li>" );
            } else {
                buf.append( "<li>No outliers detected</li>" );
            }

            buf.append( "</ul></td>" );
        } else {
            buf.append( placeHolder );
        }

        if ( hasPCA ) {
            buf.append(
                    "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img title='PCA Scree' src=\"" + contextPath + "/expressionExperiment/pcaScree.html?id=" )
                    .append( this.eeid ).append( "\" />" );
            buf.append( "<br/>" );
            for ( int i = 0; i < NUM_PCS_TO_DISPLAY; i++ ) {
                // id : 'ee-details-panel - declared in ExpressionExperimentPage.js
                String linkText =
                        "<span style='cursor:pointer' onClick=\"Ext.getCmp('" + eeManagerId + "').visualizePcaHandler("
                                + this.eeid + "," + ( i + 1 ) + "," + 100
                                + ")\" title=\"Click to visualize top loaded probes for component " + ( i + 1 )
                                + "\"><img src=\"" + contextPath + "/images/icons/chart_curve.png\"></span>";
                buf.append( linkText ).append( "&nbsp;" );
            }

            buf.append(
                    "&nbsp;&nbsp;&nbsp;<span><a title=\"Download a file containing the raw eigengenes\" class=\"newpage\"  target=\"_blank\"  href=\"" + contextPath + "/expressionExperiment/eigenGenes.html?eeid=" )
                    .append( this.eeid ).append( "\">Get data</a></span>" );

            buf.append( "</td>" );
            /*
             * popupImage is defined in ExpressinExperimentPage.js
             */
            String detailsUrl = "detailedFactorAnalysis.html?id=" + this.eeid;

            int width = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * numFactors;
            int height = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * NUM_PCS_TO_DISPLAY;

            buf.append( "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'>"
                    + "<a style='cursor:pointer' onClick=\"popupImage('" ).append( detailsUrl ).append( "'," )
                    .append( width ).append( "," ).append( height ).append( ");return 1\" >" ).append(
                    "<img title='Correlations of PCs with experimental factors, click for details' src=\"" + contextPath + "/expressionExperiment/pcaFactors.html?id=" )
                    .append( this.eeid ).append( "\" /></a></td>" );

        } else {
            /*
             * Two panels for PCA, so two placeholders.
             */
            buf.append( placeHolder );
            buf.append( placeHolder );
        }

      /*  if ( hasCorrDist ) {
            buf.append(
                    " <td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><img title='Correlation distribution' src=\"" + contextPath + "/expressionExperiment/visualizeProbeCorrDist.html?id=" )
                    .append( this.eeid ).append( "\" /></td>" );
        } else {
            buf.append( placeHolder );
        }*/

        if ( hasMeanVariance ) {
            /*
             * popupImage is defined in ExpressinExperimentDetails.js
             */
            int scaleLarge = 2;
            int width = scaleLarge * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX;
            int height = scaleLarge * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX;
            String bigImageUrl = "visualizeMeanVariance.html?id=" + this.eeid + "&size=" + scaleLarge;
            buf.append(
                    "<td style=\"margin:3px;padding:2px;background-color:#EEEEEE\" valign='top'><a style='cursor:pointer' "
                            + "onClick=\"popupImage('" ).append( bigImageUrl ).append( "'," ).append( width )
                    .append( "," ).append( height ).append( ")" ).append( ";return 1\"; " )
                    .append( "title=\"Mean-variance relationship; click for larger version\" >" )
                    .append( "<img src=\"" + contextPath + "/expressionExperiment/visualizeMeanVariance.html?id=" )
                    .append( this.eeid ).append( "&size=1\" alt='Image unavailable' width='" )
                    .append( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ).append( "' height='" )
                    .append( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ).append( "' /></a>" );

            buf.append( "</ul></td>" );
        } else {
            buf.append( placeHolder );
        }

        buf.append( "</tr></table></div>" );
        return buf.toString();
    }

    /**
     * The id of the EE to display QC info
     */
    public void setEe( Long id ) {
        this.eeid = id;
    }

    public void setEeManagerId( String eeManagerId ) {
        this.eeManagerId = eeManagerId;
    }

    public void setHasCorrDist( boolean value ) {
        this.hasCorrDist = value;
    }

    public void setHasCorrMat( boolean value ) {
        this.hasCorrMat = value;
    }

    public void setHasMeanVariance( boolean value ) {
        this.hasMeanVariance = value;
    }

    public void setHasNodeDegreeDist( boolean value ) {
        this.hasNodeDegreeDist = value;
    }

    public void setHasPCA( boolean value ) {
        this.hasPCA = value;
    }

    /**
     * How many factors (including batch etc) are available for PCA display?
     */
    public void setNumFactors( int value ) {
        this.numFactors = value;
    }

    public void setNumPossibleOutliers( int value ) {
        this.numPossibleOutliers = value;
    }

    public void setNumOutliersRemoved( int value ) {
        this.numOutliersRemoved = value;
    }

}
