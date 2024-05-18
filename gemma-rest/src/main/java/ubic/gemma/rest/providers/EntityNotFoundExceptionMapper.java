package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.association.phenotype.EntityNotFoundException;
import ubic.gemma.core.util.BuildInfo;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Map {@link EntityNotFoundException} to a response containing a JSON error object.
 * @deprecated Phenocarta is scheduled for removal
 */
@Provider
@Component
@Deprecated
public class EntityNotFoundExceptionMapper extends AbstractExceptionMapper<EntityNotFoundException> {

    @Autowired
    public EntityNotFoundExceptionMapper( OpenAPI spec, BuildInfo buildInfo ) {
        super( spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( EntityNotFoundException exception ) {
        return Response.Status.NOT_FOUND;
    }
}
