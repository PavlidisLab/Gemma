package ubic.gemma.web.services.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.ServletUtils;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Handles Spring Security {@link AuthenticationException} by producing a 403 Forbidden response.
 */
@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

    private static final Log log = LogFactory.getLog( AuthenticationExceptionMapper.class.getName() );

    @Context
    private HttpServletRequest request;

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( AuthenticationException e ) {
        log.error( "Failed to authenticate user for request: " + ServletUtils.summarizeRequest( request ) + ".", e );
        // for security reasons, we don't include the error object in the response entity
        return Response.status( Response.Status.FORBIDDEN ).entity(
                        new ResponseErrorObject( new WellComposedErrorBody( Response.Status.FORBIDDEN, e.getMessage() ), OpenApiUtils.getOpenApi( servletConfig ) ) )
                .build();
    }
}
