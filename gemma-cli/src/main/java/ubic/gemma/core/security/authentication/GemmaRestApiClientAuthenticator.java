package ubic.gemma.core.security.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.GemmaRestApiClient;

@Component
public class GemmaRestApiClientAuthenticator implements CLIAuthenticationAware {

    private final GemmaRestApiClient gemmaRestApiClient;

    @Autowired
    public GemmaRestApiClientAuthenticator( GemmaRestApiClient gemmaRestApiClient ) {
        this.gemmaRestApiClient = gemmaRestApiClient;
    }

    @Override
    public void setAuthentication( Authentication authentication ) {
        this.gemmaRestApiClient.setAuthentication( authentication );
    }

    @Override
    public void clearAuthentication() {
        this.gemmaRestApiClient.clearAuthentication();
    }
}
