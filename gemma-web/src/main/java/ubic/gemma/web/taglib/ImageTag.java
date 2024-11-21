package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.form.TagWriter;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

import static ubic.gemma.web.taglib.TagWriterUtils.writeAttributes;

/**
 * Write an {@code <img/>} tag.
 * @author poirigui
 */
public class ImageTag extends AbstractStaticAssetTag implements DynamicAttributes {

    private String src;

    @Nullable
    private String alt;

    @Nullable
    private String height;

    @Nullable
    private String width;

    @Nullable
    private String cssClass;

    @Nullable
    private String cssStyle;

    private final Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    @Override
    protected int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );
        tagWriter.startTag( "img" );
        writeStaticAssetAttribute( "src", src, tagWriter );
        tagWriter.writeOptionalAttributeValue( "alt", htmlEscape( alt ) );
        tagWriter.writeOptionalAttributeValue( "height", height );
        tagWriter.writeOptionalAttributeValue( "width", width );
        tagWriter.writeOptionalAttributeValue( "class", cssClass );
        tagWriter.writeOptionalAttributeValue( "style", cssStyle );
        writeAttributes( dynamicAttributes, isHtmlEscape(), tagWriter );
        tagWriter.endTag();
        return SKIP_BODY;
    }

    public void setSrc( String src ) {
        this.src = src;
    }

    public void setAlt( @Nullable String alt ) {
        this.alt = alt;
    }

    public void setHeight( @Nullable String height ) {
        this.height = height;
    }

    public void setWidth( @Nullable String width ) {
        this.width = width;
    }

    public void setCssClass( @Nullable String cssClass ) {
        this.cssClass = cssClass;
    }

    public void setCssStyle( @Nullable String cssStyle ) {
        this.cssStyle = cssStyle;
    }

    @Override
    public void setDynamicAttribute( String uri, String localName, Object value ) {
        dynamicAttributes.put( localName, value );
    }
}
