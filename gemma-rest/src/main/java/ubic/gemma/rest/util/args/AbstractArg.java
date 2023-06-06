package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;
import ubic.gemma.rest.util.MalformedArgException;

import javax.annotation.Nonnull;

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
    }

    /**
     * Obtain the value represented by this argument.
     *
     * @return the value represented by this argument, which can never be null
     * @throws MalformedArgException if this arg is malformed
     */
    @Nonnull
    @Override
    public T getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return String.valueOf( this.value );
    }
}
