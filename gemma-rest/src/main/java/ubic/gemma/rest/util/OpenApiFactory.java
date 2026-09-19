package ubic.gemma.rest.util;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.ServletConfigAware;
import ubic.gemma.core.context.AbstractAsyncFactoryBean;

import jakarta.servlet.ServletConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Factory for {@link OpenAPI}.
 * <p>
 * The singleton is managed by {@link OpenApiContextLocator} and identified by the contextId argument.
 */
@Setter
@Slf4j
public class OpenApiFactory extends AbstractAsyncFactoryBean<OpenAPI> implements ServletConfigAware, BeanFactoryAware {

    /**
     * A context identifier for retrieving the OpenAPI context from {@link OpenApiContextLocator}.
     * <p>
     * Use this if you need more than one context or if you use a specific context identifier other than {@link OpenApiContext#OPENAPI_CONTEXT_ID_DEFAULT}.
     */
    private final String contextId;

    /**
     * A list of servers displayed in the specification.
     */
    private List<Server> servers;

    /**
     * A list of model converters to register.
     */
    private List<ModelConverter> modelConverters;

    /**
     * A servlet configuration from which the jax-rs endpoints and resources are discovered.
     */
    private ServletConfig servletConfig;

    private BeanFactory beanFactory;

    private OpenApiContext ctx = null;

    public OpenApiFactory( String contextId ) {
        this.contextId = contextId;
    }

