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
        setNullCause( "short name", "Platform" );
    }

    @Override
    public ArrayDesign getPersistentObject( ArrayDesignService service ) {
        return check( this.value == null ? null : service.findByShortName( this.value ) );
    }

    @Override
    public String getPropertyName( ArrayDesignService service ) {
        return "shortName";
    }

}
