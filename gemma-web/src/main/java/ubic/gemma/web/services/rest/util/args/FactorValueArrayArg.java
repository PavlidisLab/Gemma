package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

@ArraySchema(schema = @Schema(implementation = FactorValueArg.class))
public class FactorValueArrayArg extends AbstractEntityArrayArg<String, FactorValue, FactorValueService> {

    public FactorValueArrayArg( List<String> values ) {
        super( FactorValueArg.class, values );
    }

    public static FactorValueArrayArg valueOf( String s ) {
        return new FactorValueArrayArg( StringUtils.splitAndTrim( s ) );
    }

}
