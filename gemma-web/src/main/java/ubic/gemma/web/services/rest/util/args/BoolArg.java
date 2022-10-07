package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.web.services.rest.util.MalformedArgException;

/**
 * Class representing an API argument that should be a boolean.
 *
 * @author tesarst
 */
@Schema(type = "boolean")
public class BoolArg extends AbstractArg<Boolean> {

    private BoolArg( boolean value ) {
        super( value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request boolean argument
     * @return an instance of BoolArg representing boolean value of the input string, or a malformed BoolArg that will
     * throw an {@link javax.ws.rs.BadRequestException} when accessing its value, if the input String can not be
     * converted into a boolean.
     */
    @SuppressWarnings("unused")
    public static BoolArg valueOf( final String s ) throws MalformedArgException {
        if ( !( s.equals( "false" ) || s.equals( "true" ) ) ) {
            throw new MalformedArgException( String.format( "Value '%s' can not converted to a boolean", s ),
                    new IllegalArgumentException( "Boolean value has to be either 'true' or 'false'" ) );
        }
        return new BoolArg( Boolean.parseBoolean( s ) );
    }

}
