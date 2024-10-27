package ubic.gemma.web.util;

import ubic.gemma.persistence.util.EntityUrlBuilder;

public class WebEntityUrlBuilder extends EntityUrlBuilder {

    private final String contextPath;

    public WebEntityUrlBuilder( String hostUrl, String contextPath ) {
        super( hostUrl );
        this.contextPath = contextPath;
        setWebByDefault();
    }

    /**
     * Obtain an {@link EntityUrlChooser} relative to the current context path.
     * @throws IllegalStateException if there is no current request, use {@link #fromBaseUrl(String)} in that case
     * instead
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
