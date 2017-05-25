package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Created by tesarst on 24/05/17.
 * Mutable argument type base class for dataset (ExpressionExperiment) API
 */
public abstract class DatasetArg<T>
        extends MutableArg<T, ExpressionExperiment, ExpressionExperimentService, ExpressionExperimentValueObject> {

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
}
