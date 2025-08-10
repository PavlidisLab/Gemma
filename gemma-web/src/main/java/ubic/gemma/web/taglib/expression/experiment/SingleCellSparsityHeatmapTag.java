package ubic.gemma.web.taglib.expression.experiment;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.visualization.SingleCellSparsityHeatmap;
import ubic.gemma.corej11.visualization.ChartUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Tag for displaying a single-cell sparsity heatmap.
 * @see SingleCellSparsityHeatmap
 * @author poirigui
 */
@Setter
public class SingleCellSparsityHeatmapTag extends AbstractHeatmapTag<SingleCellSparsityHeatmap> {

    private transient WebEntityUrlBuilder entityUrlBuilder;

    // this is mainly to prevent access to the pageContext which is not set when rendering via DWR
    private final Boolean htmlEscape;
    private final String contextPath;

    private final Map<SingleCellSparsityHeatmap.SingleCellHeatmapType, String> type2Alt = new HashMap<>();

    @SuppressWarnings("unused") /* needed for JSP */ public SingleCellSparsityHeatmapTag() {
        this.contextPath = null;
        this.htmlEscape = null;
    }

    public SingleCellSparsityHeatmapTag( String contextPath, boolean htmlEscape ) {
        this.contextPath = contextPath;
        this.htmlEscape = htmlEscape;
    }

    // needed for JSP
    @Override
    public void setHeatmap( SingleCellSparsityHeatmap heatmap ) {
        super.setHeatmap( heatmap );
    }

    /**
     * Set the alternative text for a specific heatmap type.
     * @see #setAlt(String)
     */
    public void setAlt( String alt, SingleCellSparsityHeatmap.SingleCellHeatmapType type ) {
        type2Alt.put( type, alt );
    }

    private String getContextPath() {
        if ( contextPath != null ) {
            return contextPath;
        } else {
            return pageContext.getServletContext().getContextPath();
        }
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
    protected void writeHeatmapYLabelsAndImage( TagWriter writer ) throws JspException {
        if ( heatmap.getType() != null ) {
            super.writeHeatmapYLabelsAndImage( writer );
            writeAggregatedImage( heatmap.getType(), writer );
        } else {
            // write all possible heatmap types side-by-side
            writer.startTag( "div" );
            writer.writeAttribute( "class", "heatmap-hbox" );
            if ( maxWidth > 0 ) {
                writer.writeAttribute( "style", "overflow-x: scroll;" );
            }
            writeHeatmapYLabelsAndImage( "Cell-level Sparsity", SingleCellSparsityHeatmap.SingleCellHeatmapType.CELL, writer );

            if ( !heatmap.isTranspose() ) {
                writer.startTag( "div" );
                writer.writeAttribute( "class", "heatmap-vbox" );
                writer.startTag( "div" );
                writer.writeAttribute( "class", "heatmap-y-labels" );
                writer.writeAttribute( "style", "line-height: " + heatmap.getCellSize() + "px; font-size: " + ( heatmap.getCellSize() - 2 ) + "px;" );
                writer.startTag( "a" );
                writer.writeAttribute( "href", contextPath + "/expressionExperiment/showAllExpressionExperimentSubSets.html?id=" + heatmap.getExpressionExperiment().getId() + "&dimension=" + heatmap.getDimension().getId() );
                writer.appendValue( "All cell types" );
                writer.endTag();
                writer.endTag();
                writeAggregatedImage( SingleCellSparsityHeatmap.SingleCellHeatmapType.CELL, writer );
                writer.endTag();
            }

            writeHeatmapYLabelsAndImage( "Gene-level Sparsity", SingleCellSparsityHeatmap.SingleCellHeatmapType.GENE, writer );

            if ( !heatmap.isTranspose() ) {
                writer.startTag( "div" );
                writer.writeAttribute( "class", "heatmap-vbox" );
                writer.startTag( "div" );
                writer.writeAttribute( "class", "heatmap-y-labels" );
                writer.writeAttribute( "style", "line-height: " + heatmap.getCellSize() + "px; font-size: " + ( heatmap.getCellSize() - 2 ) + "px;" );
                writer.startTag( "a" );
                writer.writeAttribute( "href", contextPath + "/expressionExperiment/showAllExpressionExperimentSubSets.html?id=" + heatmap.getExpressionExperiment().getId() + "&dimension=" + heatmap.getDimension().getId() );
                writer.appendValue( "All cell types" );
                writer.endTag();
                writer.endTag();
                writeAggregatedImage( SingleCellSparsityHeatmap.SingleCellHeatmapType.GENE, writer );
                writer.endTag();
            }

            writer.endTag();
        }
        if ( heatmap.isTranspose() ) {
            // write aggregated images below
            // here
        }
    }

    private void writeHeatmapYLabelsAndImage( @Nullable String title, SingleCellSparsityHeatmap.SingleCellHeatmapType type, TagWriter writer ) throws JspException {
        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap-vbox" );
        if ( title != null ) {
            writer.startTag( "div" );
            writer.writeAttribute( "style", "text-align: center;" );
            writer.startTag( "strong" );
            writer.appendValue( htmlEscape( title ) );
            writer.endTag();
            writer.endTag();
        }
        if ( showYLabels ) {
            writeYLabels( writer );
        }
        writeHeatmapImage( type, writer );
        writer.endTag(); // </div> // heatmap-vbox
    }

    @Override
    protected void writeHeatmapImage( TagWriter writer ) throws JspException {
        Assert.notNull( heatmap.getType(), "Can only write heatmap images of known type." );
        writeHeatmapImage( heatmap.getType(), writer );
    }

    private void writeHeatmapImage( SingleCellSparsityHeatmap.SingleCellHeatmapType type, TagWriter writer ) throws JspException {
        String imageUrl = getContextPath() + "/expressionExperiment/visualizeSingleCellSparsityHeatmap.html?id=" + heatmap.getExpressionExperiment().getId() + "&type=" + type.name().toLowerCase();
        if ( useResizeTrick ) {
            imageUrl += "&cellSize=1";
        } else {
            imageUrl += "&cellSize=" + heatmap.getCellSize();
        }
        if ( heatmap.isTranspose() ) {
            imageUrl += "&transpose=true";
        }
        int height = heatmap.getCellSize() * heatmap.getXLabels().size();
        int width = heatmap.getCellSize() * heatmap.getYLabels().size();
        writer.startTag( "img" );
        writer.writeAttribute( "class", "heatmap-img" );
        writer.writeAttribute( "src", imageUrl );
        writer.writeAttribute( "height", String.valueOf( height ) );
        writer.writeAttribute( "width", String.valueOf( width ) );
        writer.writeOptionalAttributeValue( "alt", htmlEscape( type2Alt.get( type ) ) );
        writer.endTag(); // </img>
        if ( heatmap.isTranspose() ) {
            // when the heatmap is transposed, aggregated images are displayed below
            writeAggregatedImage( type, writer );
        }
    }

    private void writeAggregatedImage( SingleCellSparsityHeatmap.SingleCellHeatmapType type, TagWriter writer ) throws JspException {
        String imageUrl;
        int height, width;
        int cellSize = heatmap.getCellSize();
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            if ( useResizeTrick ) {
                // resize trick
                heatmap.setCellSize( 1 );
            }
            BufferedImage image = heatmap.createAggregateImage( type );
            heatmap.setCellSize( cellSize ); // restore
            // create a data URL with the image
            // with the resize trick on, DPI must be 0
            ChartUtils.writeBufferedImageAsPNG( baos, image, useResizeTrick ? 0 : dpi, alt, getBuildInfo());
            imageUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString( baos.toByteArray() );
            if ( useResizeTrick ) {
                height = heatmap.getCellSize() * image.getHeight();
                width = heatmap.getCellSize() * image.getWidth();
            } else {
                height = image.getHeight();
                width = image.getWidth();
            }
        } catch ( IOException e ) {
            throw new JspException( e );
        } finally {
            heatmap.setCellSize( cellSize ); // restore
        }
        writer.startTag( "img" );
        writer.writeAttribute( "class", "heatmap-img" );
        writer.writeAttribute( "src", imageUrl );
        writer.writeAttribute( "height", String.valueOf( height ) );
        writer.writeAttribute( "width", String.valueOf( width ) );
        writer.writeOptionalAttributeValue( "alt", htmlEscape( type2Alt.get( type ) ) );
        writer.endTag(); // </img>
    }

