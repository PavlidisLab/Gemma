package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

/**
 * Maps a long identifier to a {@link FactorValue}.
 *
 * @author poirigui
 */
@Schema(type = "integer", format = "int64", description = "A factor value numerical identifier.")
public class FactorValueIdArg extends FactorValueArg<Long> {

    public FactorValueIdArg( long value ) {
        super( "id", Long.class, value );
    }

    @Override
    FactorValue getEntity( FactorValueService service ) {
        return service.load( this.getValue() );
    }
}
