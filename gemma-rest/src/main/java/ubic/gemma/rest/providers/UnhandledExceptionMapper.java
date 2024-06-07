package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Render unhandled exceptions.
 * <p>
 * The {@link RequestExceptionLogger} does not always work reliably, and more than often the exception is caught instead
 * by the configured JSP error page.
 */
@Provider
@Component
public class UnhandledExceptionMapper extends AbstractExceptionMapper<Throwable> {

    @Autowired
    public UnhandledExceptionMapper( OpenAPI spec, BuildInfo buildInfo ) {
        super( spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( Throwable exception ) {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    protected boolean logException( Throwable t ) {
        return true;
    }
}
