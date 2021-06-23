package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Maps {@link FactorValue} by its factor value.
 * @author poirigui
 */
public class FactorValueValueArg extends FactorValueArg<String> {

    public FactorValueValueArg( String value ) {
        this.value = value;
    }

    @Override
    public FactorValue getPersistentObject( FactorValueService service ) {
        return service.findByValue( value ).stream().findFirst().get();
    }

    @Override
    public String getPropertyName( FactorValueService service ) {
        return "factorValueValue";
    }
}
