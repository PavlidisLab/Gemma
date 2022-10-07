package ubic.gemma.web.services.rest.providers;

import org.springframework.security.access.AccessDeniedException;
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
 * Map Spring Security's {@link AccessDeniedException} to a 403 Forbidden response.
 */
@Provider
public class AccessDeniedExceptionMapper extends AbstractExceptionMapper<AccessDeniedException> {

    @Override
    protected Response.Status getStatus( AccessDeniedException exception ) {
        return Response.Status.FORBIDDEN;
    }
}
