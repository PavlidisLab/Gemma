package ubic.gemma.rest.providers;

import org.glassfish.jersey.message.GZipEncoder;
import ubic.gemma.rest.annotations.GZIP;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Arrays;

/**
 * Automagically add the {@code Content-Encoding: gzip} header to endpoints annotated with {@link GZIP}.
 * <p>
 * The compression is handled by {@link GZipEncoder}.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class GzipHeaderDecorator implements WriterInterceptor {

    @Override
    public void aroundWriteTo( WriterInterceptorContext context ) throws WebApplicationException, IOException {
        boolean hasGzipAnnotation = Arrays.stream( context.getAnnotations() )
                .anyMatch( a -> a instanceof GZIP && !( ( GZIP ) a ).alreadyCompressed() );
        if ( hasGzipAnnotation || isOpenApiSpec( context.getEntity() ) ) {
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
