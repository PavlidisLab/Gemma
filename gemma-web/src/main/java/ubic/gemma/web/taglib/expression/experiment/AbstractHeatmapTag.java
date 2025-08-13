package ubic.gemma.web.taglib.expression.experiment;

import lombok.Setter;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.visualization.Heatmap;
import ubic.gemma.core.visualization.ChartUtils;
import ubic.gemma.web.taglib.TagWriterUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Setter
public abstract class AbstractHeatmapTag<T extends Heatmap> extends HtmlEscapingAwareTag implements DynamicAttributes {

    private transient BuildInfo buildInfo;

    /**
     * Heatmap to render.
     */
    protected T heatmap;
    /**
     * Alternative text to use for the image.
     */
    protected String alt;
    /**
     * Maximum width of the heatmap in pixels.
     */
    protected int maxWidth = -1;
    /**
     * Maximum height of the heatmap in pixels.
     */
    protected int maxHeight = -1;
    /**
     * Use the resize trick to render the heatmap by generating one pixel per cell and scaling it with CSS.
     */
    protected boolean useResizeTrick = true;
    /**
     * Display row labels.
     */
    protected boolean showXLabels = true;
    /**
     * Display column labels.
     */
    protected boolean showYLabels = true;

    protected final Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    protected BuildInfo getBuildInfo() {
        if ( buildInfo == null ) {
            getRequestContext().getWebApplicationContext().getBean( BuildInfo.class );
        }
        return buildInfo;
    }

    @Override
    public void setDynamicAttribute( String uri, String localName, Object value ) {
        dynamicAttributes.put( localName, value );
    }

    @Override
    protected final int doStartTagInternal() throws Exception {
        writeHeatmap( new TagWriter( pageContext ) );
        return SKIP_BODY;
    }

    protected void writeHeatmap( TagWriter writer ) throws JspException {
        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap" );
        String style = "";
        if ( maxHeight > 0 ) {
            style += "max-height: " + maxHeight + "px; overflow-y: scroll;";
        }
        if ( maxWidth > 0 ) {
            style += "max-width: " + maxWidth + "px;";
        }
        writer.writeOptionalAttributeValue( "style", style );
        TagWriterUtils.writeAttributes( dynamicAttributes, isHtmlEscape(), writer );

        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap-hbox" );

        writeHeatmapYLabelsAndImage( writer );

        if ( showXLabels ) {
            writeXLabels( writer );
        }

        writer.endTag(); // </div> heatmap-hbox

        writer.endTag(); // </div> heatmap
    }

    protected void writeHeatmapYLabelsAndImage( TagWriter writer ) throws JspException {
        writer.startTag( "div" );
        writer.writeAttribute( "class", "heatmap-vbox" );
        if ( maxWidth > 0 ) {
            writer.writeAttribute( "style", "overflow-x: scroll;" );
        }
        if ( showYLabels ) {
            writeYLabels( writer );
        }
        writeHeatmapImage( writer );
        writer.endTag(); // </div> heatmap-vbox
    }

    /**
     * Write the heatmap image.
     * <p>
     * The default implementation creates a data URL.
     */
    protected void writeHeatmapImage( TagWriter writer ) throws JspException {
        String imageUrl;
        int width, height;
        int cellSize = useResizeTrick ? 1 : heatmap.getCellSize();
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            BufferedImage image = heatmap.createImage( cellSize );
            // create a data URL with the image
            ChartUtils.writeBufferedImageAsPNG( baos, image, alt, getBuildInfo() );
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
        }
        writer.startTag( "img" );
        writer.writeAttribute( "src", imageUrl );
        writer.writeAttribute( "class", "heatmap-img" );
        writer.writeOptionalAttributeValue( "alt", htmlEscape( alt ) );
        writer.writeAttribute( "width", String.valueOf( width ) );
        writer.writeAttribute( "height", String.valueOf( height ) );
        writer.endTag();
    }

    protected abstract void writeXLabels( TagWriter writer ) throws JspException;

    protected abstract void writeYLabels( TagWriter writer ) throws JspException;

    protected String htmlEscape( String s ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( s ) : s;
    }
}
