package ubic.gemma.web.services.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.web.services.rest.util.MalformedArgException;
import ubic.gemma.web.services.rest.util.OpenApiUtils;
import ubic.gemma.web.services.rest.util.ResponseErrorObject;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@CommonsLog
public class MalformedArgExceptionMapper implements ExceptionMapper<MalformedArgException> {

    @Context
    private ServletConfig servletConfig;

    @Override
    public Response toResponse( MalformedArgException e ) {
        WellComposedErrorBody body = new WellComposedErrorBody( Response.Status.BAD_REQUEST, e.getMessage() );
        if ( e.getCause() != null ) {
            WellComposedErrorBody.addExceptionFields( body, e.getCause() );
        }
        return Response.status( body.getStatus() )
                .type( MediaType.APPLICATION_JSON_TYPE )
                .entity( new ResponseErrorObject( body, OpenApiUtils.getOpenApi( servletConfig ) ) )
                .build();
    }
}
