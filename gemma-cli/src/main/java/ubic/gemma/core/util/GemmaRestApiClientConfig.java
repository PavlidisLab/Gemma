package ubic.gemma.core.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GemmaRestApiClientConfig {

    @Bean
    public GemmaRestApiClient gemmaRestApiClient( @Value("${gemma.hosturl}") String hostUrl ) {
        return new GemmaRestApiClientImpl( hostUrl );
    }
}
