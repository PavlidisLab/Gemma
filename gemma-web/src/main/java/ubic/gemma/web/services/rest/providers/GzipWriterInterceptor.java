package ubic.gemma.web.services.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.web.services.rest.annotations.GZIP;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/**
 * Compress certain JSON payloads with GZIP.
 *
 * TODO: content negotiation
 */
@Provider
@CommonsLog
public class GzipWriterInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo( WriterInterceptorContext writerInterceptorContext ) throws IOException, WebApplicationException {
        log.info( writerInterceptorContext.getAnnotations() );
        boolean hasGZipAnnotation = Arrays.stream( writerInterceptorContext.getAnnotations() )
                .map( Annotation::annotationType )
                .anyMatch( GZIP.class::equals );
        if ( hasGZipAnnotation ) {
            writerInterceptorContext.getHeaders().putSingle( "Content-Encoding", "gzip" );
            writerInterceptorContext.setOutputStream( new GZIPOutputStream( writerInterceptorContext.getOutputStream() ) );
            log.info( "Will compress payload with gzip!" );
        }
        writerInterceptorContext.proceed();
    }
}
