package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

/**
 * Class representing an API argument that should be an integer.
 *
 * @author tesarst
 */
public class IntArg extends AbstractArg<Integer> {
    private static final String ERROR_MSG = "Value '%s' can not converted to an integer";

    private IntArg( int value ) {
        super( value );
    }

    private IntArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request integer argument
     * @return an instance of IntArg representing integer value of the input string, or a malformed IntArg that will throw an
     * {@link ubic.gemma.web.services.rest.util.GemmaApiException} when accessing its value, if the input String can not
     * be converted into an integer.
     */
    @SuppressWarnings("unused")
    public static IntArg valueOf( final String s ) {
        try {
            return new IntArg( Integer.parseInt( s ) );
        } catch ( NumberFormatException e ) {
            return new IntArg( String.format( ERROR_MSG, s ), e );
        }
    }

}
