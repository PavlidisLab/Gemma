package ubic.gemma.web.services.rest.swagger.resolver;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.*;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.web.services.rest.SearchWebService;
import ubic.gemma.web.services.rest.util.args.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
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
public class CustomModelResolver extends ModelResolver {

    private final SearchService searchService;
    private final BeanFactory beanFactory;

    @Autowired
    public CustomModelResolver( @Qualifier("swaggerObjectMapper") ObjectMapper objectMapper, SearchService searchService, BeanFactory beanFactory ) {
        super( objectMapper );
        this.searchService = searchService;
        this.beanFactory = beanFactory;
    }

    @Override
    public Schema resolve( AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain ) {
        JavaType t = Json.mapper().constructType( type.getType() );
        if ( FilterArg.class.isAssignableFrom( t.getRawClass() ) || SortArg.class.isAssignableFrom( t.getRawClass() ) ) {
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
        if ( FilterArg.class.isAssignableFrom( a.getRawType() ) || SortArg.class.isAssignableFrom( a.getRawType() ) ) {
            String availableProperties = resolveAvailableProperties( a, schema );
            return description == null ? availableProperties : description + "\n\n" + availableProperties;
        }

        return description;
    }

    private String resolveAvailableProperties( Annotated a, io.swagger.v3.oas.annotations.media.Schema schema ) {
        String filteringServiceName = getGemmaExtensionProperty( schema, "filteringService" )
                .orElseThrow( () -> new IllegalArgumentException( String.format( "A %s must have an x-gemma extension with the filteringService field to resolve its available values.",
                        a.getRawType().getSimpleName() ) ) );
        FilteringVoEnabledService<?, ?> filteringService = beanFactory.getBean( filteringServiceName, FilteringVoEnabledService.class );
        return String.format( "Available properties:\n\n%s",
                filteringService.getFilterableProperties().stream()
                        // FIXME: The Criteria-based services don't support ordering results by collection size, see https://github.com/PavlidisLab/Gemma/issues/520
                        .filter( p -> !isCriteriaBased( filteringService ) || !p.endsWith( ".size" ) )
                        .sorted()
                        .map( p -> String.format( "- %s `%s`%s",
                                p,
                                resolveType( SimpleType.constructUnsafe( filteringService.getFilterablePropertyType( p ) ) ),
                                filteringService.getFilterablePropertyDescription( p ) != null ? " (" + filteringService.getFilterablePropertyDescription( p ) + ")" : ""
                        ) ).collect( Collectors.joining( "\n" ) ) );
    }

    private static boolean isCriteriaBased( FilteringVoEnabledService<?, ?> service ) {
        // this is a temporary fix, there's no way to tell if the DAO is implemented with AbstractCriteriaFilteringVoEnabledDao
        // from the service layer
        return service instanceof ExpressionAnalysisResultSetService || service instanceof QuantitationTypeService;
    }

    /**
     * Quick 'n dirty conversion, this can also be done more correctly via {@link #resolve(AnnotatedType, ModelConverterContext, Iterator)},
     * but that would be unnecessarily expensive.
     * <p>
     * See <a href="https://swagger.io/docs/specification/data-models/data-types/">Data Types</a> for more details about
     * available OpenAPI data types.
     * <p>
     * TODO: also resolve the format
     */
    private String resolveType( JavaType type ) {
        if ( type.isTypeOrSubTypeOf( Boolean.class ) ) {
            return "boolean";
        } else if ( type.isTypeOrSubTypeOf( Integer.class ) || type.isTypeOrSubTypeOf( Long.class ) ) {
            return "integer";
        } else if ( type.isTypeOrSubTypeOf( Float.class ) || type.isTypeOrSubTypeOf( Double.class ) ) {
            return "number";
        } else if ( type.isTypeOrSubTypeOf( Date.class ) || type.isTypeOrSubTypeOf( String.class ) ) {
            return "string";
        } else if ( type.isArrayType() || type.isCollectionLikeType() ) {
            return "array";
        } else {
            return "object";
        }
    }

    /**
     * Retrieve the value of an OpenAPI Gemma extension property.
     * <p>
     * Gemma extensions appear under {@code x-gemma} in the generated specification.
     */
    private static Optional<String> getGemmaExtensionProperty( io.swagger.v3.oas.annotations.media.Schema schema, String property ) {
        return Stream.of( schema.extensions() )
                .filter( e1 -> "gemma".equals( e1.name() ) )
                .findFirst()
                .flatMap( e -> Stream.of( e.properties() )
                        .filter( p -> property.equals( p.name() ) )
                        .map( ExtensionProperty::value )
                        .findFirst() );
    }
}
