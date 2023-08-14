package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * Long argument type for platform API, referencing the platform ID.
 *
 * @author tesarst
 */
@Schema(type = "integer", format = "int64", description = "A platform numerical identifier.")
public class PlatformIdArg extends PlatformArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    PlatformIdArg( long l ) {
        super( "id", Long.class, l );
    }

    @Override
    ArrayDesign getEntity( ArrayDesignService service ) {
        return service.load( this.getValue() );
    }
}
