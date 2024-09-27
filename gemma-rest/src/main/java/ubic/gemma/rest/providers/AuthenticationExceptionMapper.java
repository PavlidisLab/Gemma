package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Handles Spring Security {@link AuthenticationException} by producing a 403 Forbidden response.
 */
@Provider
@Component
public class AuthenticationExceptionMapper extends AbstractExceptionMapper<AuthenticationException> {

    @Autowired
    public AuthenticationExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, OpenAPI spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( AuthenticationException exception ) {
        return Response.Status.FORBIDDEN;
    }

    @Override
    protected boolean logException( AuthenticationException e ) {
        return true;
    }
}
