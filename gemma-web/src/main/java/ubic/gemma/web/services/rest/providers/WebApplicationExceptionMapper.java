package ubic.gemma.web.services.rest.providers;

import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link WebApplicationException} so that it always expose a {@link ResponseErrorObject} entity.
 * @author poirigui
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse( WebApplicationException e ) {
        return Response.fromResponse( e.getResponse() )
                .entity( new ResponseErrorObject( new WellComposedErrorBody( Response.Status.fromStatusCode( e.getResponse().getStatus() ), e.getMessage() ) ) )
                .build();
    }
}
