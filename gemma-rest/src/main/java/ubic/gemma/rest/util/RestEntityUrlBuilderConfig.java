package ubic.gemma.rest.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestEntityUrlBuilderConfig {

    @Bean
    public RestEntityUrlBuilder restEntityUrlBuilder( @Value("${gemma.hosturl}") String gemmaHostUrl ) {
        return new RestEntityUrlBuilder( gemmaHostUrl );
    }
}
