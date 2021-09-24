package ubic.gemma.web.services.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map Spring Security's {@link AccessDeniedException} to a 403 Forbidden response.
 */
@Provider
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    private static Log log = LogFactory.getLog( AccessDeniedExceptionMapper.class.getName() );

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( AccessDeniedException e ) {
        log.error( "Exception during authentication:", e );
        // for security reasons, we don't include the error object in the response entity
        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.FORBIDDEN, e.getMessage() );
        return Response.status( Response.Status.FORBIDDEN )
                .entity( new ResponseErrorObject( errorBody, OpenApiUtils.getOpenApi( servletConfig ) ) )
                .build();
    }
}
