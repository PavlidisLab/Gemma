package ubic.gemma.rest.util;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;

import java.util.Collections;

@Configuration
public class OpenApiConfig {

    @Bean
    public FactoryBean<OpenAPI> openApi( CustomModelResolver customModelResolver ) {
        OpenApiFactory factory = new OpenApiFactory( "ubic.gemma.rest" );
        factory.setModelConverters( Collections.singletonList( customModelResolver ) );
        return factory;
    }
}
