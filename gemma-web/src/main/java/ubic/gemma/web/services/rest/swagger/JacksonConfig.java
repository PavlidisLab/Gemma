package ubic.gemma.web.services.rest.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    public ObjectMapper swaggerObjectMapper() {
        return Json.mapper();
    }
}
