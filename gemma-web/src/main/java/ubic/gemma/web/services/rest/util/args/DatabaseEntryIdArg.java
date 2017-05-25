package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * Created by tesarst on 25/05/17.
 * Long argument type for DatabaseEntry API, referencing the Taxon ID.
 */
public class DatabaseEntryIdArg extends DatabaseEntryArg<Long> {

    DatabaseEntryIdArg( long l ) {
        this.value = l;
        this.nullCause = "The identifier was recognised to be an ID, but database entry with this ID does not exist.";
    }

    @Override
    public DatabaseEntryValueObject getValueObject( DatabaseEntryService service ) {
        return new DatabaseEntryValueObject( this.getPersistentObject( service ) );
    }

    @Override
    public DatabaseEntry getPersistentObject( DatabaseEntryService service ) {
        return service.load( value );
    }
}
