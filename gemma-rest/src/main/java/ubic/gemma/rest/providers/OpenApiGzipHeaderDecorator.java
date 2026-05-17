package ubic.gemma.rest.providers;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

/**
 * Decorate the OpenAPI spec with a {@code Content-Encoding: gzip} header.
 * @author poirigui
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class OpenApiGzipHeaderDecorator implements WriterInterceptor {

    @Override
    public void aroundWriteTo( WriterInterceptorContext context ) throws WebApplicationException, IOException {
        if ( isOpenApiSpec( context.getEntity() ) ) {
            context.getHeaders().putSingle( "Content-Encoding", "gzip" );
        }
        context.proceed();
    }

    /**
     * This is a hack, but we don't control the endpoint from Swagger's jax-rs integration
     */
    private boolean isOpenApiSpec( Object entity ) {
        return entity instanceof String && ( ( String ) entity ).startsWith( "{\"openapi\"" );
    }
}
