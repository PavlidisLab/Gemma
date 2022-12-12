package ubic.gemma.web.services.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import io.swagger.v3.core.util.Json;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure the various beans injected in Swagger's components relating to Jackson's JSON serialization.
 *
 * @author poirigui
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // parse and render date as ISO 9601
                .setDateFormat( new StdDateFormat() );
    }

    /**
     * This is the ObjectMapper used by Swagger to generate the /openapi.json endpoint. It is defined here so that it
     * can be accessed from {@link ubic.gemma.web.services.rest.swagger.resolver.CustomModelResolver}.
     */
    @Bean
    public ObjectMapper swaggerObjectMapper() {
        return Json.mapper();
    }
}
