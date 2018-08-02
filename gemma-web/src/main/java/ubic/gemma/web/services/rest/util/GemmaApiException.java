package ubic.gemma.web.services.rest.util;

import javax.ws.rs.core.Response;

/**
 * Exception for the REST API related issues.
 *
 * @author tesarst
 */
@SuppressWarnings("WeakerAccess") // Getters used by RS serializer.
public class GemmaApiException extends RuntimeException {

    private final Response.Status code;
    private ResponseErrorObject errorObject;

    public GemmaApiException( Response.Status code ) {
        this.code = code;
    }

    public GemmaApiException( WellComposedErrorBody body ) {
        super( body.getMessage() );
        this.errorObject = new ResponseErrorObject( body );
        this.code = body.getStatus();
    }

    public ResponseErrorObject getErrorObject() {
        return errorObject;
    }

    public Response.Status getCode() {
        return code;
    }
}
