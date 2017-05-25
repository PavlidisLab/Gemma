package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;

/**
 * Created by tesarst on 26/05/17.
 * Base class for non Object-specific functionality argument types.
 */
abstract class PrimitiveArg {

    private String errorMessage = "";
    private boolean malComposed;
    private Exception exception;

    PrimitiveArg() {
        this.malComposed = false;
    }

    /**
     * Constructor used to inform that the received argument was not well-formed.
     *
     * @param errorMessage the error message to be displayed to the client.
     * @param exception    the exception that the client should be informed about.
     */
    PrimitiveArg( String errorMessage, Exception exception ) {
        this.malComposed = true;
        this.exception = exception;
        this.errorMessage = errorMessage;
    }

    /* ********************************
     * Package-private methods
     * ********************************/

    /**
     * Checks whether the instance of this object was created as a malformed argument, and if true, throws an
     * exception using the information provided in the constructor.
     */
    void checkMalformed() {
        if ( this.malComposed ) {
            Response.Status code = Response.Status.BAD_REQUEST;
            WellComposedErrorBody body = new WellComposedErrorBody( code, errorMessage );
            WellComposedErrorBody.addExceptionFields( body, this.exception );
            throw new GemmaApiException( body, code );
        }
    }

}
