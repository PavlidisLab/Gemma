package ubic.gemma.rest.util;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.context.ServletConfigAware;

import javax.servlet.ServletConfig;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Factory for {@link OpenAPI}.
 * <p>
 * The singleton is managed by {@link OpenApiContextLocator} and identified by the contextId argument.
 */
@Setter
public class OpenApiFactory implements FactoryBean<OpenAPI>, ServletConfigAware {

    /**
     * A unique context identifier for retrieving the OpenAPI context from {@link OpenApiContextLocator}.
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

    public OpenApiFactory( String contextId ) {
        this.contextId = contextId;
    }

    @Override
    public OpenAPI getObject() throws Exception {
        OpenApiContext ctx = OpenApiContextLocator.getInstance().getOpenApiContext( contextId );
        if ( ctx != null ) {
            return ctx.read();
        }
        ctx = new JaxrsOpenApiContextBuilder<>()
                .ctxId( contextId )
                // Swagger will automatically discover our application's resources and register them
                .servletConfig( servletConfig )
                .buildContext( false );
        if ( modelConverters != null ) {
            ctx.setModelConverters( new LinkedHashSet<>( modelConverters ) );
        }
        ctx.init();
        OpenAPI spec = ctx.read();
        if ( servers != null ) {
            spec.servers( servers );
        }
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
}
