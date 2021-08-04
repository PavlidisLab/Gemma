package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.util.Objects;

/**
 * Composite Sequence argument for CS ID.
 */
@Schema(implementation = Long.class)
public class CompositeSequenceIdArg extends CompositeSequenceArg<Long> {

    CompositeSequenceIdArg( long s ) {
        super( s );
    }

    @Override
    public CompositeSequence getEntity( CompositeSequenceService service ) {
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Platform not set for composite sequence retrieval" );
        CompositeSequence cs = service.load( this.getValue() );
        if ( !Objects.equals( cs.getArrayDesign().getId(), this.arrayDesign.getId() ) ) {
            throw new IllegalArgumentException( "Platform does not match the sequence's platform." );
        }
        return checkEntity( this.getValue() == null ? null : cs );
    }

    @Override
    public String getPropertyName() {
        return "id";
    }

    @Override
    public String getEntityName() {
        return "CompositeSequence";
    }

}
