package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Created by tesarst on 24/05/17.
 * String argument type for dataset API, referencing the Dataset short name. Can also be null.
 */
public class DatasetStringArg extends DatasetArg<String> {

    DatasetStringArg( String s ) {
        this.value = s;
        this.nullCause = "The identifier was recognised to be a short name, but dataset with this short name does not exist or is not accessible.";
    }

    /**
     * Tries to retrieve a dataset object by its short name.
     *
     * @param service the ExpressionExperimentService that handles the search.
     * @return an ExpressionExperiment object, if found, or null, if the original argument value was null, or if the search did not find
     * any dataset that would match the string.
     */
    @Override
    public ExpressionExperiment getPersistentObject( ExpressionExperimentService service ) {
        return this.value == null ? null : service.findByShortName( this.value );
    }

}
