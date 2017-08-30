package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * String argument type for dataset API, referencing the Dataset short name. Can also be null.
 *
 * @author tesarst
 */
public class DatasetStringArg extends DatasetArg<String> {

    DatasetStringArg( String s ) {
        this.value = s;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "short name", "Dataset" );
    }

    @Override
    public ExpressionExperiment getPersistentObject( ExpressionExperimentService service ) {
        return check( this.value == null ? null : service.findByShortName( this.value ) );
    }

}
