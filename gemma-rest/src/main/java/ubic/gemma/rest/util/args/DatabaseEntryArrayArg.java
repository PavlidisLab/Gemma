package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

@ArraySchema(arraySchema = @Schema(description = DatabaseEntryArrayArg.ARRAY_SCHEMA_DESCRIPTION), schema = @Schema(implementation = DatabaseEntryArg.class), minItems = 1)
public class DatabaseEntryArrayArg extends AbstractEntityArrayArg<DatabaseEntry, DatabaseEntryService> {

    public static final String OF_WHAT = "database entry IDs or accessions";
    public static final String ARRAY_SCHEMA_DESCRIPTION = ARRAY_SCHEMA_DESCRIPTION_PREFIX + OF_WHAT + ". " + ARRAY_SCHEMA_COMPRESSION_DESCRIPTION;

    private DatabaseEntryArrayArg( List<String> values ) {
        super( DatabaseEntryArg.class, values );
    }

    public static DatabaseEntryArrayArg valueOf( final String s ) throws MalformedArgException {
        return valueOf( s, OF_WHAT, DatabaseEntryArrayArg::new, true );
    }
}
