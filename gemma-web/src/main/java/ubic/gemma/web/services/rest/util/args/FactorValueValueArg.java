package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Maps {@link FactorValue} by its factor value.
 * @author poirigui
 */
@Schema(type = "string")
public class FactorValueValueArg extends FactorValueArg<String> {

    public FactorValueValueArg( String value ) {
        super( value );
    }

    @Override
    public FactorValue getEntity( FactorValueService service ) {
        return checkEntity( service.findByValue( getValue() ).stream().findFirst().get() );
    }

    @Override
    public String getPropertyName() {
        return "value";
    }
}
