package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

/**
 * Composite Sequence argument for CS name.
 * ArrayDesign property has to be populated via parent class setter before getPersistentObject is called!
 *
 * @author tesarst
 */
public class CompositeSequenceNameArg extends CompositeSequenceArg<String> {

    CompositeSequenceNameArg( String s ) {
        this.value = s;
        setNullCause("name", "Composite Sequence" );
    }

    @Override
    public CompositeSequence getPersistentObject( CompositeSequenceService service ) {
        if( arrayDesign == null ) throw new IllegalArgumentException( "Platform not set for composite sequence retrieval" );
        return check( this.value == null ? null : service.findByName( arrayDesign, this.value ) );
    }

    @Override
    public String getPropertyName( CompositeSequenceService service ) {
        return "name";
    }

}
