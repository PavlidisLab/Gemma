package ubic.gemma.web.util;

import javax.servlet.ServletContext;

/**
 * Serve static assets from the servlet context.
 * @author poirigui
 */
public class ServletStaticAssetServer implements StaticAssetServer {

    private final ServletContext servletContext;

    public ServletStaticAssetServer( ServletContext servletContext ) {
        this.servletContext = servletContext;
    }

    @Override
    public String getBaseUrl() {
        return servletContext.getContextPath();
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public String getLaunchInstruction() {
        return "";
    }
}
