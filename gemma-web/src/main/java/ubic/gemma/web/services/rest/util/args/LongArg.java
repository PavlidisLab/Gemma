package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

/**
 * Class representing an API argument that should be a long.
 *
 * @author tesarst
 */
public class LongArg extends MalformableArg {
    private static final String ERROR_MSG = "Value '%s' can not converted to a long number";

    private Long value;

    private LongArg( long value ) {
        this.value = value;
    }

    private LongArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    @Override
    public String toString() {
        if(this.value == null) return "";
        return String.valueOf( this.value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request long number argument
     * @return an instance of LongArg representing long number value of the input string, or a malformed LongArg that will throw a
     * {@link GemmaApiException} when accessing its value, if the input String can not
     * be converted into a long.
     */
    @SuppressWarnings("unused")
    public static LongArg valueOf( final String s ) {
        try {
            return new LongArg( Long.parseLong( s ) );
        } catch ( NumberFormatException e ) {
            return new LongArg( String.format( ERROR_MSG, s ), e );
        }
    }

    /**
     * @return the long value of the original String argument. If the original argument could not be converted into
     * a long number, will produce a {@link GemmaApiException} instead.
     */
    public long getValue() {
        this.checkMalformed();
        return value;
    }

}
