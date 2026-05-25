package ubic.gemma.rest.util;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
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
