package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Mutable argument type base class for dataset (ExpressionExperiment) API.
 *
 * @author tesarst
 */
@Schema(oneOf = { DatasetIdArg.class, DatasetStringArg.class })
@CommonsLog
public abstract class DatasetArg<T>
        extends AbstractEntityArg<T, ExpressionExperiment, ExpressionExperimentService> {

    protected DatasetArg( String propertyName, Class<T> propertyType, T value ) {
        super( propertyName, propertyType, value );
    }

    @Nullable
    abstract Long getEntityId( ExpressionExperimentService service );

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static DatasetArg<?> valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Dataset identifier cannot be null or empty." );
        }
        try {
            return new DatasetIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new DatasetStringArg( s );
        }
    }
}
