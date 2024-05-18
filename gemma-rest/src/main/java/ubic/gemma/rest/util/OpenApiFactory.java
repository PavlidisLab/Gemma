package ubic.gemma.rest.util;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.web.context.ServletConfigAware;

import javax.servlet.ServletConfig;

public class OpenApiFactory extends AbstractFactoryBean<OpenAPI> implements ServletConfigAware {

    private ServletConfig servletConfig;

    @Override
    protected OpenAPI createInstance() throws Exception {
        return new JaxrsOpenApiContextBuilder<>()
                .servletConfig( servletConfig )
                // if we don't set that, it will reuse the default context which is already built, but for some very
                // obscure reason ignores the 'openApi.configuration.location' init parameter. Using a different
                // context identifier will result in a newly, built-from-scratch context.
                .ctxId( "ubic.gemma.web.services.rest" )
                .buildContext( true )
                .read();
    }

    @Override
    public Class<?> getObjectType() {
        return OpenAPI.class;
    }

    public void setServletConfig( ServletConfig servletConfig ) {
        this.servletConfig = servletConfig;
    }
}
