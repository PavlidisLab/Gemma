package ubic.gemma.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.annotations.GZIPs;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Base class for decorators that handle the {@link GZIP} annotation.
 * @author poirigui
 * @see GZIP
 * @see GzipHeaderDecorator
 * @see GzipHeaderDecoratorAfterGZipEncoder
 */
public abstract class AbstractGzipHeaderDecorator implements WriterInterceptor {

    protected final Log log = LogFactory.getLog( getClass() );

    @Override
    public void aroundWriteTo( WriterInterceptorContext context ) throws IOException, WebApplicationException {
        boolean hasGzipAnnotation = gzipAnnotations( context )
                .anyMatch( a -> isApplicable( context, a ) );
        if ( hasGzipAnnotation ) {
            context.getHeaders().putSingle( "Content-Encoding", "gzip" );
        }
        context.proceed();
    }

    /**
     * Every {@link GZIP} declared on the resource method, flattening the {@link GZIPs} container.
     * <p>
     * A method carrying two {@code @GZIP} annotations reaches us as a single {@code @GZIPs} — the compiler wraps
     * repeated annotations, and {@code getAnnotations()} reports the container rather than its contents. Without
     * the flatten step a per-media-type split is silently ignored and the method gets no compression at all.
     */
    private static Stream<GZIP> gzipAnnotations( WriterInterceptorContext context ) {
        return Arrays.stream( context.getAnnotations() )
                .flatMap( a -> {
                    if ( a instanceof GZIP ) {
                        return Stream.of( ( GZIP ) a );
                    } else if ( a instanceof GZIPs ) {
                        return Arrays.stream( ( ( GZIPs ) a ).value() );
                    } else {
                        return Stream.empty();
                    }
                } );
    }

    /**
     * Check if the given {@link GZIP} annotation applies to the given context.
     */
    protected boolean isApplicable( WriterInterceptorContext context, GZIP a ) {
        if ( a.mediaTypes().length > 0 ) {
            Object contentType = context.getHeaders().getFirst( "Content-Type" );
            if ( contentType == null ) {
                throw new IllegalStateException( "There is no Content-Type header defined, but a media type restriction is set for GZIP compression." );
            }
            MediaType ct = MediaType.valueOf( contentType.toString() );
            return Arrays.stream( a.mediaTypes() )
                    .map( MediaType::valueOf )
                    .anyMatch( mt -> mt.isCompatibleWith( ct ) );
        }
        return true;
    }
}
