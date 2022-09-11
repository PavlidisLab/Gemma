package ubic.gemma.rest.providers;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.rest.util.MalformedArgException;
import ubic.gemma.rest.util.OpenApiUtils;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.WellComposedErrorBody;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@CommonsLog
public class MalformedArgExceptionMapper extends AbstractExceptionMapper<MalformedArgException> {

    @Override
    protected Response.Status getStatus( MalformedArgException exception ) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    protected WellComposedErrorBody getWellComposedErrorBody( MalformedArgException exception ) {
        WellComposedErrorBody body = new WellComposedErrorBody( Response.Status.BAD_REQUEST, exception.getMessage() );
        if ( exception.getCause() != null ) {
            WellComposedErrorBody.addExceptionFields( body, exception.getCause() );
        }
        return body;
    }
}
