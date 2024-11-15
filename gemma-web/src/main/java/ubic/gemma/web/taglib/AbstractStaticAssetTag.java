package ubic.gemma.web.taglib;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.web.util.StaticServer;

import javax.servlet.jsp.JspException;

/**
 * Base class for tags that refers to static assets.
 * @author poirigui
 * @see StaticServer
 */
@CommonsLog
public abstract class AbstractStaticAssetTag extends RequestContextAwareTag {

    private String baseUrl;

    /**
     * Write a URL attribute for a static asset.
     */
    protected void writeStaticAssetAttribute( String attributeName, String src, TagWriter tagWriter ) throws JspException {
        Assert.isTrue( src.startsWith( "/" ), "A static asset path must start with '/'." );
        if ( baseUrl == null ) {
            baseUrl = getBaseUrl();
        }
        tagWriter.writeAttribute( attributeName, baseUrl + src );
    }

    /**
     * Obtain the base URL from which static assets are resolved.
     */
    private String getBaseUrl() {
        WebApplicationContext ctx = getRequestContext().getWebApplicationContext();
        StaticServer staticServer = ctx.getBean( StaticServer.class );
        String baseUrl;
        if ( staticServer.isEnabled() ) {
            baseUrl = staticServer.getBaseUrl();
            return baseUrl;
        } else {
            return getRequestContext().getWebApplicationContext().getServletContext().getContextPath();
        }
    }
}
