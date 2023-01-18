package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

import static ubic.gemma.rest.util.StringUtils.splitAndTrim;

@ArraySchema(schema = @Schema(implementation = DatabaseEntryArg.class))
public class DatabaseEntryArrayArg extends AbstractEntityArrayArg<String, DatabaseEntry, DatabaseEntryService> {

    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Database entry identifiers";

    private DatabaseEntryArrayArg( List<String> values ) {
        super( DatabaseEntryArg.class, values );
    }

    public static DatabaseEntryArrayArg valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( String.format( DatabaseEntryArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( DatabaseEntryArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new DatabaseEntryArrayArg( splitAndTrim( s ) );
    }
}
