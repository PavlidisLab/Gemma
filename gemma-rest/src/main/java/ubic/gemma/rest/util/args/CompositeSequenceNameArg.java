package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite Sequence argument for CS name. ArrayDesign property has to be populated via parent class setter before
 * getPersistentObject is called!
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A composite sequence name.")
public class CompositeSequenceNameArg extends CompositeSequenceArg<String> {

    CompositeSequenceNameArg( String s ) {
        super( "name", String.class, s );
    }

    @Override
    CompositeSequence getEntity( CompositeSequenceService service ) {
        throw new UnsupportedOperationException( "Obtaining a single entity by name without a platform is not supported. Use getEntities() or getEntityWithPlatform() instead." );
    }

    @Override
    List<CompositeSequence> getEntities( CompositeSequenceService service ) {
        return new ArrayList<>( service.findByName( getValue() ) );
    }

    @Override
    CompositeSequence getEntityWithPlatform( CompositeSequenceService service, ArrayDesign platform ) {
        return service.findByName( platform, this.getValue() );
    }
}
