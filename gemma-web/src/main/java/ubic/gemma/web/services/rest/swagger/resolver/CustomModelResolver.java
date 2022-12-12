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
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.web.services.rest.SearchWebService;
import ubic.gemma.web.services.rest.util.args.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final BeanFactory beanFactory;

    /**
     * Cached descriptions.
     */
    private final Map<String, String> descriptionsCache = new HashMap<String, String>();

    @Autowired
    public CustomModelResolver( @Qualifier("swaggerObjectMapper") ObjectMapper objectMapper, SearchService searchService, BeanFactory beanFactory ) {
        super( objectMapper );
        this.searchService = searchService;
        this.beanFactory = beanFactory;
    }

    @Override
    public Schema resolve( AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain ) {
        JavaType t = Json.mapper().constructType( type.getType() );
        if ( FilterArg.class.isAssignableFrom( t.getRawClass() ) ) {
            type.resolveAsRef( false );
            return super.resolve( type, context, chain ).type( "string" ).properties( null );
        } else if ( Arg.class.isAssignableFrom( t.getRawClass() ) ) {
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
            return super.resolve( type, context, chain );
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
        String description = super.resolveDescription( a, annotations, schema );

        // append available properties to the description
        if ( FilterArg.class.isAssignableFrom( a.getRawType() ) && getGemmaExtensionProperty( schema, "filteringService" ) != null ) {
            String availableProperties = resolveAvailableProperties( schema );
            return description == null ? availableProperties : description + "\n\n" + availableProperties;
        }

        return description;
    }

    private String resolveAvailableProperties( io.swagger.v3.oas.annotations.media.Schema schema ) {
        String filteringServiceName = Objects.requireNonNull( getGemmaExtensionProperty( schema, "filteringService" ),
                "A FilterArg must have a x-gemma-filtering-service extension field to resolve its available values." );
        FilteringService<?> filteringService = beanFactory.getBean( filteringServiceName, FilteringService.class );
        return String.format( "Available properties:\n\n%s.",
                filteringService.getFilterableProperties().stream().map( p -> String.format( "- %s %s", p, filteringService.getFilterablePropertyType( p ).getSimpleName() ) ).collect( Collectors.joining( "\n" ) ) );
    }

    /**
     * Retrieve the value of an OpenAPI Gemma extension property.
     * <p>
     * Gemma extensions appear as {@code x-gemma-{property}} in the generated specification.
     */
    private static String getGemmaExtensionProperty( io.swagger.v3.oas.annotations.media.Schema schema, String property ) {
        return Stream.of( schema.extensions() )
                .filter( e1 -> "gemma".equals( e1.name() ) )
                .findFirst()
                .flatMap( e -> Stream.of( e.properties() )
                        .filter( p -> property.equals( p.name() ) )
                        .map( ExtensionProperty::value )
                        .findFirst() )
                .orElse( null );
    }
}
