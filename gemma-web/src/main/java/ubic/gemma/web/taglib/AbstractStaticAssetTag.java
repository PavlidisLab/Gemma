package ubic.gemma.web.taglib;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.gemma.web.util.StaticAssetServer;

import javax.servlet.jsp.JspException;

/**
 * Base class for tags that refers to static assets.
 * @author poirigui
 * @see StaticAssetServer
 */
@CommonsLog
public abstract class AbstractStaticAssetTag extends HtmlEscapingAwareTag {

    private StaticAssetServer staticAssetServer;

    /**
     * Write a URL attribute for a static asset.
     */
    protected void writeStaticAssetAttribute( String attributeName, String src, TagWriter tagWriter ) throws JspException {
        if ( staticAssetServer == null ) {
            staticAssetServer = getRequestContext().getWebApplicationContext().getBean( StaticAssetServer.class );
        }
        String url = staticAssetServer.resolveUrl( src );
        tagWriter.writeAttribute( attributeName, htmlEscape( url ) );
    }

    protected String htmlEscape( String value ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( value ) : value;
    }
}
