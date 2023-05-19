package ubic.gemma.rest.analytics.ga4;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Nullable;

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
