package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

/**
 * Class representing an API argument that should be an array of Strings.
 *
 * @author tesarst
 */
@ArraySchema(schema = @Schema(implementation = String.class))
public class StringArrayArg extends AbstractArrayArg<String> {
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Strings";

    public StringArrayArg( List<String> values ) {
        super( values );
    }

    public StringArrayArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param  s the request arrayString argument
     * @return an instance of ArrayStringArg representing array of strings from the input string, or a malformed
     *           ArrayStringArg that will throw an
     *           {@link javax.ws.rs.BadRequestException} when accessing its value, if the input String can not be converted into an
     *           array of strings.
     */
    @SuppressWarnings("unused")
    public static StringArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new StringArrayArg( String.format( ERROR_MSG, s ), new IllegalArgumentException(
                    "Provide a string that contains at least one character, or several strings separated by a comma (',') character." ) );
        }
        return new StringArrayArg( StringUtils.splitAndTrim( s ) );
    }

}
