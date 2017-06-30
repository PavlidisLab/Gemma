package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * Created by tesarst on 24/05/17.
 * Long argument type for platform API, referencing the platform ID.
 */
public class PlatformIdArg extends PlatformArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    PlatformIdArg( long l ) {
        this.value = l;
        this.nullCause = "The identifier was recognised to be an ID, but dataset with this ID does not exist.";
    }

    @Override
    public ArrayDesign getPersistentObject( ArrayDesignService service ) {
        return service.load( this.value );
    }
}
