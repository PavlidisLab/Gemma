package ubic.gemma.rest.providers;

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
 * Adds a {@code Content-Encoding: gzip} after the context has been intercepted by {@link org.glassfish.jersey.message.GZipEncoder}.
 * @author poirigui
 */
@Provider
@Priority(Priorities.ENTITY_CODER + 10)
public class GzipHeaderDecoratorAfterGZipEncoder implements WriterInterceptor {

    @Override
    public void aroundWriteTo( WriterInterceptorContext context ) throws IOException, WebApplicationException {
        boolean hasGzipAnnotation = Arrays.stream( context.getAnnotations() )
                .anyMatch( a -> a instanceof GZIP && ( ( GZIP ) a ).alreadyCompressed() );
        if ( hasGzipAnnotation ) {
            context.getHeaders().putSingle( "Content-Encoding", "gzip" );
        }
        context.proceed();
    }
}
