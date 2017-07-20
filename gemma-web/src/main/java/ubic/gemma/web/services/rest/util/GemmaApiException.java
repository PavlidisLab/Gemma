package ubic.gemma.web.services.rest.util;

import javax.ws.rs.core.Response;
import java.io.Serializable;

/**
 * Created by tesarst on 17/05/17.
 * Exception used to handle api error code setting.
 */
public class GemmaApiException extends RuntimeException implements Serializable {

    private Response.Status code;
    private ResponseErrorObject errorObject;

    public GemmaApiException( Response.Status code ) {
        this.code = code;
    }

    public GemmaApiException( WellComposedErrorBody body) {
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
