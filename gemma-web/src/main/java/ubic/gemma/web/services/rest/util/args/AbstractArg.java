package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;
import ubic.gemma.web.services.rest.util.MalformedArgException;

/**
 * Base class for non Object-specific functionality argument types, that can be malformed on input (E.g an argument
 * representing a number was a non-numeric string in the request).
 *
 * The {@link Schema} annotation used in sub-classes ensures that custom args are represented by a string in the OpenAPI
 * specification.
 *
 * @author tesarst
 */
public abstract class AbstractArg<T> implements Arg<T> {

    private final T value;

    /* if this is malformed */
    private final boolean malformed;
    private String errorMessage;
    private Throwable cause;

    /**
     * Constructor for well-formed value.
     *
     * Note that well-formed values can never be null. Although, the argument itself can be null to represent a value
     * that is omitted by the request.
     *
     * @param value a well-formed value which cannot be null
     */
    protected AbstractArg( @NonNull T value ) {
        this.value = value;
        this.malformed = false;
    }

    /**
     * Constructor used to inform that the received argument is malformed.
     *
     * If this is used, subsequent call to {@link #getValue()} will raise a {@link MalformedArgException} which will in
     * turn converted to a proper response as it inherits {@link javax.ws.rs.WebApplicationException}.
     *
     * @param errorMessage the error message to be displayed to the client.
     * @param cause        the exception that the client should be informed about.
     */
    protected AbstractArg( @NonNull String errorMessage, Throwable cause ) {
        this.value = null;
        this.malformed = true;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    /**
     * Test if this argument is malformed without triggering a {@link MalformedArgException}.
     * @return
     */
    public final boolean isMalformed() {
        return malformed;
    }

    /**
     * Obtain the value represented by this argument.
     *
     * @return the value represented by this argument, which can never be null
     * @throws MalformedArgException if this arg is malformed
     */
    @Override
    public T getValue() throws MalformedArgException {
        if ( this.malformed ) {
            throw new MalformedArgException( this.errorMessage, this.cause );
        }
        return this.value;
    }

    @Override
    public String toString() {
        if ( this.malformed ) {
            return "This " + getClass().getName() + " is malformed because of the following error: " + errorMessage;
        } else {
            return String.valueOf( this.value );
        }
    }
}
