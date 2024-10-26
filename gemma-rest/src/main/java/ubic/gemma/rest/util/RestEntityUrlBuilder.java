package ubic.gemma.rest.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ubic.gemma.persistence.util.EntityUrlBuilder;

@Component
public class RestEntityUrlBuilder extends EntityUrlBuilder {

    @Autowired
    public RestEntityUrlBuilder( @Value("${gemma.hosturl}") String hostUrl ) {
        super( hostUrl );
        setRestByDefault();
    }

    public EntityUrlChooser fromContextPath() {
        RequestAttributes attr = RequestContextHolder.currentRequestAttributes();
        if ( attr instanceof ServletRequestAttributes ) {
            // happens if attr is a JaxrsServletRequestAttributes, although this is package-protected
            return fromBaseUrl( ( ( ServletRequestAttributes ) RequestContextHolder.currentRequestAttributes() ).getRequest().getContextPath() );
        } else {
            return fromBaseUrl( "" );
        }
    }
}
