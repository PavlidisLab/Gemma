package ubic.gemma.web.util;

import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.servlet.ServletContext;

/**
 * This builder has extras URL-generating capabilities for Web applications.
 * @author poirigui
 */
public class WebEntityUrlBuilder extends EntityUrlBuilder {

    private final String contextPath;

    public WebEntityUrlBuilder( String hostUrl, ServletContext servletContext ) {
        super( hostUrl );
        this.contextPath = servletContext.getContextPath();
        setWebByDefault();
    }

    /**
     * Obtain an {@link EntityUrlChooser} relative to the servlet context path.
     */
    public EntityUrlChooser fromContextPath() {
        return fromBaseUrl( contextPath );
    }

    /**
     * Obtain an {@link EntityUrlChooser} relative to a root URL (i.e. '/').
     * <p>
     * Use this if the context path will be prefixed by a separate mechanism, for example in a
     * {@link org.springframework.web.servlet.view.RedirectView} that is context-relative.
     */
    public EntityUrlChooser fromRoot() {
        return fromBaseUrl( "" );
    }
}
