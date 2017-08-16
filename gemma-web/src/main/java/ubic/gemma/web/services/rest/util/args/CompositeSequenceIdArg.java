package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

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
        return check( this.value == null ? null : service.load( this.value ));
    }

}
