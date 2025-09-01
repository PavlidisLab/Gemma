package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.web.servlet.tags.form.TagWriter;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;

/**
 * Write an {@code <img/>} tag.
 * @author poirigui
 */
@Setter
public class ImageTag extends AbstractStaticAssetTag {

    @Nullable
    private String alt;

    @Nullable
    private String height;

    @Nullable
    private String width;

    public ImageTag() {
        super( "src" );
    }

    @Override
    protected int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );
        tagWriter.startTag( "img" );
        writeSrcAttribute( tagWriter );
        tagWriter.writeOptionalAttributeValue( "alt", htmlEscape( alt ) );
        tagWriter.writeOptionalAttributeValue( "height", height );
        tagWriter.writeOptionalAttributeValue( "width", width );
        writeOptionalAttributes( tagWriter );
        tagWriter.endTag();
        return SKIP_BODY;
    }
}
