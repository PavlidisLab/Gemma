package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * Mutable argument type base class for DatabaseEntry API.
 *
 * @author tesarst
 */
@Schema(anyOf = { DatabaseEntryIdArg.class, DatabaseEntryStringArg.class })
public abstract class DatabaseEntryArg<T>
        extends AbstractEntityArg<T, DatabaseEntry, DatabaseEntryService> {

    DatabaseEntryArg( T value ) {
        super( value );
    }

    @Override
    public String getEntityName() {
        return "DatabaseEntry";
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request database entry argument
     * @return instance of appropriate implementation of DatabaseEntryArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatabaseEntryArg<?> valueOf( final String s ) {
        try {
            return new DatabaseEntryIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatabaseEntryStringArg( s );
        }
    }
}
