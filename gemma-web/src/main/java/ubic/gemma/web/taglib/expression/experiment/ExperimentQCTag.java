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

import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.visualization.SingleCellSparsityHeatmap;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentQCController;
import ubic.gemma.web.taglib.TagWriterUtils;
import ubic.gemma.web.util.StaticAssetServer;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.web.util.JavaScriptUtils.javaScriptEscape;

/**
 * @author paul
 */
@Setter
public class ExperimentQCTag extends HtmlEscapingAwareTag implements DynamicAttributes {

    private StaticAssetServer staticAssetServer;
    private WebEntityUrlBuilder entityUrlBuilder;

    /**
     * Expression experiment to display the QC info for.
     */
    private ExpressionExperiment expressionExperiment;
    /**
     * DOM ID of the manager for the experiment.
     */
    @Nullable
    private String eeManagerId = null;

    /**
     * Indicate if there is a correlation matrix.
     */
    private boolean hasCorrMat = false;
    /**
     * Number of outliers removed.
     */
    private int numOutliersRemoved = 0;
    /**
     * Number of possible outliers.
     */
    private int numPossibleOutliers = 0;

    /**
     * Indicate if there is a PCA.
     */
    private boolean hasPCA = false;
    /**
     * How many factors (including batch etc) are available for PCA display?
     */
    private int numFactors = 2;
    /**
     * Number of principal components to display.
     */
    private int numPcsToDisplay = 3;

    /**
     * Indicate if there is a mean-variance relation.
     */
    private boolean hasMeanVariance = false;

    /**
     * Indicate if the experiment has single-cell data.
     */
    private boolean hasSingleCellData = false;
    /**
     * Heatmap for single-cell data.
     */
    private SingleCellSparsityHeatmap singleCellSparsityHeatmap;

    private Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    private final Boolean htmlEscape;

    @SuppressWarnings("unused")
    public ExperimentQCTag() {
        htmlEscape = null;
    }

    public ExperimentQCTag( boolean htmlEscape ) {
        this.htmlEscape = htmlEscape;
    }

    @Override
    protected boolean isHtmlEscape() {
        if ( htmlEscape != null ) {
            return htmlEscape;
        } else {
            return super.isHtmlEscape();
        }
    }

    @Override
    public void setDynamicAttribute( String uri, String localName, Object value ) {
        dynamicAttributes.put( localName, value );
    }

    @Override
    public int doStartTagInternal() throws JspException {
        if ( staticAssetServer == null ) {
            staticAssetServer = getRequestContext().getWebApplicationContext().getBean( StaticAssetServer.class );
        }
        if ( entityUrlBuilder == null ) {
            entityUrlBuilder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        }
        writeQc( new TagWriter( pageContext ), pageContext.getServletContext().getContextPath() );
        return SKIP_BODY;
    }

