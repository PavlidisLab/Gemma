package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * Long argument type for DatabaseEntry API, referencing the Taxon ID.
 *
 * @author tesarst
 */
@Schema(implementation = Long.class)
public class DatabaseEntryIdArg extends DatabaseEntryArg<Long> {

    DatabaseEntryIdArg( long l ) {
        super( l );
    }

    @Override
    public DatabaseEntry getEntity( DatabaseEntryService service ) {
        return checkEntity( service.load( getValue() ) );
    }

    @Override
    public String getPropertyName() {
        return "id";
    }
}
