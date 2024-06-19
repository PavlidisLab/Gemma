package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceDao;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;

@ArraySchema(arraySchema = @Schema(description = CompositeSequenceArrayArg.ARRAY_SCHEMA_DESCRIPTION), schema = @Schema(implementation = CompositeSequenceArg.class), minItems = 1)
public class CompositeSequenceArrayArg
        extends AbstractEntityArrayArg<CompositeSequence, CompositeSequenceService> {

    public static final String OF_WHAT = "element IDs or names";

    public static final String ARRAY_SCHEMA_DESCRIPTION =
            AbstractArrayArg.ARRAY_SCHEMA_DESCRIPTION_PREFIX + CompositeSequenceArrayArg.OF_WHAT + ". "
                    + AbstractArrayArg.ARRAY_SCHEMA_COMPRESSION_DESCRIPTION;

    private ArrayDesign arrayDesign;

    private CompositeSequenceArrayArg( List<String> values ) {
        super( CompositeSequenceArg.class, values );
    }

    public void setPlatform( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public Filter getPlatformFilter() {
        return Filter.parse( CompositeSequenceDao.OBJECT_ALIAS, "arrayDesign.id", Long.class, Filter.Operator.eq, this.arrayDesign.getId().toString() );
    }

    public static CompositeSequenceArrayArg valueOf( final String s ) throws MalformedArgException {
        return valueOf( s, OF_WHAT, CompositeSequenceArrayArg::new, true );
    }
}
