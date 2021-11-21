package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import javax.ws.rs.NotFoundException;

/**
 * Represents an API arguments that maps to a {@link FactorValue} by its ID or name.
 * @author poirigui
 */
@Schema(subTypes = { FactorValueIdArg.class, FactorValueValueArg.class })
public abstract class FactorValueArg<A> extends AbstractEntityArg<A, FactorValue, FactorValueService> {

    protected FactorValueArg( A value ) {
        super( FactorValue.class, value );
    }

    protected FactorValueArg( String message, Throwable cause ) {
        super( FactorValue.class, message, cause );
    }

    public static FactorValueArg<?> valueOf( String value ) {
        if ( StringUtils.isBlank( value ) ) {
            return new FactorValueIdArg( "Factor value identifier cannot be null or empty.", null );
        }
        try {
            return new FactorValueIdArg( Long.valueOf( value ) );
        } catch ( NumberFormatException e ) {
            return new FactorValueValueArg( value );
        }
    }
}
