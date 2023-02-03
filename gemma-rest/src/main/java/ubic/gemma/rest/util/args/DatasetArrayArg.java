package ubic.gemma.rest.util.args;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

@ArraySchema(schema = @Schema(implementation = DatasetArg.class))
public class DatasetArrayArg
        extends AbstractEntityArrayArg<String, ExpressionExperiment, ExpressionExperimentService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Dataset identifiers";

    private DatasetArrayArg( List<String> values ) {
        super( DatasetArg.class, values );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayDataset argument
     * @return an instance of ArrayDatasetArg representing an array of Dataset identifiers from the input string, or a
     * malformed ArrayDatasetArg that will throw an {@link javax.ws.rs.BadRequestException} when accessing its value, if
     * the input String can not be converted into an array of Dataset identifiers.
     */
    @SuppressWarnings("unused")
    public static DatasetArrayArg valueOf( final String s ) throws MalformedArgException {
        if ( Strings.isNullOrEmpty( s ) ) {
            throw new MalformedArgException( String.format( DatasetArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( DatasetArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new DatasetArrayArg( splitAndTrim( s ) );
    }

}
