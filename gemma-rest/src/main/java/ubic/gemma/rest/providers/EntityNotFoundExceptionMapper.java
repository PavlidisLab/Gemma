package ubic.gemma.rest.providers;

import ubic.gemma.core.association.phenotype.EntityNotFoundException;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link EntityNotFoundException} to a response containing a JSON error object.
 */
@Provider
public class EntityNotFoundExceptionMapper extends AbstractExceptionMapper<EntityNotFoundException> {

    @Context
    private ServletConfig servletConfig;

    @Override
    protected Response.Status getStatus( EntityNotFoundException exception ) {
        return Response.Status.NOT_FOUND;
    }
}
