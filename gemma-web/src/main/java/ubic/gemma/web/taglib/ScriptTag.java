package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.web.servlet.tags.form.TagWriter;

import javax.servlet.jsp.JspException;

/**
 * Write a {@code <style/>} tag.
 * @author poirigui
 */
@Setter
public class ScriptTag extends AbstractStaticAssetTag {

    private boolean async;
    private boolean defer;

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
        writeOptionalAttributes( tagWriter );
        tagWriter.endTag( true );
        return SKIP_BODY;
    }
}
