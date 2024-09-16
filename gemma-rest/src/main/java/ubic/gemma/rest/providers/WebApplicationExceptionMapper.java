package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.glassfish.jersey.server.ContainerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.ResponseErrorObject;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link WebApplicationException} so that it always expose a {@link ResponseErrorObject} entity.
 * <p>
 * By default, {@link InternalServerErrorException} are logged.
 *
 * @author poirigui
 */
@Provider
@Component
public class WebApplicationExceptionMapper extends AbstractExceptionMapper<WebApplicationException> {

    @Autowired
    public WebApplicationExceptionMapper( @Value("${gemma.hosturl}") String hostUrl, OpenAPI spec, BuildInfo buildInfo ) {
        super( hostUrl, spec, buildInfo );
    }

    @Override
    protected boolean logException( WebApplicationException e ) {
        return e instanceof InternalServerErrorException;
    }

    @Override
    protected Response.Status getStatus( WebApplicationException exception ) {
        return Response.Status.fromStatusCode( exception.getResponse().getStatus() );
    }

    @Override
    protected Response.ResponseBuilder getResponseBuilder( ContainerRequest request, WebApplicationException exception ) {
        return super.getResponseBuilder( request, exception )
                .replaceAll( exception.getResponse().getHeaders() );
    }
}
