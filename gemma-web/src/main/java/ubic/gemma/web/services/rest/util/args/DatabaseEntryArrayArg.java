package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

@ArraySchema(schema = @Schema(implementation = DatabaseEntryArg.class))
public class DatabaseEntryArrayArg extends AbstractEntityArrayArg<DatabaseEntry, DatabaseEntryService> {

    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Database entry identifiers";

    private DatabaseEntryArrayArg( List<String> values ) {
        super( values );
    }

    @Override
    protected Class<? extends AbstractEntityArg> getEntityArgClass() {
        return DatabaseEntryArg.class;
    }

    public DatabaseEntryArrayArg( String format, IllegalArgumentException e ) {
        super( format, e );
    }

    public static DatabaseEntryArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new DatabaseEntryArrayArg( String.format( DatabaseEntryArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( DatabaseEntryArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new DatabaseEntryArrayArg( StringUtils.splitAndTrim( s ) );
    }
}
