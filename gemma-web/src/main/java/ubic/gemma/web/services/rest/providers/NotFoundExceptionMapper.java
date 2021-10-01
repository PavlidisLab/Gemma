package ubic.gemma.web.services.rest.providers;

import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
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
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( NotFoundException e ) {
        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND, e.getMessage() );
        if ( e.getCause() != null ) {
            WellComposedErrorBody.addExceptionFields( errorBody, e.getCause() );
        }
        return Response.fromResponse( e.getResponse() )
                .entity( new ResponseErrorObject( errorBody, OpenApiUtils.getOpenApi( servletConfig ) ) )
                .build();
    }
}
