package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Arrays;
import java.util.List;

public class ArrayDatabaseEntryArg extends ArrayEntityArg<DatabaseEntry, DatabaseEntryService> {

    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Database entry identifiers";

    private ArrayDatabaseEntryArg( List<String> values ) {
        super( values, DatabaseEntryArg.class );
    }

    public ArrayDatabaseEntryArg( String format, IllegalArgumentException e ) {
        super( format, e );
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_DATABASE_ENTRY_ALIAS;
    }

    @Override
    protected void setPropertyNameAndType( DatabaseEntryService service ) {

    }

    public static ArrayDatabaseEntryArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new ArrayDatabaseEntryArg( String.format( ArrayDatabaseEntryArg.ERROR_MSG, s ),
                    new IllegalArgumentException( ArrayDatabaseEntryArg.ERROR_MSG_DETAIL ) );
        }
        return new ArrayDatabaseEntryArg( Arrays.asList( ArrayEntityArg.splitString( s ) ) );
    }
}
