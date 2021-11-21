package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * String argument type for DatabaseEntry API, Can also be null.
 *
 * @author tesarst
 */
@Schema(type = "string")
public class DatabaseEntryStringArg extends DatabaseEntryArg<String> {

    DatabaseEntryStringArg( String s ) {
        super( s );
    }

    DatabaseEntryStringArg( String message, Throwable cause ) {
        super( message, cause );
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
