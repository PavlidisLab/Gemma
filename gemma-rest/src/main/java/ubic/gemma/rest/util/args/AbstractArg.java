package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import ubic.gemma.rest.util.MalformedArgException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

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

    /**
     * A base64-encoded gzip header to detect compressed filters.
     */
    private static final String BASE64_ENCODED_GZIP_MAGIC = "H4s";

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

    /**
     * Decode a base64-encoded gzip-compressed argument.
     * <p>
     * This intended to be used in the {@code valueOf} methods of subclasses.
     */
    protected static String decodeCompressedArg( @Nullable String s ) {
        if ( s != null && s.startsWith( BASE64_ENCODED_GZIP_MAGIC ) && isValidBase64( s ) ) {
            try {
                return IOUtils.toString( new GZIPInputStream( new ByteArrayInputStream( Base64.getDecoder().decode( s ) ) ), StandardCharsets.UTF_8 );
            } catch ( IOException e ) {
                throw new MalformedArgException( "Invalid base64-encoded filter, make sure that your filter is first gzipped and then base64-encoded.", e );
            }
        } else {
            return s;
        }
    }

    private static boolean isValidBase64( String s ) {
        try {
            Base64.getDecoder().decode( s );
            return true;
        } catch ( IllegalArgumentException e ) {
            // invalid base-64 encoded buffer, this might be a regular string
            return false;
        }
    }
}
