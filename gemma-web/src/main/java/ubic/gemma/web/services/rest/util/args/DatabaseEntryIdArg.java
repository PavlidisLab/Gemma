package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * Long argument type for DatabaseEntry API, referencing the Taxon ID.
 *
 * @author tesarst
 */
@Schema(type = "integer", format = "int64", description = "A database entry numerical identifier.")
public class DatabaseEntryIdArg extends DatabaseEntryArg<Long> {

    DatabaseEntryIdArg( long l ) {
        super( l );
    }

    @Override
    protected String getPropertyName( DatabaseEntryService service ) {
        return service.getIdentifierPropertyName();
    }

    @Override
    public DatabaseEntry getEntity( DatabaseEntryService service ) {
        return checkEntity( service, service.load( getValue() ) );
    }
}
