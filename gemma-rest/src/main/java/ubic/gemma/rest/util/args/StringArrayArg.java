package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Class representing an API argument that should be an array of strings.
 *
 * @author tesarst
 */
@ArraySchema(arraySchema = @Schema(description = StringArrayArg.ARRAY_SCHEMA_DESCRIPTION), schema = @Schema(implementation = String.class), minItems = 1)
public class StringArrayArg extends AbstractArrayArg<String> {

    public static final String OF_WHAT = "strings";
    public static final String ARRAY_SCHEMA_DESCRIPTION = ARRAY_SCHEMA_DESCRIPTION_PREFIX + OF_WHAT + ". " + ARRAY_SCHEMA_COMPRESSION_DESCRIPTION;

    private StringArrayArg( List<String> values ) {
        super( values );
    }

    public static StringArrayArg valueOf( String s ) {
        return valueOf( s, "strings", StringArrayArg::new, true );
    }
}
