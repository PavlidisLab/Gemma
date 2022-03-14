package ubic.gemma.web.services.rest.providers;

import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link WebApplicationException} so that it always expose a {@link ResponseErrorObject} entity.
 *
 * @author poirigui
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( WebApplicationException e ) {
        return Response.fromResponse( e.getResponse() )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .entity( new ResponseErrorObject(
                        new WellComposedErrorBody( Response.Status.fromStatusCode( e.getResponse().getStatus() ),
                                e.getMessage() ), OpenApiUtils.getOpenApi( servletConfig ) ) ).build();
    }
}
