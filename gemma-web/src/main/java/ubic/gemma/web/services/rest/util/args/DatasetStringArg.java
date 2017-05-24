package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Created by tesarst on 24/05/17.
 * String argument type for taxon API, referencing the Taxon scientific name, common name or abbreviation. Can also be null.
 */
public class DatasetStringArg extends DatasetArg<String> {

    DatasetStringArg( String s ) {
        this.value = s;
    }

    /**
     * Tries to retrieve a dataset object by its short name.
     *
     * @param service the ExpressionExperimentService that handles the search.
     * @return an ExpressionExperiment object, if found, or null, if the original argument value was null, or if the search did not find
     * any dataset that would match the string.
     */
    @Override
    protected ExpressionExperiment getPersistentObject( ExpressionExperimentService service ) {
        return this.value == null ? null : service.findByShortName( this.value );
    }

    /**
     * Tries to retrieve an ExpressionExperimentValueObject by the datasets short name.
     *
     * @see #getPersistentObject(ExpressionExperimentService)
     */
    @Override
    protected ExpressionExperimentValueObject getValueObject( ExpressionExperimentService service ) {
        return service.loadValueObject( this.getPersistentObject( service ).getId() );
    }
}
