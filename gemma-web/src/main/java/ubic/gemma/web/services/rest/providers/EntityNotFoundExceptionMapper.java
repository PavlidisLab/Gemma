package ubic.gemma.web.services.rest.providers;

import ubic.gemma.core.association.phenotype.EntityNotFoundException;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link EntityNotFoundException} to a response containing a JSON error object.
 */
@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse( EntityNotFoundException e ) {
        return Response.status( Response.Status.NOT_FOUND )
                .entity( new ResponseErrorObject( new WellComposedErrorBody( Response.Status.NOT_FOUND, e.getMessage() ) ) )
                .build();
    }
}
