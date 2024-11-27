package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.form.TagWriter;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * write a {@code <style/>} tag.
 * @author poirigui
 */
public class StyleTag extends AbstractStaticAssetTag implements DynamicAttributes {

    private final Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    public StyleTag() {
        super( "href" );
    }

    @Override
    protected int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );
        tagWriter.startTag( "link" );
        writeSrcAttribute( tagWriter );
        tagWriter.writeAttribute( "rel", "stylesheet" );
        TagWriterUtils.writeAttributes( dynamicAttributes, isHtmlEscape(), tagWriter );
        tagWriter.endTag();
        return SKIP_BODY;
    }

    @Override
    public void setDynamicAttribute( String uri, String localName, Object value ) {
        this.dynamicAttributes.put( localName, value );
    }
}
