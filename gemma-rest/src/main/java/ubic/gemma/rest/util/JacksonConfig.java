package ubic.gemma.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubic.gemma.rest.serializers.FactorValueBasicValueObjectSerializer;
import ubic.gemma.rest.serializers.FactorValueValueObjectSerializer;

/**
 * Configuration for JSON serialization with Jackson.
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
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // handle special serialization of statements
                .registerModule( new SimpleModule().addSerializer( new FactorValueValueObjectSerializer() ) )
                .registerModule( new SimpleModule().addSerializer( new FactorValueBasicValueObjectSerializer() ) )
                // parse and render date as ISO 9601
                .setDateFormat( new StdDateFormat() );
    }
}
