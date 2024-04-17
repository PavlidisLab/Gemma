package ubic.gemma.rest.providers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Render unhandled exceptions.
 * <p>
 * The {@link RequestExceptionLogger} does not always work reliably, and more than often the exception is caught instead
 * by the configured JSP error page.
 */
@Provider
public class UnhandledExceptionMapper extends AbstractExceptionMapper<Throwable> {

    @Override
    protected Response.Status getStatus( Throwable exception ) {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    protected boolean logException( Throwable t ) {
        return true;
    }
}
