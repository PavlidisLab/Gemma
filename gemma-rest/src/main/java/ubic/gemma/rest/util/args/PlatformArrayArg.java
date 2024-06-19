package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

@ArraySchema(arraySchema = @Schema(description = PlatformArrayArg.ARRAY_SCHEMA_DESCRIPTION), schema = @Schema(implementation = PlatformArg.class), minItems = 1)
public class PlatformArrayArg extends AbstractEntityArrayArg<ArrayDesign, ArrayDesignService> {

    public static final String OF_WHAT = "platform IDs or short names";
    public static final String ARRAY_SCHEMA_DESCRIPTION = ARRAY_SCHEMA_DESCRIPTION_PREFIX + OF_WHAT + ". " + ARRAY_SCHEMA_COMPRESSION_DESCRIPTION;

    private PlatformArrayArg( List<String> values ) {
        super( PlatformArg.class, values );
    }

    public static PlatformArrayArg valueOf( final String s ) throws MalformedArgException {
        return valueOf( s, OF_WHAT, PlatformArrayArg::new, true );
    }
}
