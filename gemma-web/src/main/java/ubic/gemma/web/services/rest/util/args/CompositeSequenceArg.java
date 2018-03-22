package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

/**
 * Mutable argument type base class for Composite Sequence arguments.
 *
 * @author tesarst
 */
public abstract class CompositeSequenceArg<T>
        extends MutableArg<T, CompositeSequence, CompositeSequenceValueObject, CompositeSequenceService> {

    protected ArrayDesign arrayDesign;

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Composite Sequence argument
     * @return instance of CompositeSequenceArg.
     */
    @SuppressWarnings("unused")
    public static MutableArg<?, CompositeSequence, CompositeSequenceValueObject, CompositeSequenceService> valueOf(
            final String s ) {
        try {
            return new CompositeSequenceIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new CompositeSequenceNameArg( s );
        }
    }

    /**
     * Sets the platform for which the persistent object should be retrieved.
     */
    public void setPlatform( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    @Override
    public abstract CompositeSequence getPersistentObject( CompositeSequenceService service );
}
