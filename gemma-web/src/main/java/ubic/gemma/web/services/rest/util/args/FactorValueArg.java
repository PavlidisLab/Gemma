package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Represents an API arguments that maps to a {@link FactorValue} by its ID or name.
 * @author poirigui
 */
@Schema(subTypes = { FactorValueIdArg.class, FactorValueValueArg.class })
public abstract class FactorValueArg<A> extends AbstractEntityArg<A, FactorValue, FactorValueService> {

    protected FactorValueArg( A value ) {
        super( FactorValue.class, value );
    }

    public static FactorValueArg<?> valueOf( String value ) {
        try {
            return new FactorValueIdArg( Long.valueOf( value ) );
        } catch ( NumberFormatException e ) {
            return new FactorValueValueArg( value );
        }
    }
}
