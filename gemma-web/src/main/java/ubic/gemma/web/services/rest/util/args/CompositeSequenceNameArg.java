package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import javax.ws.rs.BadRequestException;

/**
 * Composite Sequence argument for CS name. ArrayDesign property has to be populated via parent class setter before
 * getPersistentObject is called!
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A composite sequence name.")
public class CompositeSequenceNameArg extends CompositeSequenceArg<String> {

    CompositeSequenceNameArg( String s ) {
        super( s );
    }

    @Override
    public CompositeSequence getEntity( CompositeSequenceService service ) {
        if ( platform == null )
            throw new BadRequestException( "Platform not set for composite sequence retrieval" );
        return checkEntity( service, this.getValue() == null ? null : service.findByName( platform, this.getValue() ) );
    }

    @Override
    public String getPropertyName( CompositeSequenceService service ) {
        return "name";
    }

}
