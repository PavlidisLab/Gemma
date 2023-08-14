package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Argument used to represent an offset.
 */
@Schema(type = "integer", minimum = "0", description = "The offset of the first retrieved result.")
public class OffsetArg extends AbstractArg<Integer> {

    private OffsetArg( int value ) {
        super( value );
    }

    public static OffsetArg valueOf( String s ) throws MalformedArgException {
        int offset;
        try {
            offset = Integer.parseInt( s );
        } catch ( NumberFormatException e ) {
            throw new MalformedArgException( "Offset must be a valid integer.", e );
        }
        if ( offset < 0 ) {
            throw new MalformedArgException( "Offset must be positive.", null );
        }
        return new OffsetArg( offset );
    }
}
