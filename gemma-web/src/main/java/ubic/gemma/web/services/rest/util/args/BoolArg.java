package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Class representing an API argument that should be a boolean.
 *
 * @author tesarst
 */
@Schema(implementation = Boolean.class)
public class BoolArg extends AbstractArg<Boolean> {
    private static final String ERROR_MSG = "Value '%s' can not converted to a boolean";

    private BoolArg( boolean value ) {
        super( value );
    }

    private BoolArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request boolean argument
     * @return an instance of BoolArg representing boolean value of the input string, or a malformed BoolArg that will throw an
     * {@link javax.ws.rs.BadRequestException} when accessing its value, if the input String can not be converted into a boolean.
     */
    @SuppressWarnings("unused")
    public static BoolArg valueOf( final String s ) {
        if ( !( s.equals( "false" ) || s.equals( "true" ) ) ) {
            return new BoolArg( String.format( ERROR_MSG, s ),
                    new IllegalArgumentException( "Boolean value has to be either 'true' or 'false'" ) );
        }
        return new BoolArg( Boolean.parseBoolean( s ) );
    }

}
