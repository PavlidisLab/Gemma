package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
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
        super( s );
    }

    @Override
    protected String getPropertyName( CompositeSequenceService service ) {
        return service.getIdentifierPropertyName();
    }

    @Override
    public CompositeSequence getEntity( CompositeSequenceService service ) {
        if ( platform == null )
            throw new BadRequestException( "Platform not set for composite sequence retrieval" );
        CompositeSequence cs = service.load( this.getValue() );
        if ( cs != null && !Objects.equals( cs.getArrayDesign().getId(), this.platform.getId() ) ) {
            throw new BadRequestException( "Platform does not match the sequence's platform." );
        }
        return checkEntity( service, cs );
    }
}
