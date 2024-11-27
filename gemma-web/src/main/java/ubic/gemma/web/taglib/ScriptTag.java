package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.form.TagWriter;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Write a {@code <style/>} tag.
 * @author poirigui
 */
public class ScriptTag extends AbstractStaticAssetTag implements DynamicAttributes {

    private boolean async;
    private boolean defer;
    private final Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    public ScriptTag() {
        super( "src" );
    }

    @Override
    protected int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );
        tagWriter.startTag( "script" );
        writeSrcAttribute( tagWriter );
        TagWriterUtils.writeBooleanAttribute( "async", async, tagWriter );
        TagWriterUtils.writeBooleanAttribute( "defer", defer, tagWriter );
        TagWriterUtils.writeAttributes( dynamicAttributes, isHtmlEscape(), tagWriter );
        tagWriter.endTag( true );
        return SKIP_BODY;
    }

    public void setAsync( boolean async ) {
        this.async = async;
    }

    public void setDefer( boolean defer ) {
        this.defer = defer;
    }

    @Override
    public void setDynamicAttribute( String uri, String localName, Object value ) {
        this.dynamicAttributes.put( localName, value );
    }
}
