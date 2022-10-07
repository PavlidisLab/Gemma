package ubic.gemma.web.services.rest.providers;

import org.apache.commons.math3.analysis.function.Abs;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This mapper ensures that raised {@link NotFoundException} throughout the API contain well-formed {@link ResponseErrorObject}
 * entity.
 *
 * Normally, this would be handled by {@link WebApplicationExceptionMapper}, but we also want to expose the stack trace
 * in the case of a missing entity.
 *
 * @author poirigui
 */
@Provider
public class NotFoundExceptionMapper extends AbstractExceptionMapper<NotFoundException> {

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
