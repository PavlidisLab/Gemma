package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * String argument type for dataset API, referencing the Dataset short name. Can also be null.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A dataset short name.")
public class DatasetStringArg extends DatasetArg<String> {

    DatasetStringArg( String s ) {
        super( s );
    }

    @Override
    public ExpressionExperiment getEntity( ExpressionExperimentService service ) {
        return this.checkEntity( service, Strings.isNullOrEmpty( this.getValue() ) ? null : service.findByShortName( this.getValue() ) );
    }

    @Override
    public String getPropertyName( ExpressionExperimentService service ) {
        return "shortName";
    }

}
