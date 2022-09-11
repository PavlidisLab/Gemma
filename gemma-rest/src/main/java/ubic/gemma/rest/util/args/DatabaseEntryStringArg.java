package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

import javax.annotation.Nonnull;

/**
 * String argument type for DatabaseEntry API, Can also be null.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A database entry name.")
public class DatabaseEntryStringArg extends DatabaseEntryArg<String> {

    DatabaseEntryStringArg( String s ) {
        super( s );
    }

    @Nonnull
    @Override
    public DatabaseEntry getEntity( DatabaseEntryService service ) {
        String value = getValue();
        return checkEntity( service, value == null ? null : service.findLatestByAccession( value ) );
    }

    @Override
    public String getPropertyName( DatabaseEntryService service ) {
        return "accession";
    }
}
