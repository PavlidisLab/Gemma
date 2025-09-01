package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.form.TagWriter;

import javax.servlet.jsp.JspException;

/**
 * write a {@code <style/>} tag.
 * @author poirigui
 */
public class StyleTag extends AbstractStaticAssetTag {

    public StyleTag() {
        super( "href" );
    }

    @Override
    protected int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );
        tagWriter.startTag( "link" );
        writeSrcAttribute( tagWriter );
        tagWriter.writeAttribute( "rel", "stylesheet" );
        writeOptionalAttributes( tagWriter );
        tagWriter.endTag();
        return SKIP_BODY;
    }
}
