package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceDao;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.MalformedArgException;
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

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayCompositeSequence argument
     * @return an instance of ArrayCompositeSequenceArg representing an array of CompositeSequence identifiers from the
     * input string, or a malformed ArrayCompositeSequenceArg that will throw an {@link javax.ws.rs.BadRequestException}
     * when accessing its value, if the input String can not be converted into an array of CompositeSequence
     * identifiers.
     */
    @SuppressWarnings("unused")
    public static CompositeSequenceArrayArg valueOf( final String s ) throws MalformedArgException {
        if ( Strings.isNullOrEmpty( s ) ) {
            throw new MalformedArgException( String.format( CompositeSequenceArrayArg.ERROR_MSG, s ),
                    new IllegalArgumentException( CompositeSequenceArrayArg.ERROR_MSG_DETAIL ) );
        }
        return new CompositeSequenceArrayArg( StringUtils.splitAndTrim( s ) );
    }

    public void setPlatform( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public ObjectFilter getPlatformFilter() {
        return ObjectFilter.parseObjectFilter( CompositeSequenceDao.OBJECT_ALIAS, "arrayDesign.id", Long.class, ObjectFilter.Operator.eq, this.arrayDesign.getId().toString() );
    }
}
