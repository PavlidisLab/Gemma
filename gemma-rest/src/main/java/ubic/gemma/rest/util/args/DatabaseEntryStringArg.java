package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * String argument type for DatabaseEntry API, Can also be null.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A database entry name.")
public class DatabaseEntryStringArg extends DatabaseEntryArg<String> {

    DatabaseEntryStringArg( String s ) {
        super( "accession", String.class, s );
    }

    @Override
    DatabaseEntry getEntity( DatabaseEntryService service ) {
        return service.findLatestByAccession( getValue() );
    }
}
