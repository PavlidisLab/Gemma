package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * String argument type for DatabaseEntry API, Can also be null.
 *
 * @author tesarst
 */
public class DatabaseEntryStringArg extends DatabaseEntryArg<String> {

    DatabaseEntryStringArg( String s ) {
        this.value = s;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "Accession ID", "Database Entry" );
    }

    @Override
    public DatabaseEntry getPersistentObject( DatabaseEntryService service ) {
        return check( this.value == null ? null : service.load( this.value ) );
    }
}
