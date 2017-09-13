package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.web.services.rest.util.GemmaApiException;

/**
 * Class representing an API argument that should be a boolean.
 *
 * @author tesarst
 */
public class BoolArg extends MalformableArg {
    private static final String ERROR_MSG = "Value '%s' can not converted to a boolean";

    private Boolean value;

    private BoolArg( boolean value ) {
        this.value = value;
    }

    private BoolArg( String errorMessage, Exception exception ) {
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
     * @param s the request boolean argument
     * @return an instance of BoolArg representing boolean value of the input string, or a malformed BoolArg that will throw an
     * {@link GemmaApiException} when accessing its value, if the input String can not be converted into a boolean.
     */
    @SuppressWarnings("unused")
    public static BoolArg valueOf( final String s ) {
        if ( !( s.equals( "false" ) || s.equals( "true" ) ) ) {
            return new BoolArg( String.format( ERROR_MSG, s ),
                    new IllegalArgumentException( "Boolean value has to be either 'true' or 'false'" ) );
        }
        return new BoolArg( Boolean.parseBoolean( s ) );
    }

    /**
     * @return the boolean value of the original String argument. If the original argument could not be converted into
     * a boolean, will produce a {@link GemmaApiException} instead.
     */
    public boolean getValue() {
        this.checkMalformed();
        return value;
    }

}
