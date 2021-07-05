package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Maps {@link FactorValue} by its factor value.
 * @author poirigui
 */
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
