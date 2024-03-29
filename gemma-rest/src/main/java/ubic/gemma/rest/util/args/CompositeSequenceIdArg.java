package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import javax.ws.rs.BadRequestException;
import java.util.Objects;

/**
 * Composite Sequence argument for CS ID.
 */
@Schema(type = "integer", format = "int64", description = "A composite sequence numerical identifier.")
public class CompositeSequenceIdArg extends CompositeSequenceArg<Long> {

    CompositeSequenceIdArg( long s ) {
        super( "id", Long.class, s );
    }

    @Override
    CompositeSequence getEntity( CompositeSequenceService service ) {
        return service.load( this.getValue() );
    }

    @Override
    CompositeSequence getEntityWithPlatform( CompositeSequenceService service, ArrayDesign platform ) {
        CompositeSequence cs = getEntity( service );
        if ( cs != null && !Objects.equals( cs.getArrayDesign().getId(), platform.getId() ) ) {
            throw new BadRequestException( "Platform does not match the sequence's platform." );
        }
        return cs;
    }
}
