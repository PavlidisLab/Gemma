package ubic.gemma.web.services.rest.util;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Utilities for accessing {@link OpenAPI} definitions at runtime.
 * @author poirigui
 */
public class OpenApiUtils {

    public static OpenAPI getOpenApi() {
        try {
            return new JaxrsOpenApiContextBuilder<>()
                    .buildContext( true )
                    .read();
        } catch ( OpenApiConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

}
