package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * String argument type for DatabaseEntry API, Can also be null.
 *
 * @author tesarst
 */
@Schema(implementation = String.class)
public class DatabaseEntryStringArg extends DatabaseEntryArg<String> {

    DatabaseEntryStringArg( String s ) {
        super( s );
    }

    @Override
    public DatabaseEntry getEntity( DatabaseEntryService service ) {
        String value = getValue();
        return checkEntity( value == null ? null : service.load( value ) );
    }

    @Override
    public String getPropertyName() {
        return "accession";
    }
}
