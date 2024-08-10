package ubic.gemma.rest.util;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.ServletConfigAware;

import javax.servlet.ServletConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Factory for {@link OpenAPI}.
 * <p>
 * The singleton is managed by {@link OpenApiContextLocator} and identified by the contextId argument.
 */
@Setter
@CommonsLog
public class OpenApiFactory implements FactoryBean<OpenAPI>, ServletConfigAware, DisposableBean, BeanFactoryAware {

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
     * A list of additional model converters to register.
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
    public OpenAPI getObject() throws Exception {
        Assert.state( OpenApiContextLocator.getInstance().getOpenApiContext( contextId ) == ctx,
                "OpenAPI context for " + contextId + " does not match the context managed by this factory, is there another factory involved?" );
        if ( ctx == null ) {
            log.info( "Creating OpenAPI specification..." );
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
        return spec;
    }

    @Override
    public Class<?> getObjectType() {
        return OpenAPI.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() {
        Field map = ReflectionUtils.findField( OpenApiContextLocator.class, "map" );
        ReflectionUtils.makeAccessible( map );
        ( ( Map<?, ?> ) ReflectionUtils.getField( map, OpenApiContextLocator.getInstance() ) )
                .remove( contextId );
        log.info( "OpenAPI context was destroyed." );
    }

    @Override
    public void setBeanFactory( BeanFactory beanFactory ) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
