package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * Long argument type for DatabaseEntry API, referencing the Taxon ID.
 *
 * @author tesarst
 */
public class DatabaseEntryIdArg extends DatabaseEntryArg<Long> {

    DatabaseEntryIdArg( long l ) {
        this.value = l;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "ID", "Database Entry" );
    }

    @Override
    public DatabaseEntry getPersistentObject( DatabaseEntryService service ) {
        return check(service.load( value ));
    }
}
