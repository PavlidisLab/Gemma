package ubic.gemma.web.services.rest.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Specialized error for malformed {@link ubic.gemma.web.services.rest.util.args.Arg}
 *
 * The recommended HTTP status for this exception is 400 Bad Request.
 *
 * @author poirigui
 */
public class MalformedArgException extends WebApplicationException {

    public MalformedArgException( String message, Throwable cause ) {
        super( message, cause, createResponse( message, cause ) );
    }

    private static Response createResponse( String errorMessage, Throwable cause ) {
        WellComposedErrorBody body = new WellComposedErrorBody( Response.Status.BAD_REQUEST, errorMessage );
        WellComposedErrorBody.addExceptionFields( body, cause );
        return Response.status( body.getStatus() )
                .entity( new ResponseErrorObject( body ) )
                .build();
    }
}
