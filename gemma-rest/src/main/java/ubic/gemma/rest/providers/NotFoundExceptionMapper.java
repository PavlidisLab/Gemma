package ubic.gemma.rest.providers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * This mapper ensures that raised {@link NotFoundException} throughout the API contain well-formed {@link ResponseErrorObject}
 * entity.
 * <p>
 * Normally, this would be handled by {@link WebApplicationExceptionMapper}, but we also want to expose the stack trace
 * in the case of a missing entity.
 *
 * @author poirigui
 */
@Provider
@Component
public class NotFoundExceptionMapper extends AbstractExceptionMapper<NotFoundException> {

    @Autowired
    public NotFoundExceptionMapper( OpenAPI spec, BuildInfo buildInfo ) {
        super( spec, buildInfo );
    }

    @Override
    protected Response.Status getStatus( NotFoundException exception ) {
        return Response.Status.NOT_FOUND;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( NotFoundException exception ) {
        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND, exception.getMessage() );
        if ( exception.getCause() != null ) {
            WellComposedErrorBody.addExceptionFields( errorBody, exception.getCause() );
        }
        return errorBody;
    }
}
