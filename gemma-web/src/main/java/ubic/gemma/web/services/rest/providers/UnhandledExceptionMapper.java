package ubic.gemma.web.services.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.ServletUtils;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Render unhandled exceptions.
 *
 * The {@link RequestExceptionLogger} does not always work reliably, and more than often the exception is caught instead
 * by the configured JSP error page.
 */
@Provider
@CommonsLog
public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private HttpServletRequest request;

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( Throwable throwable ) {
        log.error( "Unhandled exception was raised for " + ServletUtils.summarizeRequest( request ) + ".", throwable );
        Response.Status code = Response.Status.INTERNAL_SERVER_ERROR;
        // we don't display the stacktrace for this kind of exception
        WellComposedErrorBody errorBody = new WellComposedErrorBody( code, throwable.getMessage() );
        return Response.status( code )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .entity( new ResponseErrorObject( errorBody, OpenApiUtils.getOpenApi( servletConfig ) ) ).build();
    }
}