    @Override
    protected OpenAPI createObject() throws Exception {
        Assert.state( OpenApiContextLocator.getInstance().getOpenApiContext( contextId ) == ctx,
                "OpenAPI context for " + contextId + " does not match the context managed by this factory, is there another factory involved?" );
        if ( ctx == null ) {
            log.debug( "Creating OpenAPI specification for ID " + contextId + "..." );
            ctx = new JaxrsOpenApiContextBuilder<>()
                    .ctxId( contextId )
                    // Swagger will automatically discover our application's resources and register them
                    .servletConfig( servletConfig )
                    .buildContext( false );
            if ( modelConverters != null ) {
                ctx.setModelConverters( new LinkedHashSet<>( modelConverters ) );
            }
            ctx.init();
        }
        OpenAPI spec = ctx.read();
        if ( servers != null ) {
            spec.servers( servers );
        }
        OpenAPIVisitor visitor = new OpenAPIVisitor( s -> {
            if ( s != null && s.startsWith( "classpath:" ) ) {
                try {
                    return IOUtils.resourceToString( s.substring( "classpath:".length() ), StandardCharsets.UTF_8 );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else if ( beanFactory instanceof ConfigurableBeanFactory ) {
                return ( ( ConfigurableBeanFactory ) beanFactory ).resolveEmbeddedValue( s );
            } else {
                return s;
            }
        } );
        visitor.visit( spec );
        applyFilterAndSortArgDefaults( spec );
        stripQualityFromResponseMediaTypes( spec );
        return spec;
    }

    /**
     * Surface default values for {@code FilterArg} and {@code SortArg} parameters in the rendered spec.
     * <p>
     * The {@code CustomModelResolver} emits these parameter types as {@code $ref}s to global schemas (so the
     * descriptions and {@code x-gemma-filterable-properties} extensions can be reused across endpoints). In
     * OpenAPI 3.0 a schema object containing {@code $ref} discards sibling keywords, so any {@code default}
     * set on the parameter schema is lost to renderers (Swagger UI, generated clients, etc.) — see
     * https://github.com/PavlidisLab/Gemma/issues/786.
     * <p>
     * Across the REST surface the {@code @DefaultValue} on these parameters is uniform: {@code ""} for filter
     * and {@code "+id"} for sort. Reflect that convention on the parameter object directly (via
     * {@link Parameter#setExample(Object)}) so consumers see the effective default without breaking the
     * shared {@code $ref}.
     */
    private void applyFilterAndSortArgDefaults( OpenAPI spec ) {
        if ( spec.getPaths() == null ) {
            return;
        }
        for ( PathItem pathItem : spec.getPaths().values() ) {
            for ( Operation op : pathItem.readOperations() ) {
                if ( op.getParameters() == null ) {
                    continue;
                }
                for ( Parameter p : op.getParameters() ) {
                    Schema<?> schema = p.getSchema();
                    if ( schema == null || schema.get$ref() == null ) {
                        continue;
                    }
                    String refName = schema.get$ref().replaceFirst( "^#/components/schemas/", "" );
                    if ( refName.startsWith( "FilterArg" ) && p.getExample() == null ) {
                        p.setExample( "" );
                    } else if ( refName.startsWith( "SortArg" ) && p.getExample() == null ) {
                        p.setExample( "+id" );
                    }
                }
            }
        }
    }

    /**
     * Drop the JAX-RS quality-of-source parameter from response media types.
     * <p>
     * A resource that serves both JSON and TSV lowers the TSV branch with
     * {@code @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8 + ";qs=0.9")} so JSON stays the default when the
     * client expresses no preference. That is a server-side negotiation weight and has to stay in the
     * annotation, but swagger-core copies the whole {@code @Produces} string into the response
     * {@code content} map and rewrites {@code qs} to {@code q} on the way, producing keys like
     * {@code text/tab-separated-values; charset=UTF-8; q=0.9}.
     * <p>
     * A {@code q} parameter is not part of a media type in a {@code content} map — it belongs in an
     * {@code Accept} header — so that key matches nothing a client would send, and the same endpoint
     * could carry two spellings of it ({@code ; q=0.9} on one response, {@code ;q=0.9} on another) that
     * no consumer can tell are the same type. Stripping it here rather than per endpoint keeps the
     * annotation honest about negotiation and the spec honest about the wire.
     */
    private void stripQualityFromResponseMediaTypes( OpenAPI spec ) {
        if ( spec.getPaths() == null ) {
            return;
        }
        for ( PathItem pathItem : spec.getPaths().values() ) {
            for ( Operation op : pathItem.readOperations() ) {
                if ( op.getResponses() == null ) {
                    continue;
                }
                for ( ApiResponse response : op.getResponses().values() ) {
                    Content content = response.getContent();
                    if ( content == null ) {
                        continue;
                    }
                    Content normalized = new Content();
                    boolean changed = false;
                    for ( Map.Entry<String, MediaType> entry : content.entrySet() ) {
                        String key = stripQuality( entry.getKey() );
                        changed |= !key.equals( entry.getKey() );
                        // two keys can normalize onto one; the first spelling wins
                        normalized.putIfAbsent( key, entry.getValue() );
                    }
                    if ( changed ) {
                        response.setContent( normalized );
                    }
                }
            }
        }
    }

    /**
     * A media type without its {@code q} / {@code qs} parameter, other parameters (notably
     * {@code charset}) kept and re-joined with the {@code "; "} separator the rest of the spec uses.
     * <p>
     * Public so {@code OpenApiTest} can assert against the same rule the factory applies rather than
     * a second, drifting copy of it.
     */
    public static String stripQuality( String mediaType ) {
        String[] parts = mediaType.split( ";" );
        StringBuilder sb = new StringBuilder( parts[0].trim() );
        for ( int i = 1; i < parts.length; i++ ) {
            String parameter = parts[i].trim();
            int eq = parameter.indexOf( '=' );
            String name = eq >= 0 ? parameter.substring( 0, eq ).trim() : parameter;
            if ( name.equalsIgnoreCase( "q" ) || name.equalsIgnoreCase( "qs" ) ) {
                continue;
            }
            sb.append( "; " ).append( parameter );
        }
        return sb.toString();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    protected void destroyObject( OpenAPI object ) {
        Field map = ReflectionUtils.findField( OpenApiContextLocator.class, "map" );
        ReflectionUtils.makeAccessible( map );
        ( ( Map<?, ?> ) ReflectionUtils.getField( map, OpenApiContextLocator.getInstance() ) )
                .remove( contextId );
        log.debug( "OpenAPI context with ID " + contextId + " was destroyed." );
    }

    @Override
    public void setBeanFactory( BeanFactory beanFactory ) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
