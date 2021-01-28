package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

/**
 * Class representing an API argument that should be an integer.
 *
 * @author tesarst
 */
public class IntArg extends MalformableArg {
    private static final String ERROR_MSG = "Value '%s' can not converted to an integer";

    private Integer value;

    private IntArg( int value ) {
        this.value = value;
    }

    private IntArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    @Override
    public String toString() {
        if ( this.value == null ) return "";
        return String.valueOf( this.value );
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

    /**
     * @return the integer value of the original String argument. If the original argument could not be converted into
     * an integer, will produce a {@link GemmaApiException} instead.
     */
    public int getValue() {
        this.checkMalformed();
        return value;
    }

}
