package ubic.gemma.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.rest.util.OpenApiUtils;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedErrorBody;
import ubic.gemma.rest.util.ServletUtils;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Render unhandled exceptions.
 *
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
    protected boolean logException() {
        return true;
    }
}
