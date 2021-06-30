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
        super( l );
        setNullCause( "ID", "Database Entry" );
    }

    @Override
    public DatabaseEntry getPersistentObject( DatabaseEntryService service ) {
        return check( service.load( value ) );
    }

    @Override
    public String getPropertyName( DatabaseEntryService service ) {
        return "id";
    }
}
