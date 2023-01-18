package ubic.gemma.rest.util.args;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.rest.util.MalformedArgException;

import javax.annotation.Nullable;

/**
 * Mutable argument type base class for Composite Sequence arguments.
 *
 * @author tesarst
 */
@Getter
@Setter
@Schema(oneOf = { CompositeSequenceIdArg.class, CompositeSequenceNameArg.class })
public abstract class CompositeSequenceArg<T>
        extends AbstractEntityArg<T, CompositeSequence, CompositeSequenceService> {

    /**
     * Platform this is filtering for, or null.
     */
    @Nullable
    @JsonIgnore
    protected ArrayDesign platform;

    protected CompositeSequenceArg( T arg ) {
        super( CompositeSequence.class, arg );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Composite Sequence argument
     * @return instance of CompositeSequenceArg.
     */
    @SuppressWarnings("unused")
    public static CompositeSequenceArg<?> valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Composite sequence identifier cannot be null or empty.", null );
        }
        try {
            return new CompositeSequenceIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new CompositeSequenceNameArg( s );
        }
    }
}
