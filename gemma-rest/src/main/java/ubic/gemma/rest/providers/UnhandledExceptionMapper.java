package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Render unhandled exceptions.
 */
@Provider
@Component
public class UnhandledExceptionMapper extends AbstractExceptionMapper<Throwable> {

    @Autowired
    public UnhandledExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, OpenAPI spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( Throwable exception ) {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }
}
