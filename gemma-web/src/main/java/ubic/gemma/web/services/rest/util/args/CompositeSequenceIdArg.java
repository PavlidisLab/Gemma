package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.util.Objects;

/**
 * Composite Sequence argument for CS ID.
 */
public class CompositeSequenceIdArg extends CompositeSequenceArg<Long> {

    CompositeSequenceIdArg( long s ) {
        this.value = s;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "ID", "Composite Sequence" );
    }

    @Override
    public CompositeSequence getPersistentObject( CompositeSequenceService service ) {
        if( arrayDesign == null ) throw new IllegalArgumentException( "Platform not set for composite sequence retrieval" );
        CompositeSequence cs = service.load( this.value );
        if( !Objects.equals( cs.getArrayDesign().getId(), this.arrayDesign.getId() ) ) {
            throwNotFound();
        }
        return check( this.value == null ? null : cs );
    }

    @Override
    public String getPropertyName( CompositeSequenceService service ) {
        return "id";
    }

}
