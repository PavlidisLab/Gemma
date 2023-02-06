package ubic.gemma.rest.providers;

import ubic.gemma.rest.annotations.GZIP;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Automagically add the {@code Content-Encoding: gzip} header to endpoints annotated with {@link GZIP}.
 * <p>
 * The compression is handled by {@link org.glassfish.jersey.message.GZipEncoder}.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class GzipHeaderDecorator implements WriterInterceptor {

    @Override
    public void aroundWriteTo( WriterInterceptorContext writerInterceptorContext ) throws WebApplicationException, IOException {
        boolean hasGZipAnnotation = Arrays.stream( writerInterceptorContext.getAnnotations() )
                .map( Annotation::annotationType )
                .anyMatch( GZIP.class::equals );
        if ( hasGZipAnnotation ) {
            writerInterceptorContext.getHeaders().putSingle( "Content-Encoding", "gzip" );
        }
        writerInterceptorContext.proceed();
    }
}
