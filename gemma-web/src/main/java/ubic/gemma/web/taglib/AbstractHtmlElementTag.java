package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An alternative to Spring's {@link org.springframework.web.servlet.tags.form.AbstractHtmlElementTag} that does not
 * carry the complexity of form data bindings.
 *
 * @author poirigui
 */
public abstract class AbstractHtmlElementTag extends HtmlEscapingAwareTag implements DynamicAttributes {

    @Nullable
    private String title;

    @Nullable
    private String cssClass;

    @Nullable
    private String cssStyle;

    private final Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    public void setTitle( @Nullable String title ) {
        this.title = title;
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

    protected void writeOptionalAttributes( TagWriter tagWriter ) throws JspException {
        tagWriter.writeOptionalAttributeValue( "title", htmlEscape( title ) );
        tagWriter.writeOptionalAttributeValue( "class", htmlEscape( cssClass ) );
        tagWriter.writeOptionalAttributeValue( "style", htmlEscape( cssStyle ) );
        TagWriterUtils.writeAttributes( dynamicAttributes, isHtmlEscape(), tagWriter );
    }

    protected String htmlEscape( @Nullable String value ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( value ) : value;
    }
}
