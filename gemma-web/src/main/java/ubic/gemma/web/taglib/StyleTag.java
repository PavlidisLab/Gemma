package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.form.TagWriter;

import javax.servlet.jsp.JspException;

/**
 * write a {@code <style/>} tag.
 * @author poirigui
 */
public class StyleTag extends AbstractStaticAssetTag {

    private String href;

    @Override
    protected int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );
        tagWriter.startTag( "link" );
        writeStaticAssetAttribute( "href", href, tagWriter );
        tagWriter.writeAttribute( "rel", "stylesheet" );
        tagWriter.endTag();
        return SKIP_BODY;
    }

    public void setHref( String href ) {
        this.href = href;
    }
}
