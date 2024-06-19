package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

@ArraySchema(arraySchema = @Schema(description = DatasetArrayArg.ARRAY_SCHEMA_DESCRIPTION), schema = @Schema(implementation = DatasetArg.class), minItems = 1)
public class DatasetArrayArg
        extends AbstractEntityArrayArg<ExpressionExperiment, ExpressionExperimentService> {

    public static final String OF_WHAT = "dataset IDs or short names";
    public static final String ARRAY_SCHEMA_DESCRIPTION = ARRAY_SCHEMA_DESCRIPTION_PREFIX + OF_WHAT + ". " + ARRAY_SCHEMA_COMPRESSION_DESCRIPTION;

    private DatasetArrayArg( List<String> values ) {
        super( DatasetArg.class, values );
    }

    public static DatasetArrayArg valueOf( final String s ) throws MalformedArgException {
        return valueOf( s, OF_WHAT, DatasetArrayArg::new, true );
    }
}
