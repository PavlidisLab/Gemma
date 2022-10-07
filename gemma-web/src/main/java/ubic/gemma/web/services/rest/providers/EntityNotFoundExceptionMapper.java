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
public class EntityNotFoundExceptionMapper extends AbstractExceptionMapper<EntityNotFoundException> {

    @Context
    private ServletConfig servletConfig;

    @Override
    protected Response.Status getStatus( EntityNotFoundException exception ) {
        return Response.Status.NOT_FOUND;
    }
}
