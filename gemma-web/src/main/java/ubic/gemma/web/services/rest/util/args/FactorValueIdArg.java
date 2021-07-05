package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Maps a long identifier to a {@link FactorValue}.
 * @author poirigui
 */
public class FactorValueIdArg extends FactorValueArg<Long> {

    public FactorValueIdArg( long value ) {
        super( value );
    }

    @Override
    public FactorValue getEntity( FactorValueService service ) {
        return checkEntity( service.load( this.getValue() ) );
    }

    @Override
    public String getPropertyName() {
        return "factorValueId";
    }
}
