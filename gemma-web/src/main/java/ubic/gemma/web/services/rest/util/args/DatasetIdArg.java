package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Created by tesarst on 24/05/17.
 * Long argument type for taxon API, referencing the Taxon ID.
 */
public class DatasetIdArg extends DatasetArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    DatasetIdArg( long l ) {
        this.value = l;
        this.nullCause = "The identifier was recognised to be an ID, but dataset with this ID does not exist.";
    }

    @Override
    public ExpressionExperiment getPersistentObject( ExpressionExperimentService service ) {
        return service.load( this.value );
    }
}
