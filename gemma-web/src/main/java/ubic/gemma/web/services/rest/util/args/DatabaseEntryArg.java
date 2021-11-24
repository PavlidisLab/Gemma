package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

import javax.ws.rs.NotFoundException;

/**
 * Mutable argument type base class for DatabaseEntry API.
 *
 * @author tesarst
 */
@Schema(subTypes = { DatabaseEntryIdArg.class, DatabaseEntryStringArg.class })
public abstract class DatabaseEntryArg<T>
        extends AbstractEntityArg<T, DatabaseEntry, DatabaseEntryService> {

    protected DatabaseEntryArg( T value ) {
        super( DatabaseEntry.class, value );
    }

    protected DatabaseEntryArg( String message, Throwable cause ) {
        super( DatabaseEntry.class, message, cause );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request database entry argument
     * @return instance of appropriate implementation of DatabaseEntryArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatabaseEntryArg<?> valueOf( final String s ) {
        if ( StringUtils.isBlank( s ) ) {
            return new DatabaseEntryStringArg( "Database entry cannot be null or empty.", null );
        }
        try {
            return new DatabaseEntryIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatabaseEntryStringArg( s );
        }
    }
}
