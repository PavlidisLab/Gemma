package ubic.gemma.rest.providers;

import ubic.gemma.rest.util.ResponseErrorObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link WebApplicationException} so that it always expose a {@link ResponseErrorObject} entity.
 *
 * @author poirigui
 */
@Provider
public class WebApplicationExceptionMapper extends AbstractExceptionMapper<WebApplicationException> {

    @Override
    protected Response.Status getStatus( WebApplicationException exception ) {
        return Response.Status.fromStatusCode( exception.getResponse().getStatus() );
    }
}
