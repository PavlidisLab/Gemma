package ubic.gemma.rest.swagger.resolver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.rest.SearchWebService;
import ubic.gemma.rest.util.args.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Resolve {@link Arg} parameters' schema.
 * <p>
 * This should always be added last with {@link ModelConverters#addConverter(ModelConverter)} to take priority as it
 * addresses a glitch in the original {@link ModelResolver}.
 *
 * @author poirigui
 */
@Component
@CommonsLog
@ParametersAreNonnullByDefault
public class CustomModelResolver extends ModelResolver {

    @Autowired
    private List<EntityArgService<?, ?>> entityArgServices;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MessageSource messageSource;

    public CustomModelResolver() {
        super( Json.mapper() );
    }

    @Override
    public Schema<?> resolve( AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain ) {
        JavaType t;
        if ( type.getType() instanceof JavaType ) {
            t = ( JavaType ) type.getType();
        } else {
            t = objectMapper().constructType( type.getType() );
        }
        if ( t.isTypeOrSubTypeOf( FilterArg.Filter.class ) || t.isTypeOrSubTypeOf( SortArg.Sort.class ) ) {
            return null; // ignore those...
        } else if ( t.isTypeOrSubTypeOf( FilterArg.class ) || t.isTypeOrSubTypeOf( SortArg.class ) || t.isTypeOrSubTypeOf( QueryArg.class ) ) {
            Schema<?> resolved = super.resolve( type, context, chain );
            String ref = resolved.get$ref();
            // FilterArg and SortArg schemas in parameters are refs to globally-defined schemas and those are
            // unfortunately not holding the right type, so we need to override them
            // see https://github.com/PavlidisLab/Gemma/issues/524 for details
            //noinspection unchecked
            context.getDefinedModels()
                    .get( ref.replaceFirst( "^#/components/schemas/", "" ) )
                    .type( "string" )
                    .properties( null );
            return resolved;
        } else if ( t.isTypeOrSubTypeOf( Arg.class ) && t.getRawClass() != null ) {
            // I'm suspecting there's a bug in Swagger that causes request parameters annotations to shadow the
            // definitions in the class's Schema annotation
            Schema<?> resolvedSchema = super.resolve( new AnnotatedType( t.getRawClass() ), context, chain );
            // There's a bug with abstract class such as TaxonArg and GeneArg that result in the schema containing 'type'
            // and 'properties' fields instead of solely emitting the oneOf
            if ( t.isAbstract() ) {
                //noinspection unchecked
                return resolvedSchema.type( null ).properties( null );
            } else {
                return resolvedSchema;
            }
        } else {
            return super.resolve( type, context, chain );
        }
    }

    /**
     * Resolves allowed values for the {@link ubic.gemma.rest.SearchWebService#search(QueryArg, TaxonArg, PlatformArg, List, LimitArg, ExcludeArg)}
     * resultTypes argument.
     * <p>
     * This ensures that the OpenAPI specification exposes all supported search result types in the {@link SearchService} as
     * allowable values.
     */
    @Override
    protected List<String> resolveAllowableValues( Annotated a, Annotation[] annotations, @Nullable io.swagger.v3.oas.annotations.media.Schema schema ) {
        if ( schema != null && schema.name().equals( SearchWebService.RESULT_TYPES_SCHEMA_NAME ) ) {
            return searchService.getSupportedResultTypes().stream().map( Class::getName ).collect( Collectors.toList() );
        }
        return super.resolveAllowableValues( a, annotations, schema );
    }

    @Override
    protected String resolveDescription( @Nullable Annotated a, Annotation[] annotations, @Nullable io.swagger.v3.oas.annotations.media.Schema schema ) {
        String description = super.resolveDescription( a, annotations, schema );

        // append available properties to the description
        if ( a != null && ( FilterArg.class.isAssignableFrom( a.getRawType() ) || SortArg.class.isAssignableFrom( a.getRawType() ) ) ) {
            String availableProperties = resolveAvailablePropertiesAsString( a );
            return description == null ? availableProperties : description + "\n\n" + availableProperties;
        }

        if ( a != null && QueryArg.class.isAssignableFrom( a.getRawType() ) ) {
            try {
                return ( description != null ? description + "\n\n" : "" )
                        + IOUtils.toString( new ClassPathResource( "/restapidocs/fragments/QueryArg.md" ).getInputStream(), StandardCharsets.UTF_8 )
                        // this part of the template is using embedded HTML in Markdown
                        .replace( "{searchableProperties}", getSearchableProperties().entrySet().stream()
                                .map( e -> "<h2>" + escapeHtml4( e.getKey() ) + "</h2>"
                                        + "<ul>" + e.getValue().stream().map( v -> "<li>" + escapeHtml4( v ) + "</li>" ).collect( Collectors.joining() ) + "</ul>" )
                                .collect( Collectors.joining() ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }

        return description;
    }

    @Override
    protected Map<String, Object> resolveExtensions( @Nullable Annotated a, Annotation[] annotations, @Nullable io.swagger.v3.oas.annotations.media.Schema schema ) {
        Map<String, Object> extensions = super.resolveExtensions( a, annotations, schema );
        if ( a != null && ( FilterArg.class.isAssignableFrom( a.getRawType() ) || SortArg.class.isAssignableFrom( a.getRawType() ) ) ) {
            extensions = extensions != null ? new HashMap<>( extensions ) : new HashMap<>();
            extensions.put( "x-gemma-filterable-properties", resolveAvailableProperties( a ) );
            extensions = Collections.unmodifiableMap( extensions );
        }
        if ( a != null && QueryArg.class.isAssignableFrom( a.getRawType() ) ) {
            extensions = extensions != null ? new HashMap<>( extensions ) : new HashMap<>();
            extensions.put( "x-gemma-searchable-properties", getSearchableProperties() );
            extensions = Collections.unmodifiableMap( extensions );
        }
        return extensions;
    }

    private final Comparator<String> FIELD_COMPARATOR = Comparator
            .comparing( ( String s ) -> StringUtils.countMatches( s, '.' ), Comparator.naturalOrder() )
            .thenComparing( s -> s );

    private Map<String, List<String>> getSearchableProperties() {
        Map<String, List<String>> sp = new HashMap<>();
        for ( Class<? extends Identifiable> resultType : searchService.getSupportedResultTypes() ) {
            List<String> fields = searchService.getFields( resultType ).stream().sorted( FIELD_COMPARATOR ).collect( Collectors.toList() );
            if ( !fields.isEmpty() ) {
                sp.put( resultType.getName(), fields );
            }
        }
        return sp;
    }

    @Value
    private static class FilterablePropMeta {
        String name;
        String type;
        @Nullable
        String description;
        @Nullable
        List<FilterablePropMetaAllowedValue> allowedValues;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        SecurityRequirement security;
    }

    @Value
    private static class FilterablePropMetaAllowedValue {
        String value;
        @Nullable
        String label;
    }


    private List<FilterablePropMeta> resolveAvailableProperties( Annotated a ) {
        // this is the case for FilterArg and SortArg
        JavaType[] candidateServiceTypes = a.getType().findTypeParameters( a.getRawType() );
        //noinspection unchecked
        Class<Identifiable> clazz = ( Class<Identifiable> ) candidateServiceTypes[0].getRawClass();
        // kind of silly, this can be done with Spring 4+ with generic injection
        EntityArgService<?, ?> filteringService = entityArgServices.stream()
                .filter( s -> clazz.isAssignableFrom( s.getElementClass() ) )
                .findAny()
                .orElseThrow( () -> new IllegalArgumentException( String.format( "Could not find filtering service for %s for resolving available properties of %s.", clazz.getName(), a ) ) );
        // FIXME: make this locale-sensitive
        Locale locale = Locale.getDefault();
        return filteringService.getFilterableProperties().stream()
                // FIXME: The Criteria-based services don't support ordering results by collection size, see https://github.com/PavlidisLab/Gemma/issues/520
                .filter( p -> !SortArg.class.isAssignableFrom( a.getRawType() ) || !isCriteriaBased( filteringService ) || !p.endsWith( ".size" ) )
                .sorted()
                .map( p -> new FilterablePropMeta( p,
                        resolveType( SimpleType.constructUnsafe( filteringService.getFilterablePropertyType( p ) ) )
                                + ( filteringService.getFilterablePropertyIsUsingSubquery( p ) ? "[]" : "" ),
                        filteringService.getFilterablePropertyDescription( p ),
                        resolveAllowedValues( filteringService, p, locale ),
                        securityRequirementFromConfigAttributes( filteringService.getFilterablePropertyConfigAttributes( p ) ) ) )
                .collect( Collectors.toList() );
    }

    @Nullable
    private SecurityRequirement securityRequirementFromConfigAttributes( @Nullable Collection<ConfigAttribute> configAttributes ) {
        if ( configAttributes == null ) {
            return null;
        }
        List<String> scopes = configAttributes.stream().map( ConfigAttribute::getAttribute ).collect( Collectors.toList() );
        return new SecurityRequirement()
                .addList( "basicAuth", scopes )
                .addList( "cookieAuth", scopes );
    }

    private List<FilterablePropMetaAllowedValue> resolveAllowedValues( EntityArgService<?, ?> filteringService, String p, Locale locale ) {
        List<Object> allowedValues = filteringService.getFilterablePropertyAllowedValues( p );
        List<MessageSourceResolvable> allowedValuesLabels = filteringService.getFilterablePropertyResolvableAllowedValuesLabels( p );
        if ( allowedValues != null && allowedValuesLabels != null ) {
            assert allowedValues.size() == allowedValuesLabels.size();
            int numValues = allowedValues.size();
            List<FilterablePropMetaAllowedValue> l = new ArrayList<>( numValues );
            for ( int i = 0; i < numValues; i++ ) {
                String title = messageSource.getMessage( allowedValuesLabels.get( i ), locale );
                l.add( new FilterablePropMetaAllowedValue( allowedValues.get( i ).toString(), title ) );
            }
            return l;
        }
        return null;
    }

    private String resolveAvailablePropertiesAsString( Annotated a ) {
        return String.format( "Available properties:\n\n%s",
                resolveAvailableProperties( a ).stream()
                        .map( p -> String.format( "- %s `%s`%s",
                                p.name,
                                p.type,
                                resolvePropDescription( p ) ) )
                        .collect( Collectors.joining( "\n" ) ) );
    }

    private String resolvePropDescription( FilterablePropMeta prop ) {
        StringBuilder desc = new StringBuilder();
        if ( prop.description != null ) {
            desc.append( prop.description );
        }
        if ( prop.allowedValues != null ) {
            if ( desc.length() > 0 )
                desc.append( ", " );
            desc.append( "possible values: " )
                    .append( prop.allowedValues.stream().map( FilterablePropMetaAllowedValue::getValue ).collect( Collectors.joining( ", " ) ) );
        }
        // don't bother with cookie-based authentication
        if ( prop.security != null && prop.security.containsKey( "basicAuth" ) ) {
            if ( desc.length() > 0 )
                desc.append( ", " );
            desc.append( "security: " )
                    .append( String.join( ", ", prop.security.get( "basicAuth" ) ) );
        }
        if ( desc.length() > 0 )
            return " (" + desc + ")";
        else
            return "";
    }

    private static boolean isCriteriaBased( EntityArgService<?, ?> service ) {
        // this is a temporary fix, there's no way to tell if the DAO is implemented with AbstractCriteriaFilteringVoEnabledDao
        // from the service layer
        return service instanceof ExpressionAnalysisResultSetArgService;
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
        } else if ( type.isEnumType() ) {
            // FIXME: handle ordinal enums
            return "string";
        } else if ( type.isArrayType() || type.isCollectionLikeType() ) {
            return "array";
        } else {
            return "object";
        }
    }
}
