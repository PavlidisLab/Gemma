package ubic.gemma.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import io.swagger.v3.core.util.Json;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ubic.gemma.rest.serializers.FactorValueBasicValueObjectSerializer;
import ubic.gemma.rest.serializers.FactorValueValueObjectSerializer;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;

/**
 * Configure the various beans injected in Swagger's components relating to Jackson's JSON serialization.
 *
 * @author poirigui
 */
@Configuration
public class JacksonConfig {

    /**
     * Mapper used to generate JSON payloads from the REST API.
     *
     * @see ubic.gemma.rest.providers.ObjectMapperResolver
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // handle special serialization of statements
                .registerModule( new SimpleModule().addSerializer( new FactorValueValueObjectSerializer() ) )
                .registerModule( new SimpleModule().addSerializer( new FactorValueBasicValueObjectSerializer() ) )
                // parse and render date as ISO 9601
                .setDateFormat( new StdDateFormat() );
    }

    /**
     * This is the ObjectMapper used by Swagger to generate the /openapi.json endpoint. It is defined here so that it
     * can be accessed from {@link CustomModelResolver}.
     */
    @Bean
    public ObjectMapper swaggerObjectMapper() {
        return Json.mapper();
    }
}
