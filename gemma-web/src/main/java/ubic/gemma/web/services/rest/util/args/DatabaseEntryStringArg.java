package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * Created by tesarst on 25/05/17.
 * String argument type for DatabaseEntry API, Can also be null.
 */
public class DatabaseEntryStringArg extends DatabaseEntryArg<String> {

    DatabaseEntryStringArg( String s ) {
        this.value = s;
        this.nullCause = "The identifier was recognised to be an accession ID, but database entry with this accession does not exist.";
    }

    @Override
    public DatabaseEntryValueObject getValueObject( DatabaseEntryService service ) {
        return this.value == null ? null : new DatabaseEntryValueObject( this.getPersistentObject( service ) );
    }

    @Override
    public DatabaseEntry getPersistentObject( DatabaseEntryService service ) {
        return this.value == null ? null : service.load( this.value );
    }
}
