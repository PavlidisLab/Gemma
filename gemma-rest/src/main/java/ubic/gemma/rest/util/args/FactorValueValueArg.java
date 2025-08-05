package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Maps {@link FactorValue} by its factor value.
 *
 * @author poirigui
 */
@Deprecated
@Schema(type = "string", description = "The value of a factor value.")
public class FactorValueValueArg extends FactorValueArg<String> {

    public FactorValueValueArg( String value ) {
        super( "value", String.class, value );
    }

    @Override
    FactorValue getEntity( FactorValueService service ) {
        return service.findByValue( getValue(), 1 ).iterator().next();
    }
}
