package ubic.gemma.web.services.rest.swagger.resolver;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.web.services.rest.SearchWebService;
import ubic.gemma.web.services.rest.util.args.Arg;
import ubic.gemma.web.services.rest.util.args.LimitArg;
import ubic.gemma.web.services.rest.util.args.PlatformArg;
import ubic.gemma.web.services.rest.util.args.TaxonArg;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolve {@link Arg} parameters' schema.
 *
 * This should always be added last with {@link ModelConverters#addConverter(ModelConverter)} to take priority as it
 * addresses a glitch in the original {@link ModelResolver}.
 *
 * @author poirigui
 */
@Component
@CommonsLog
@SuppressWarnings("DefaultAnnotationParam")
public class CustomModelResolver extends ModelResolver {

    private final SearchService searchService;

    private final Map<String, String> descriptionsCache = new HashMap<String, String>();

    @Autowired
    public CustomModelResolver( @Qualifier("swaggerObjectMapper") ObjectMapper objectMapper, SearchService searchService ) {
        super( objectMapper );
        this.searchService = searchService;
    }

    @Override
    public Schema resolve( AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain ) {
        JavaType t = Json.mapper().constructType( type.getType() );
        if ( Arg.class.isAssignableFrom( t.getRawClass() ) ) {
            // I'm suspecting there's a bug in Swagger that causes request parameters annotations to shadow the
            // definitions in the class's Schema annotation
            Schema resolvedSchema = super.resolve( new AnnotatedType( ( t.getRawClass() ) ), context, chain );
            // There's a bug with abstract class such as TaxonArg and GeneArg that result in the schema containing 'type'
            // and 'properties' fields instead of solely emiting the oneOf
            if ( Modifier.isAbstract( t.getRawClass().getModifiers() ) ) {
                return resolvedSchema.type( null ).properties( null );
            } else {
                return resolvedSchema;
            }
        } else {
            try {
                return super.resolve( type, context, chain );
            } finally {
                descriptionsCache.clear();
            }
        }
    }

    /**
     * Resolves allowed values for the {@link ubic.gemma.web.services.rest.SearchWebService#search(String, TaxonArg, PlatformArg, List, LimitArg)}
     * resultTypes argument.
     *
     * This ensures that the OpenAPI specification exposes all supported search result types in the {@link SearchService} as
     * allowable values.
     */
    @Override
    protected List<String> resolveAllowableValues( Annotated a, Annotation[] annotations, io.swagger.v3.oas.annotations.media.Schema schema ) {
        if ( schema != null && schema.name().equals( SearchWebService.RESULT_TYPES_SCHEMA_NAME ) ) {
            return searchService.getSupportedResultTypes().stream().map( Class::getName ).collect( Collectors.toList() );
        }
        return super.resolveAllowableValues( a, annotations, schema );
    }


    @Override
    protected String resolveDescription( Annotated a, Annotation[] annotations, io.swagger.v3.oas.annotations.media.Schema schema ) {
        if ( descriptionsCache.containsKey( a.getName() ) ) {
            log.info( String.format( "Got description from cache for %s%n", a ) );
            return descriptionsCache.get( a.getName() );
        }
        ClassPathResource cpr = new ClassPathResource( "restapidocs/models/" + a.getName() + ".md" );
        if ( cpr.exists() ) {
            try ( InputStream in = cpr.getInputStream() ) {
                log.info( String.format( "Resolved description of %s in %s.", a, cpr ) );
                String description = StreamUtils.copyToString( in, StandardCharsets.UTF_8 );
                descriptionsCache.put( a.getName(), description );
                return description;
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return super.resolveDescription( a, annotations, schema );
    }
}
