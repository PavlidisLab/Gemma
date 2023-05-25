package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Represents an API arguments that maps to a {@link FactorValue} by its ID or name.
 *
 * @author poirigui
 */
@Schema(oneOf = { FactorValueIdArg.class, FactorValueValueArg.class })
public abstract class FactorValueArg<A> extends AbstractEntityArg<A, FactorValue, FactorValueService> {

    protected FactorValueArg( String propertyName, Class<A> propertyType, A value ) {
        super( propertyName, propertyType, value );
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
