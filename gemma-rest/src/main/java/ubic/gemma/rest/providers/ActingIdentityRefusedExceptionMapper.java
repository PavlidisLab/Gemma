package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.security.util.ActingIdentityRefusedException;
import ubic.gemma.core.util.BuildInfo;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.concurrent.Future;

/**
 * An {@code onBehalfOf} the server refuses to record is a bad request, not a permission failure: the caller may act for
 * someone, but not under the name it gave.
 */
@Provider
@Component
public class ActingIdentityRefusedExceptionMapper extends AbstractExceptionMapper<ActingIdentityRefusedException> {

    @Autowired
    public ActingIdentityRefusedExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, @Qualifier("openApi") Future<OpenAPI> spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( ActingIdentityRefusedException exception ) {
        return Response.Status.BAD_REQUEST;
    }
}
