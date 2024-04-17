package ubic.gemma.rest.providers;

import org.springframework.security.core.AuthenticationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Handles Spring Security {@link AuthenticationException} by producing a 403 Forbidden response.
 */
@Provider
public class AuthenticationExceptionMapper extends AbstractExceptionMapper<AuthenticationException> {

    @Override
    protected Response.Status getStatus( AuthenticationException exception ) {
        return Response.Status.FORBIDDEN;
    }

    @Override
    protected boolean logException( AuthenticationException e ) {
        return true;
    }
}
