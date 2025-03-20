package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.rest.util.MalformedArgException;

import javax.ws.rs.BadRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ArraySchema(
        schema = @Schema(implementation = String.class),
        arraySchema = @Schema(description = "List of fields to exclude from the payload. If the payload is a list of object, then the exclusion is applicable to all the listed objects."),
        minItems = 1, uniqueItems = true)
public class ExcludeArg<T> extends AbstractArrayArg<String> {

    private ExcludeArg( List<String> values ) {
        super( values );
    }

    public Set<String> getValue( Set<String> allowedValues ) throws MalformedArgException {
        if ( !allowedValues.containsAll( getValue() ) ) {
            throw new BadRequestException( String.format( "Only the following fields can be excluded: %s.",
                    String.join( ", ", allowedValues ) ) );
        }
        return new HashSet<>( getValue() );
    }

    public static ExcludeArg<?> valueOf( String s ) {
        return valueOf( s, "excluded fields", ExcludeArg::new, false );
    }
}
