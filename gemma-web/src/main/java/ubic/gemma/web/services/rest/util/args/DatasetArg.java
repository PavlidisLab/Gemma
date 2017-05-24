package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Created by tesarst on 24/05/17.
 * Mutable argument type base class for Dataset API
 */
public abstract class DatasetArg<T> extends MutableArg<T, ExpressionExperiment, ExpressionExperimentService, ExpressionExperimentValueObject> {

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatasetArg valueOf( final String s ) {
        try {
            return new DatasetIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatasetStringArg( s );
        }
    }

    /**
     * Retrieves the Taxon using the implementation of the taxonArg object.
     * @param datasetArg Implementation of DatasetArg to use for the Dataset retrieval.
     * @param service the service to use for the taxon retrieval.
     * @return a Dataset (ExpressionExperiment), if found, null otherwise.
     */
    public static ExpressionExperimentValueObject getValueObject( final DatasetArg datasetArg, ExpressionExperimentService service ) {
        try {
            return datasetArg.getValueObject( service );
        } catch ( NullPointerException e ) {
            return null;
        }
    }

    @Override
    protected abstract ExpressionExperiment getPersistentObject( ExpressionExperimentService service );

    @Override
    protected abstract ExpressionExperimentValueObject getValueObject( ExpressionExperimentService service );
}
