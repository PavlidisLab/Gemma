package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import ubic.gemma.web.services.rest.util.MalformedArgException;

/**
 * Base class for non Object-specific functionality argument types, that can be malformed on input (E.g an argument
 * representing a number was a non-numeric string in the request).
 *
 * The {@link Schema} annotation ensures that custom args are represented by a string in the OpenAPI specification.
 *
 * @author tesarst
 */
abstract class AbstractArg<T> implements Arg<T> {

    private final T value;

    /* if this is malformed */
    private final boolean malformed;
    private String errorMessage = "";
    private Throwable exception;

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
    AbstractArg( String errorMessage, Throwable exception ) {
        this.value = null;
        this.malformed = true;
        this.exception = exception;
        this.errorMessage = errorMessage;
    }

    public final boolean isMalformed() {
        return malformed;
    }

    @NotNull
    @Override
    public final T getValue() throws MalformedArgException {
        if ( this.malformed ) {
            throw new MalformedArgException( this.errorMessage, this.exception );
        }
        return this.value;
    }

    @Override
    public String toString() {
        if ( this.malformed ) {
            return "This " + getClass().getName() + " is malformed because of the following error: " + errorMessage;
        } else {
            return this.value == null ? "" : String.valueOf( this.value );
        }
    }
}
