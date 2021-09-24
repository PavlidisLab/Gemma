package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;

/**
 * Argument used to represent an offset.
 */
@Schema(implementation = Integer.class, minimum = "0")
public class OffsetArg extends AbstractArg<Integer> {

    private OffsetArg( @NonNull Integer value ) {
        super( value );
    }

    private OffsetArg( String message, Throwable cause ) {
        super( message, cause );
    }

    public static OffsetArg valueOf( String s ) {
        int offset;
        try {
            offset = Integer.parseInt( s );
        } catch ( NumberFormatException e ) {
            return new OffsetArg( "Offset must be a valid integer.", e );
        }
        if ( offset < 0 ) {
            return new OffsetArg( "Offset must be positive.", null );
        }
        return new OffsetArg( offset );
    }
}
