package ubic.gemma.rest.util;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public FactoryBean<OpenAPI> openApi() {
        return new OpenApiFactory();
    }
}
