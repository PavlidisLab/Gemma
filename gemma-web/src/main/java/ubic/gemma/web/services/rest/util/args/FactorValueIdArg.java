package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import javax.annotation.Nonnull;

/**
 * Maps a long identifier to a {@link FactorValue}.
 *
 * @author poirigui
 */
@Schema(type = "integer", format = "int64", description = "A factor value numerical identifier.")
public class FactorValueIdArg extends FactorValueArg<Long> {

    public FactorValueIdArg( long value ) {
        super( value );
    }

    @Nonnull
    @Override
    public FactorValue getEntity( FactorValueService service ) {
        return checkEntity( service, service.load( this.getValue() ) );
    }

    @Override
    public String getPropertyName( FactorValueService service ) {
        return "factorValueId";
    }
}
