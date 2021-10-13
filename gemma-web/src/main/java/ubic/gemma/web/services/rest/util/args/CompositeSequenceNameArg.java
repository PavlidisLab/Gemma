package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

/**
 * Composite Sequence argument for CS name.
 * ArrayDesign property has to be populated via parent class setter before getPersistentObject is called!
 *
 * @author tesarst
 */
@Schema(type = "string")
public class CompositeSequenceNameArg extends CompositeSequenceArg<String> {

    CompositeSequenceNameArg( String s ) {
        super( s );
    }

    @Override
    public CompositeSequence getEntity( CompositeSequenceService service ) {
        if ( arrayDesign == null )
            throw new IllegalArgumentException( "Platform not set for composite sequence retrieval" );
        return checkEntity( this.getValue() == null ? null : service.findByName( arrayDesign, this.getValue() ) );
    }

    @Override
    public String getPropertyName() {
        return "name";
    }

}
