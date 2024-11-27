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

    private final String srcAttributeName;
    private String src;

    protected AbstractStaticAssetTag( String srcAttributeName ) {
        this.srcAttributeName = srcAttributeName;
    }

    public void setSrc( String src ) {
        this.src = src;
    }

    /**
     * Write a URL attribute for a static asset.
     */
    protected void writeSrcAttribute( TagWriter tagWriter ) throws JspException {
        tagWriter.writeAttribute( srcAttributeName, htmlEscape( resolveUrl( src ) ) );
    }

    protected String resolveUrl( String src ) {
        if ( staticAssetServer == null ) {
            staticAssetServer = getRequestContext().getWebApplicationContext().getBean( StaticAssetServer.class );
        }
        return staticAssetServer.resolveUrl( src );
    }

    protected String htmlEscape( String value ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( value ) : value;
    }
}
