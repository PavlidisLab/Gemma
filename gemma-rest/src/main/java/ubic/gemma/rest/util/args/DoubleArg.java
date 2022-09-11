package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Class representing an API argument that should be a double.
 *
 * @author tesarst
 */
@Schema(type = "number", format = "double")
public class DoubleArg extends AbstractArg<Double> {

    private DoubleArg( double value ) {
        super( value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request double number argument
     * @return an instance of DoubleArg representing double number value of the input string, or a malformed DoubleArg
     * that will throw a {@link javax.ws.rs.BadRequestException} when accessing its value, if the input String can not
     * be converted into a double.
     */
    @SuppressWarnings("unused")
    public static DoubleArg valueOf( final String s ) throws MalformedArgException {
        try {
            return new DoubleArg( Double.parseDouble( s ) );
        } catch ( NumberFormatException e ) {
            throw new MalformedArgException( String.format( "Value '%s' can not converted to a double number", s ), e );
        }
    }

}
