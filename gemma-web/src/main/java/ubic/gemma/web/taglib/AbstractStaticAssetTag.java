package ubic.gemma.web.taglib;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.web.util.StaticAssetServer;

import javax.servlet.jsp.JspException;

/**
 * Base class for tags that refers to static assets.
 * @author poirigui
 * @see StaticAssetServer
 */
@CommonsLog
public abstract class AbstractStaticAssetTag extends RequestContextAwareTag {

    private StaticAssetServer staticAssetServer;

    /**
     * Write a URL attribute for a static asset.
     */
    protected void writeStaticAssetAttribute( String attributeName, String src, TagWriter tagWriter ) throws JspException {
        if ( staticAssetServer == null ) {
            staticAssetServer = getRequestContext().getWebApplicationContext().getBean( StaticAssetServer.class );
        }
        tagWriter.writeAttribute( attributeName, staticAssetServer.resolveUrl( src ) );
    }
}
