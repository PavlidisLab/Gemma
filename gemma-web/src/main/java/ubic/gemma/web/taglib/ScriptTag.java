package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.form.TagWriter;

import javax.servlet.jsp.JspException;

/**
 * Write a {@code <style/>} tag.
 * @author poirigui
 */
public class ScriptTag extends AbstractStaticAssetTag {

    private String src;

    @Override
    protected int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );
        tagWriter.startTag( "script" );
        writeStaticAssetAttribute( "src", src, tagWriter );
        tagWriter.endTag( true );
        return SKIP_BODY;
    }

    public void setSrc( String src ) {
        this.src = src;
    }
}
