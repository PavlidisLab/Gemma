package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * String argument type for dataset API, referencing the Dataset short name. Can also be null.
 *
 * @author tesarst
 */
public class DatasetStringArg extends DatasetArg<String> {

    DatasetStringArg( String s ) {
        super( s );
        this.setNullCause( "short name", "Dataset" );
    }

    @Override
    public ExpressionExperiment getPersistentObject( ExpressionExperimentService service ) {
        return this.check( Strings.isNullOrEmpty( this.value ) ? null : service.findByShortName( this.value ) );
    }

    @Override
    public String getPropertyName( ExpressionExperimentService service ) {
        return "shortName";
    }

}
