package ubic.gemma.rest.analytics.ga4;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Nullable;

/**
 * Strategy that retrieves the user agent from the current request header.
 * @see RequestContextHolder#currentRequestAttributes()
 * @author poirigui
 */
public class RequestBasedUserAgentRetrievalStrategy implements UserAgentRetrievalStrategy {

    @Nullable
    @Override
    public String get() {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        if ( attributes instanceof ServletRequestAttributes ) {
            return ( ( ServletRequestAttributes ) attributes ).getRequest().getHeader( "User-Agent" );
        }
        return null;
    }
}
