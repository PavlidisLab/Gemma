package ubic.gemma.web.services.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.AuthenticationException;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.ServletUtils;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
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
    protected boolean logException() {
        return true;
    }
}
