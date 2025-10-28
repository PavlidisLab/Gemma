package ubic.gemma.web.taglib;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.web.assets.StaticAssetResolver;

import javax.servlet.jsp.JspException;

/**
 * Base class for tags that refers to static assets.
 * @author poirigui
 * @see StaticAssetResolver
 */
@CommonsLog
public abstract class AbstractStaticAssetTag extends AbstractHtmlElementTag {

    private transient StaticAssetResolver staticAssetResolver;

    private final String srcAttributeName;

    @Setter
    private String src;

    protected AbstractStaticAssetTag( String srcAttributeName ) {
        this.srcAttributeName = srcAttributeName;
    }

    /**
     * Write a URL attribute for a static asset.
     */
    protected void writeSrcAttribute( TagWriter tagWriter ) throws JspException {
        tagWriter.writeAttribute( srcAttributeName, htmlEscape( resolveUrl( src ) ) );
    }

    protected String resolveUrl( String src ) {
        if ( staticAssetResolver == null ) {
            staticAssetResolver = getRequestContext().getWebApplicationContext().getBean( StaticAssetResolver.class );
        }
        return staticAssetResolver.resolveUrl( src );
    }
}
