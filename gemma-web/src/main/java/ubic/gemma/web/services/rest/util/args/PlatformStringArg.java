package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * String argument type for platform API, referencing the Platform short name. Can also be null.
 *
 * @author tesarst
 */
public class PlatformStringArg extends PlatformArg<String> {

    PlatformStringArg( String s ) {
        super( s );
    }

    @Override
    public ArrayDesign getEntity( ArrayDesignService service ) {
        String value = this.getValue();
        return checkEntity( value == null ? null : service.findByShortName( value ) );
    }

    @Override
    public String getPropertyName() {
        return "shortName";
    }

}
