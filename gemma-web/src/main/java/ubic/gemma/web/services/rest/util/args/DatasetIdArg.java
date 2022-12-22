package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nonnull;

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
        super( l );
    }

    @Override
    protected String getPropertyName( ExpressionExperimentService service ) {
        return service.getIdentifierPropertyName();
    }

    @Nonnull
    @Override
    public ExpressionExperiment getEntity( ExpressionExperimentService service ) {
        return checkEntity( service, service.load( this.getValue() ) );
    }
}
