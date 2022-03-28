package ubic.gemma.web.services.rest.providers;

import ubic.gemma.core.association.phenotype.EntityNotFoundException;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link EntityNotFoundException} to a response containing a JSON error object.
 */
@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( EntityNotFoundException e ) {
        return Response.status( Response.Status.NOT_FOUND )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .entity( new ResponseErrorObject( new WellComposedErrorBody( Response.Status.NOT_FOUND, e.getMessage() ), OpenApiUtils.getOpenApi( servletConfig ) ) )
                .build();
    }
}
