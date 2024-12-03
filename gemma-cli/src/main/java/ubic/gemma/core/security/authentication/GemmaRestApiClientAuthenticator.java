package ubic.gemma.core.security.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.GemmaRestApiClient;

import javax.annotation.Nullable;

@Component
public class GemmaRestApiClientAuthenticator implements CliAuthenticationAware {

    private final GemmaRestApiClient gemmaRestApiClient;

    @Autowired
    public GemmaRestApiClientAuthenticator( GemmaRestApiClient gemmaRestApiClient ) {
        this.gemmaRestApiClient = gemmaRestApiClient;
    }

    @Override
    public void setAuthentication( @Nullable Authentication authentication ) {
        if ( authentication != null ) {
            this.gemmaRestApiClient.setAuthentication( authentication );
        } else {
            this.gemmaRestApiClient.clearAuthentication();
        }
    }
}
