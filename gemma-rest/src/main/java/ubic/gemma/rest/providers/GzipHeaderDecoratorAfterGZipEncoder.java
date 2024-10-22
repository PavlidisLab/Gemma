package ubic.gemma.rest.providers;

import ubic.gemma.rest.annotations.GZIP;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

/**
 * Adds a {@code Content-Encoding: gzip} after the context has been intercepted by {@link org.glassfish.jersey.message.GZipEncoder}.
 * @author poirigui
 */
@Provider
@Priority(Priorities.ENTITY_CODER + 10)
public class GzipHeaderDecoratorAfterGZipEncoder extends AbstractGzipHeaderDecorator implements WriterInterceptor {

    @Override
    protected boolean isApplicable( WriterInterceptorContext context, GZIP a ) {
        if ( !a.alreadyCompressed() ) {
            // Content-Encoding was added by GzipHeaderDecorator
            return false;
        }
        return super.isApplicable( context, a );
    }
}
