package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Mutable argument type base class for DatabaseEntry API.
 *
 * @author tesarst
 */
@Schema(oneOf = { DatabaseEntryIdArg.class, DatabaseEntryStringArg.class })
public abstract class DatabaseEntryArg<T>
        extends AbstractEntityArg<T, DatabaseEntry, DatabaseEntryService> {

    protected DatabaseEntryArg( T value ) {
        super( DatabaseEntry.class, value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request database entry argument
     * @return instance of appropriate implementation of DatabaseEntryArg based on the actual Type the argument
     * represents.
     */
    @SuppressWarnings("unused")
    public static DatabaseEntryArg<?> valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Database entry cannot be null or empty." );
        }
        try {
            return new DatabaseEntryIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatabaseEntryStringArg( s );
        }
    }
}
