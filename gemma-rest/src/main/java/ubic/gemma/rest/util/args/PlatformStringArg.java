package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * String argument type for platform API, referencing the Platform short name. Can also be null.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A platform short name.")
public class PlatformStringArg extends PlatformArg<String> {

    PlatformStringArg( String s ) {
        super( "shortName", String.class, s );
    }

    @Override
    ArrayDesign getEntity( ArrayDesignService service ) {
        return service.findByShortName( this.getValue() );
    }
}
