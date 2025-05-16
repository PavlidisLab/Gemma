package ubic.gemma.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import ubic.gemma.rest.annotations.GZIP;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Arrays;

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
        boolean hasGzipAnnotation = Arrays.stream( context.getAnnotations() )
                .filter( a -> a instanceof GZIP )
                .map( a -> ( GZIP ) a )
                .anyMatch( a -> isApplicable( context, a ) );
        if ( hasGzipAnnotation ) {
            context.getHeaders().putSingle( "Content-Encoding", "gzip" );
        }
        context.proceed();
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