    public void writeQc( TagWriter writer, String contextPath ) throws JspException {
        Assert.notNull( staticAssetServer );

        /*
         * check if the files are available...if not, show something intelligent.
         */

        writer.startTag( "div" );
        writer.writeAttribute( "class", "eeqc" );
        writer.writeAttribute( "id", "eeqc" );

        TagWriterUtils.writeAttributes( dynamicAttributes, isHtmlEscape(), writer );

        writer.startTag( "table" );
        writer.writeAttribute( "class", "smaller" );

        writer.startTag( "tr" );

        for ( String header : new String[] {
                "Sample correlation",
                "PCA Scree",
                "PCA+Factors",
                "Mean-Variance"
        } ) {
            writer.startTag( "th" );
            writer.startTag( "strong" );
            writer.appendValue( header );
            writer.endTag(); // </strong>
            writer.endTag(); // </th>
        }

        if ( hasSingleCellData ) {
            writer.startTag( "th" );
            writer.startTag( "strong" );
            writer.appendValue( "Single-cell" );
            writer.endTag(); // </strong>
            writer.endTag(); // </th>
        }

        writer.endTag();

        writer.startTag( "tr" );

        if ( hasCorrMat ) {

            /*
             * popupImage is defined in ExpressionExperimentDetails.js
             */
            int width = 400;
            int height = 400;
            String bigImageUrl = contextPath + "/expressionExperiment/visualizeCorrMat.html?id=" + this.expressionExperiment.getId() + "&size=4&forceShowLabels=1";
            writer.startTag( "td" );

            writer.startTag( "a" );
            writer.writeAttribute( "style", "cursor:pointer" );
            writer.writeAttribute( "onclick", "Gemma.ExpressionExperimentPage.popupImage('" + javaScriptEscape( bigImageUrl ) + "'," + width + "," + height + ");return 1;" );
            writer.writeAttribute( "title", "Assay correlations (bright=higher); click for larger version." );

            writer.startTag( "img" );
            writer.writeAttribute( "style", "image-rendering: pixelated;" );
            writer.writeAttribute( "src", contextPath + "/expressionExperiment/visualizeCorrMat.html?id=" + this.expressionExperiment.getId() + "&size=1" );
            writer.writeAttribute( "alt", "Correlation matrix displayed as a heatmap." );
            writer.writeAttribute( "width", String.valueOf( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ) );
            writer.writeAttribute( "height", String.valueOf( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ) );
            writer.endTag(); // </img>
            writer.endTag(); // </a>

            writer.startTag( "ul" );

            if ( this.numOutliersRemoved > 0 ) {
                writer.startTag( "li" );
                writer.startTag( "a" );
                writer.writeAttribute( "title", "Download a file containing the list of outlier samples that were removed" );
                writer.writeAttribute( "class", "newpage" );
                writer.writeAttribute( "href", contextPath + "/expressionExperiment/outliersRemoved.html?id=" + this.expressionExperiment.getId() + "&text=1" );
                writer.appendValue( this.numOutliersRemoved + " outliers removed" );
                writer.endTag(); // </a>
                writer.endTag(); // </li>
            }

            if ( this.numPossibleOutliers > 0 ) {
                writer.startTag( "li" );
                writer.startTag( "a" );
                writer.writeAttribute( "title", "Download a file containing the list of possible outlier samples" );
                writer.writeAttribute( "class", "newpage" );
                writer.writeAttribute( "href", contextPath + "/expressionExperiment/possibleOutliers.html?id=" + this.expressionExperiment.getId() + "&text=1" );
                writer.appendValue( this.numPossibleOutliers + " possible outliers" );
                writer.endTag(); // </a>
                writer.endTag(); // </li>
            }

            if ( numOutliersRemoved == 0 && numPossibleOutliers == 0 ) {
                writer.appendValue( "<li><i>No outliers removed nor detected.</i></li>" );
            } else if ( numOutliersRemoved == 0 ) {
                writer.appendValue( "<li><i>No outliers removed.</i></li>" );
            } else if ( numPossibleOutliers == 0 ) {
                writer.appendValue( "<li><i>No outliers detected.</i></li>" );
            }

            writer.startTag( "li" );
            writer.startTag( "a" );
            writer.writeAttribute( "title", "Download a file containing the raw correlation matrix data" );
            writer.writeAttribute( "class", "newpage" );
            writer.writeAttribute( "href", contextPath + "/expressionExperiment/visualizeCorrMat.html?id=" + this.expressionExperiment.getId() + "&text=1" );
            writer.appendValue( "Download correlation matrix" );
            writer.endTag(); // </a>
            writer.endTag(); // </li>

            writer.endTag(); // </ul>
            writer.endTag(); // </td>
        } else {
            writePlaceholder( writer );
        }

        if ( hasPCA ) {
            writer.startTag( "td" );
            writer.startTag( "img" );
            writer.writeAttribute( "src", contextPath + "/expressionExperiment/pcaScree.html?id=" + this.expressionExperiment.getId() );
            writer.writeAttribute( "alt", "Bar plot of the top 10 PCA components." );
            writer.endTag(); // </img>

            if ( eeManagerId != null ) {
                writer.startTag( "br" );
                writer.endTag(); // </br>
                for ( int i = 0; i < numPcsToDisplay; i++ ) {
                    if ( i > 0 ) {
                        writer.appendValue( "&nbsp;" );
                    }
                    // id : 'ee-details-panel - declared in ExpressionExperimentPage.js
                    int component = ( i + 1 );
                    writer.startTag( "span" );
                    writer.writeAttribute( "style", "cursor:pointer" );
                    writer.writeAttribute( "onclick", "Ext.getCmp('" + eeManagerId + "').visualizePcaHandler(" + this.expressionExperiment.getId() + "," + component + "," + 100 + ")" );
                    writer.writeAttribute( "title", "Visualize top loaded probes for component #" + component );
                    writer.startTag( "img" );
                    writer.writeAttribute( "src", staticAssetServer.resolveUrl( "/images/icons/chart_curve.png" ) );
                    writer.endTag(); // </img>
                    writer.endTag(); // </span>
                }
            }

            writer.startTag( "br" );
            writer.endTag();

            writer.startTag( "a" );
            writer.writeAttribute( "title", "Download a file containing the raw eigengenes" );
            writer.writeAttribute( "class", "newpage" );
            writer.writeAttribute( "href", contextPath + "/expressionExperiment/eigenGenes.html?eeid=" + this.expressionExperiment.getId() );
            writer.appendValue( "Download eigengenes" );
            writer.endTag(); // </a>

            writer.endTag(); // </td>

            /*
             * popupImage is defined in ExpressionExperimentPage.js
             */
            String detailsUrl = contextPath + "/expressionExperiment/detailedFactorAnalysis.html?id=" + this.expressionExperiment.getId();

            int width = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * numFactors;
            int height = ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX * numPcsToDisplay;

            writer.startTag( "td" );
            writer.startTag( "a" );
            writer.writeAttribute( "style", "cursor:pointer" );
            writer.writeAttribute( "onclick", "Gemma.ExpressionExperimentPage.popupImage('" + javaScriptEscape( detailsUrl ) + "'," + width + "," + height + ");return 1" );
            writer.writeAttribute( "title", "Correlations of PCs with experimental factors; click for details." );
            writer.startTag( "img" );
            writer.writeAttribute( "src", contextPath + "/expressionExperiment/pcaFactors.html?id=" + this.expressionExperiment.getId() );
            writer.writeAttribute( "alt", "Bar plot of the association between the factors and the top 3 PCA components." );
            writer.endTag(); // </img>
            writer.endTag(); // </a>
            writer.endTag(); // </td>
        } else {
            /*
             * Two panels for PCA, so two placeholders.
             */
            writePlaceholder( writer );
            writePlaceholder( writer );
        }

        if ( hasMeanVariance ) {
            /*
             * popupImage is defined in ExpressinExperimentDetails.js
             */
            int scaleLarge = 2;
            int width = scaleLarge * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX;
            int height = scaleLarge * ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX;
            String bigImageUrl = contextPath + "/expressionExperiment/visualizeMeanVariance.html?id=" + this.expressionExperiment.getId() + "&size=" + scaleLarge;
            writer.startTag( "td" );
            writer.startTag( "a" );
            writer.writeAttribute( "style", "cursor:pointer" );
            writer.writeAttribute( "onclick", "Gemma.ExpressionExperimentPage.popupImage('" + javaScriptEscape( bigImageUrl ) + "'," + width + "," + height + ");return 1" );
            writer.writeAttribute( "title", "Mean-variance relationship; click for larger version." );
            writer.startTag( "img" );
            writer.writeAttribute( "src", contextPath + "/expressionExperiment/visualizeMeanVariance.html?id=" + this.expressionExperiment.getId() + "&size=1" );
            writer.writeAttribute( "alt", "Plot of the mean-variance relation." );
            writer.writeAttribute( "width", String.valueOf( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ) );
            writer.writeAttribute( "height", String.valueOf( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX ) );
            writer.endTag(); // </img>
            writer.endTag(); // </a>
            writer.endTag(); // </td>
        } else {
            writePlaceholder( writer );
        }

        if ( hasSingleCellData ) {
            writer.startTag( "td" );
            SingleCellSparsityHeatmapTag heatmapTag = new SingleCellSparsityHeatmapTag( contextPath, isHtmlEscape() );
            heatmapTag.setHeatmap( singleCellSparsityHeatmap );
            heatmapTag.setEntityUrlBuilder( entityUrlBuilder );
            heatmapTag.setParent( this );
            heatmapTag.setPageContext( pageContext );
            heatmapTag.setAlt( "Heatmap of the number of cells with at least one expressed gene. The rows correspond to genes and columns to assays.", SingleCellSparsityHeatmap.SingleCellHeatmapType.CELL );
            heatmapTag.setAlt( "Heatmap of the number of genes with at least one cell expressing it. The rows correspond to genes and columns to assays.", SingleCellSparsityHeatmap.SingleCellHeatmapType.GENE );
            heatmapTag.setMaxHeight( ExpressionExperimentQCController.DEFAULT_QC_IMAGE_SIZE_PX );
            heatmapTag.writeHeatmap( writer );
            writer.endTag(); // </td>
        }

        writer.endTag(); // </tr>
        writer.endTag(); // </table>

        writer.endTag(); // </div>
    }

    private void writePlaceholder( TagWriter writer ) throws JspException {
        writer.startTag( "td" );
        writer.writeAttribute( "class", "eeqc-na" );
        writer.appendValue( "Not available" );
        writer.endTag(); // </td>
    }
}
