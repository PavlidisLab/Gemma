package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.web.services.rest.util.GemmaApiException;

import java.util.Arrays;
import java.util.List;

/**
 * Class representing an API argument that should be an array of Strings.
 *
 * @author tesarst
 */
public class StringArrayArg extends ArrayArg<String> {
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Strings";

    public StringArrayArg( List<String> values ) {
        super( values );
    }

    public StringArrayArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Splits string by the ',' comma character and trims the resulting strings.
     *
     * @param  arg the string to process
     * @return trimmed strings exploded from the input.
     */
    protected static String[] splitString( String arg ) {
        String[] array = arg.split( "," );
        for ( int i = 0; i < array.length; i++ )
            array[i] = array[i].trim();
        return array;
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param  s the request arrayString argument
     * @return an instance of ArrayStringArg representing array of strings from the input string, or a malformed
     *           ArrayStringArg that will throw an
     *           {@link GemmaApiException} when accessing its value, if the input String can not be converted into an
     *           array of strings.
     */
    @SuppressWarnings("unused")
    public static StringArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new StringArrayArg( String.format( ERROR_MSG, s ), new IllegalArgumentException(
                    "Provide a string that contains at least one character, or several strings separated by a comma (',') character." ) );
        }
        return new StringArrayArg( Arrays.asList( ArrayArg.splitString( s ) ) );
    }

}
