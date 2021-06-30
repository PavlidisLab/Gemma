package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;

/**
 * Base class for non Object-specific functionality argument types, that can be malformed on input (E.g an argument
 * representing a number was a non-numeric string in the request).
 *
 * @author tesarst
 */
abstract class AbstractArg<T> {

    private final T value;
    private final boolean malformed;
    private String errorMessage = "";
    private Exception exception;

    AbstractArg( T value ) {
        this.value = value;
        this.malformed = false;
    }

    /**
     * Constructor used to inform that the received argument was not well-formed.
     *
     * @param errorMessage the error message to be displayed to the client.
     * @param exception    the exception that the client should be informed about.
     */
    AbstractArg( String errorMessage, Exception exception ) {
        this.value = null;
        this.malformed = true;
        this.exception = exception;
        this.errorMessage = errorMessage;
    }

    /**
     * Checks whether the instance of this object was created as a malformed argument, and if true, throws an
     * exception using the information provided in the constructor.
     */
    private void checkMalformed() throws GemmaApiException {
        if ( this.malformed ) {
            WellComposedErrorBody body = new WellComposedErrorBody( Response.Status.BAD_REQUEST, errorMessage );
            WellComposedErrorBody.addExceptionFields( body, this.exception );
            throw new GemmaApiException( body );
        }
    }

    public final T getValue() throws GemmaApiException {
        if ( this.malformed ) {
            WellComposedErrorBody body = new WellComposedErrorBody( Response.Status.BAD_REQUEST, errorMessage );
            WellComposedErrorBody.addExceptionFields( body, this.exception );
            throw new GemmaApiException( body );
        }
        return this.value;
    }

    @Override
    public String toString() {
        if ( this.value == null ) return "";
        return String.valueOf( this.value );
    }
}
