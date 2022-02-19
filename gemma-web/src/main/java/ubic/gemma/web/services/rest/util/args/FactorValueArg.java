package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import javax.ws.rs.NotFoundException;

/**
 * Represents an API arguments that maps to a {@link FactorValue} by its ID or name.
 *
 * @author poirigui
 */
@Schema(subTypes = { FactorValueIdArg.class, FactorValueValueArg.class })
public abstract class FactorValueArg<A> extends AbstractEntityArg<A, FactorValue, FactorValueService> {

    protected FactorValueArg( A value ) {
        super( FactorValue.class, value );
    }

    public static FactorValueArg<?> valueOf( String value ) {
        if ( StringUtils.isBlank( value ) ) {
            throw new MalformedArgException( "Factor value identifier cannot be null or empty.", null );
        }
        try {
            return new FactorValueIdArg( Long.parseLong( value ) );
        } catch ( NumberFormatException e ) {
            return new FactorValueValueArg( value );
        }
    }
}
