package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Class representing an API argument that should be an integer.
 *
 * @author tesarst
 */
@Schema(type = "integer")
public class IntArg extends AbstractArg<Integer> {

    private IntArg( int value ) {
        super( value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request integer argument
     * @return an instance of IntArg representing integer value of the input string, or a malformed IntArg that will
     * throw an {@link javax.ws.rs.BadRequestException} when accessing its value, if the input String can not be
     * converted into an integer.
     */
    @SuppressWarnings("unused")
    public static IntArg valueOf( final String s ) throws MalformedArgException {
        try {
            return new IntArg( Integer.parseInt( s ) );
        } catch ( NumberFormatException e ) {
            throw new MalformedArgException( String.format( "Value '%s' can not converted to an integer", s ), e );
        }
    }

}
