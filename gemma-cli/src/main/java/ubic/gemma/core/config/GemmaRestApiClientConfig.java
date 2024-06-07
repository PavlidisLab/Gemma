package ubic.gemma.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.core.util.GemmaRestApiClientImpl;

@Configuration
public class GemmaRestApiClientConfig {

    @Bean
    public GemmaRestApiClient gemmaRestApiClient( @Value("${gemma.hosturl}") String hostUrl ) {
        return new GemmaRestApiClientImpl( hostUrl );
    }
}
