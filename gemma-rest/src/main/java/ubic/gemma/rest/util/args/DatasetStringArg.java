package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
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
        super( "shortName", String.class, s );
    }

    @Override
    ExpressionExperiment getEntity( ExpressionExperimentService service ) {
        return StringUtils.isEmpty( this.getValue() ) ? null : service.findByShortName( this.getValue() );
    }

    @Override
    Long getEntityId( ExpressionExperimentService service ) {
        ExpressionExperiment ee = getEntity( service );
        return ee != null ? ee.getId() : null;
    }
}
