package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * Long argument type for platform API, referencing the platform ID.
 *
 * @author tesarst
 */
public class PlatformIdArg extends PlatformArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    PlatformIdArg( long l ) {
        this.value = l;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "ID", "Platform" );
    }

    @Override
    public ArrayDesign getPersistentObject( ArrayDesignService service ) {
        return check( service.load( this.value ) );
    }

    @Override
    public String getPropertyName( ArrayDesignService service ) {
        return "id";
    }

}
