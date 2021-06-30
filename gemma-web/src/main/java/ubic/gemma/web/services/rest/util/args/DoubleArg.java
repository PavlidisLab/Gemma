package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

/**
 * Class representing an API argument that should be a double.
 *
 * @author tesarst
 */
public class DoubleArg extends AbstractArg<Double> {
    private static final String ERROR_MSG = "Value '%s' can not converted to a double number";

    private DoubleArg( double value ) {
        super( value );
    }

    private DoubleArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request double number argument
     * @return an instance of DoubleArg representing double number value of the input string, or a malformed DoubleArg that will throw a
     * {@link GemmaApiException} when accessing its value, if the input String can not
     * be converted into a double.
     */
    @SuppressWarnings("unused")
    public static DoubleArg valueOf( final String s ) {
        try {
            return new DoubleArg( Double.parseDouble( s ) );
        } catch ( NumberFormatException e ) {
            return new DoubleArg( String.format( ERROR_MSG, s ), e );
        }
    }

}
