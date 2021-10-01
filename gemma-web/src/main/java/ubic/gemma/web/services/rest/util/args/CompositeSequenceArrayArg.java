package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.SneakyThrows;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.ObjectFilterException;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.StringUtils;

import java.util.List;

@ArraySchema(schema = @Schema(implementation = CompositeSequenceArg.class))
public class CompositeSequenceArrayArg
        extends AbstractEntityArrayArg<String, CompositeSequence, CompositeSequenceService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one "
            + "element ID or name, or multiple, separated by (',') character. "
            + "All identifiers must be same type, i.e. do not combine IDs and names in one query.";
    private static final String ERROR_MSG = AbstractArrayArg.ERROR_MSG + " Element identifiers";

    private ArrayDesign arrayDesign;

    private CompositeSequenceArrayArg( List<String> values ) {
        super( CompositeSequenceArg.class, values );
    }

    private CompositeSequenceArrayArg( String errorMessage, Exception exception ) {
        super( CompositeSequenceArg.class, errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param  s the request arrayCompositeSequence argument
     * @return an instance of ArrayCompositeSequenceArg representing an array of CompositeSequence identifiers from
     *           the input string,
     *           or a malformed ArrayCompositeSequenceArg that will throw an {@link javax.ws.rs.BadRequestException} when accessing
     *           its value, if the
     *           input String can not be converted into an array of CompositeSequence identifiers.
     */
    @SuppressWarnings("unused")
    public static CompositeSequenceArrayArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new CompositeSequenceArrayArg( String.format( CompositeSequenceArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( CompositeSequenceArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new CompositeSequenceArrayArg( StringUtils.splitAndTrim( s ) );
    }

    public void setPlatform( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    @SneakyThrows(ObjectFilterException.class)
    public List<ObjectFilter[]> getPlatformFilter() {
        ObjectFilter filter = new ObjectFilter( ObjectFilter.DAO_PROBE_ALIAS, "arrayDesign.id", Long.class, ObjectFilter.Operator.is, this.arrayDesign.getId().toString() );
        return ObjectFilter.singleFilter( filter );
    }

}
