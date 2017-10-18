package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

/**
 * Mutable argument type base class for DatabaseEntry API.
 *
 * @author tesarst
 */
public abstract class DatabaseEntryArg<T>
        extends MutableArg<T, DatabaseEntry, DatabaseEntryValueObject, DatabaseEntryService> {

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request database entry argument
     * @return instance of appropriate implementation of DatabaseEntryArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatabaseEntryArg valueOf( final String s ) {
        try {
            return new DatabaseEntryIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatabaseEntryStringArg( s );
        }
    }
}
