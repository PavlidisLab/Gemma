package ubic.gemma.rest.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
                // JSR-310 (java.time.*) — without this, any endpoint returning an Instant /
                // LocalDateTime / etc. throws 400 at serialization time. Hit on /admin/search/indices
                // 2026-05-27 where SearchIndex.lastModified is Instant.
                .registerModule( new JavaTimeModule() )
                // parse and render date as ISO 9601
                .setDateFormat( new StdDateFormat() );
    }
}
