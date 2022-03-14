package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.web.services.rest.util.MalformedArgException;

/**
 * Class representing an API argument that should be one of the expression level consolidation options.
 *
 * @author tesarst
 */
@Schema(type = "string", allowableValues = {
        ExperimentExpressionLevelsValueObject.OPT_PICK_MAX,
        ExperimentExpressionLevelsValueObject.OPT_PICK_VAR,
        ExperimentExpressionLevelsValueObject.OPT_AVG },
        description = "An option for gene expression level consolidation.")
public class ExpLevelConsolidationArg extends AbstractArg<String> {
    private static final String ERROR_MSG = "Value '%s' can not converted to a boolean";

    private ExpLevelConsolidationArg( String value ) {
        super( value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request boolean argument
     * @return an instance of BoolArg representing boolean value of the input string, or a malformed BoolArg that will
     * throw an {@link javax.ws.rs.BadRequestException} when accessing its value, if the input String can not be
     * converted into a boolean.
     */
    @SuppressWarnings("unused")
    public static ExpLevelConsolidationArg valueOf( final String s ) {
        if ( !( s.equals( ExperimentExpressionLevelsValueObject.OPT_PICK_MAX ) || s
                .equals( ExperimentExpressionLevelsValueObject.OPT_PICK_VAR ) || s
                .equals( ExperimentExpressionLevelsValueObject.OPT_AVG ) ) ) {
            throw new MalformedArgException( String.format( ExpLevelConsolidationArg.ERROR_MSG, s ),
                    new IllegalArgumentException(
                            "The consolidate option has to be one of: " + ExperimentExpressionLevelsValueObject.OPT_AVG
                                    + ", " + ExperimentExpressionLevelsValueObject.OPT_PICK_MAX + ", "
                                    + ExperimentExpressionLevelsValueObject.OPT_PICK_VAR + "." ) );
        }
        return new ExpLevelConsolidationArg( s );
    }

}
