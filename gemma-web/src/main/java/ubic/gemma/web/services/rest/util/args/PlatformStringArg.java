package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import javax.annotation.Nonnull;

/**
 * String argument type for platform API, referencing the Platform short name. Can also be null.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A platform short name.")
public class PlatformStringArg extends PlatformArg<String> {

    PlatformStringArg( String s ) {
        super( s );
    }

    @Nonnull
    @Override
    public ArrayDesign getEntity( ArrayDesignService service ) {
        String value = this.getValue();
        return checkEntity( service, value == null ? null : service.findByShortName( value ) );
    }

    @Override
    public String getPropertyName( ArrayDesignService service ) {
        return "shortName";
    }

}
