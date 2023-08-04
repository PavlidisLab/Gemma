package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

@ArraySchema(
        schema = @Schema(implementation = String.class),
        arraySchema = @Schema(description = "List of fields to exclude from the payload. If the payload is a list of object, then the exclusion is applicable to all the listed objects."),
        minItems = 1, uniqueItems = true)
public class ExcludeArg<T> extends AbstractArrayArg<String> {

    private ExcludeArg( List<String> values ) {
        super( values );
    }

    public static ExcludeArg<?> valueOf( String s ) {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( String.format( ERROR_MSG, s ), new IllegalArgumentException(
                    "Provide a string that contains at least one character, or several strings separated by a comma (',') character." ) );
        }
        return new ExcludeArg<>( splitAndTrim( s ) );
    }
}
