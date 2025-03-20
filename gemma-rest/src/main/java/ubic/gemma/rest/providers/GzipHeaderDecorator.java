package ubic.gemma.rest.providers;

import org.glassfish.jersey.message.GZipEncoder;
import ubic.gemma.rest.annotations.GZIP;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

/**
 * Automagically add the {@code Content-Encoding: gzip} header to endpoints annotated with {@link GZIP}.
 * <p>
 * The compression is handled by {@link GZipEncoder}.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class GzipHeaderDecorator extends AbstractGzipHeaderDecorator implements WriterInterceptor {

    @Override
    protected boolean isApplicable( WriterInterceptorContext context, GZIP a ) {
        if ( a.alreadyCompressed() ) {
            // Content-Encoding will be added by GzipHeaderDecoratorAfterGZipEncoder
            return false;
        }
        return super.isApplicable( context, a );
    }
}
