package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Long argument type for dataset API, referencing the Dataset ID.
 *
 * @author tesarst
 */
@Schema(type = "integer", format = "int64", description = "A numerical dataset identifier.")
public class DatasetIdArg extends DatasetArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    public DatasetIdArg( long l ) {
        super( "id", Long.class, l );
    }

    @Override
    ExpressionExperiment getEntity( ExpressionExperimentService service ) {
        return service.load( this.getValue() );
    }

    @Override
    Long getEntityId( ExpressionExperimentService service ) {
        return this.getValue();
    }
}
