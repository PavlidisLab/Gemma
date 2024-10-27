package ubic.gemma.persistence.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EntityUrlBuilderConfig {

    @Bean
    @Primary
    public EntityUrlBuilder entityUrlBuilder( @Value("${gemma.hosturl}") String gemmaHostUrl ) {
        return new EntityUrlBuilder( gemmaHostUrl );
    }
}
