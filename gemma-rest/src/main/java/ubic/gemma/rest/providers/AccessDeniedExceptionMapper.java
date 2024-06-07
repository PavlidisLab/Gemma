package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Map Spring Security's {@link AccessDeniedException} to a 403 Forbidden response.
 */
@Provider
@Component
public class AccessDeniedExceptionMapper extends AbstractExceptionMapper<AccessDeniedException> {

    @Autowired
    public AccessDeniedExceptionMapper( OpenAPI spec, BuildInfo buildInfo ) {
        super( spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( AccessDeniedException exception ) {
        return Response.Status.FORBIDDEN;
    }
}
