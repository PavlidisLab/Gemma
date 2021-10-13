package ubic.gemma.web.services.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.web.services.rest.util.ResponseDataObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Compress certain JSON payloads with GZIP.
 *
 * The detection mechanism is really minimal for now, but it should be improved.
 */
@Provider
@CommonsLog
public class GzipWriterInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo( WriterInterceptorContext writerInterceptorContext ) throws IOException, WebApplicationException {
        Object entity = writerInterceptorContext.getEntity();
        if ( entity instanceof ResponseDataObject && ( ( ResponseDataObject ) entity ).getData() instanceof ExpressionAnalysisResultSetValueObject ) {
            writerInterceptorContext.getHeaders().putSingle( "Content-Encoding", "gzip" );
            writerInterceptorContext.setOutputStream( new GZIPOutputStream( writerInterceptorContext.getOutputStream() ) );
            log.info( "Will compress payload with gzip!" );
        }
        writerInterceptorContext.proceed();
    }
}