    @Override
    protected void writeYLabels( TagWriter writer ) throws JspException {
        if ( entityUrlBuilder == null ) {
            entityUrlBuilder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        }
        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap-y-labels" );
        writer.writeAttribute( "style", "line-height: " + heatmap.getCellSize() + "px; font-size: " + ( heatmap.getCellSize() - 2 ) + "px;" );
        if ( heatmap.isTranspose() ) {
            writeSampleLabels( writer );
        } else {
            writeSubSetLabels( writer );
        }
        writer.endTag();
    }

    @Override
    protected void writeXLabels( TagWriter writer ) throws JspException {
        if ( entityUrlBuilder == null ) {
            entityUrlBuilder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        }
        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap-x-labels" );
        writer.writeAttribute( "style", "line-height: " + heatmap.getCellSize() + "px; font-size: " + ( heatmap.getCellSize() - 2 ) + "px;" );
        if ( heatmap.isTranspose() ) {
            writeSubSetLabels( writer );
            writer.startTag( "br" );
            writer.endTag();
            writer.startTag( "a" );
            writer.writeAttribute( "href", contextPath + "/expressionExperiment/showAllExpressionExperimentSubSets.html?id=" + heatmap.getExpressionExperiment().getId() + "&dimension=" + heatmap.getDimension().getId() );
            writer.appendValue( "All cell types" );
            writer.endTag();
        } else {
            writeSampleLabels( writer );
        }
        writer.endTag();
    }

    private void writeSampleLabels( TagWriter writer ) throws JspException {
        boolean first = true;
        for ( BioAssay ba : heatmap.getSamples() ) {
            if ( !first ) {
                writer.startTag( "br" );
                writer.endTag();
            }
            first = false;
            writer.startTag( "a" );
            writer.writeAttribute( "href", entityUrlBuilder.fromContextPath().entity( ba ).toUriString() + "&dimension=" + heatmap.getDimension().getId() );
            writer.appendValue( htmlEscape( ba.getName() ) );
            writer.endTag();
        }
    }

    private void writeSubSetLabels( TagWriter writer ) throws JspException {
        String commonPrefix = StringUtils.getCommonPrefix( heatmap.getSubSets().stream().map( ExpressionExperimentSubSet::getName ).toArray( String[]::new ) );
        boolean first = true;
        for ( ExpressionExperimentSubSet subSet : heatmap.getSubSets() ) {
            if ( !first ) {
                writer.startTag( "br" );
                writer.endTag();
            }
            first = false;
            writer.startTag( "a" );
            writer.writeAttribute( "href", entityUrlBuilder.fromContextPath().entity( subSet ).toUriString() + "&dimension=" + heatmap.getDimension().getId() );
            if ( !commonPrefix.isEmpty() ) {
                writer.writeAttribute( "title", htmlEscape( subSet.getName() ) );
            }
            writer.appendValue( htmlEscape( subSet.getName().substring( commonPrefix.length() ) ) );
            writer.endTag();
        }
    }
}