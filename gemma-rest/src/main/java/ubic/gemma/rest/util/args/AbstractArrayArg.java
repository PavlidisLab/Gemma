package ubic.gemma.rest.util.args;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.rest.util.ArgUtils;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static ubic.gemma.rest.util.ArgUtils.decodeCompressedArg;

/**
 * Represents a comma-delimited array API argument.
 * <p>
 * If you use this alongside a {@link javax.ws.rs.QueryParam}, make sure that you include a {@link io.swagger.v3.oas.annotations.Parameter}
 * with the 'explode' attribute set to {@link io.swagger.v3.oas.annotations.enums.Explode#FALSE}, otherwise the
 * serialization will not be correct.
 *
 * @param <T> the type of elements the array contains
 * @author tesarst
 */
public abstract class AbstractArrayArg<T> extends AbstractArg<List<T>> {

    /**
     * Prefix to use to describe the array schema in subclasses.
     *
     * @see io.swagger.v3.oas.annotations.media.ArraySchema
     */
    public static final String ARRAY_SCHEMA_DESCRIPTION_PREFIX = "A comma-delimited list of ";

    /**
     * A description of the base64-gzip encoding to use in array schema descriptions in subclasses.
     *
     * @see io.swagger.v3.oas.annotations.media.ArraySchema
     */
    public static final String ARRAY_SCHEMA_COMPRESSION_DESCRIPTION = "The value may be compressed with gzip and encoded with base64.";

    protected AbstractArrayArg( List<T> values ) {
        super( values );
    }

    /**
     * Evaluate an input array argument.
     * <p>
     * Split a string by the ',' comma character and trim the resulting pieces.
     * <p>
     * This is meant to be used for parsing query arguments that use a comma as a delimiter.
     *
     * @param arg           the string to process
     * @param ofWhat        a description of what is expected
     * @param func          a function to convert the resulting list of string to the specific array argument
     * @param decompressArg decompress the argument as per {@link ArgUtils#decodeCompressedArg(String)}
     * @return trimmed strings exploded from the input.
     * @throws MalformedArgException wrapping any raised {@link IllegalArgumentException} which may be caused by an
     *                               empty string, an invalid base64-gzip encoded input or such an exception raised by
     *                               the passed function
     */
    protected static <T extends AbstractArrayArg<?>> T valueOf( final String arg, String ofWhat, Function<List<String>, T> func, boolean decompressArg ) throws MalformedArgException {
        String val = "'" + arg + "'";
        try {
            String decompressedArg;
            if ( decompressArg ) {
                decompressedArg = decodeCompressedArg( arg );
                val = "'" + decompressedArg + "' (decompressed from " + val + ")";
            } else {
                decompressedArg = arg;
            }
            if ( StringUtils.isBlank( decompressedArg ) ) {
                throw new IllegalArgumentException( String.format( "Provide a value that contains at least one or multiple %s separated by commas.", ofWhat ) );
            }
            return func.apply( Arrays.asList( decompressedArg.split( "\\s*,\\s*" ) ) );
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( String.format( "Value '%s' can not converted to an array of %s.", val, ofWhat ), e );
        }
    }
}
