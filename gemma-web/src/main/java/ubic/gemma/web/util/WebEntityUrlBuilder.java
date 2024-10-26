package ubic.gemma.web.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ubic.gemma.persistence.util.EntityUrlBuilder;

@Component
public class WebEntityUrlBuilder extends EntityUrlBuilder {

    @Autowired
    public WebEntityUrlBuilder( @Value("${gemma.hosturl}") String hostUrl ) {
        super( hostUrl );
        setWebByDefault();
    }

    /**
     * Obtain an {@link EntityUrlChooser} relative to the current context path.
     * @throws IllegalStateException if there is no current request, use {@link #fromBaseUrl(String)} in that case
     * instead
     */
    public EntityUrlChooser fromContextPath() {
        RequestAttributes attr = RequestContextHolder.currentRequestAttributes();
        if ( attr instanceof ServletRequestAttributes ) {
            return fromBaseUrl( ( ( ServletRequestAttributes ) RequestContextHolder.currentRequestAttributes() ).getRequest().getContextPath() );
        } else {
            return fromBaseUrl( "" );
        }
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
