package ubic.gemma.web.services.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.AuthenticationException;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Handles Spring Security {@link AuthenticationException} by producing a 403 Forbidden response.
 */
@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

    private static Log log = LogFactory.getLog( AuthenticationExceptionMapper.class.getName() );

    @Override
    public Response toResponse( AuthenticationException e ) {
        log.error( "Failed to authenticate user:", e );
        return Response.status( Response.Status.FORBIDDEN )
                .entity( new ResponseErrorObject( new WellComposedErrorBody( Response.Status.FORBIDDEN, e.getMessage() ) ) )
                .build();
    }
}
