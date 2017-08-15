package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

/**
 * Composite sequence argument for CS name.
 * ArrayDesign property has to be populated via parent class setter before getPersistentObject is called!
 */
public class CompositeSequenceNameArg extends CompositeSequenceArg<String> {

    CompositeSequenceNameArg( String s ) {
        this.value = s;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "name", "Composite Sequence" );
    }

    @Override
    public CompositeSequence getPersistentObject( CompositeSequenceService service ) {
        assert ( arrayDesign != null );
        return check( this.value == null ? null : service.findByName( arrayDesign, this.value ) );
    }

}
