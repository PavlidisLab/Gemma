package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Mutable argument type base class for dataset (ExpressionExperiment) API.
 *
 * @author tesarst
 */
@Schema(oneOf = { PlatformIdArg.class, PlatformStringArg.class })
public abstract class PlatformArg<T> extends AbstractEntityArg<T, ArrayDesign, ArrayDesignService> {

    protected PlatformArg( String propertyName, Class<T> propertyType, T value ) {
        super( propertyName, propertyType, value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument.
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static PlatformArg<?> valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Platform identifier cannot be null or empty.", null );
        }
        try {
            return new PlatformIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new PlatformStringArg( s );
        }
    }
}
