package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.util.Objects;

/**
 * Mutable argument type base class for Composite Sequence arguments.
 *
 * @author tesarst
 */
public abstract class CompositeSequenceArg<T>
        extends MutableArg<T, CompositeSequence, CompositeSequenceService, CompositeSequenceValueObject> {

    protected ArrayDesign arrayDesign;

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Composite Sequence argument
     * @return instance of CompositeSequenceArg.
     */
    @SuppressWarnings("unused")
    public static CompositeSequenceArg valueOf( final String s ) {
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

    /**
     * Gets a value object and checks if its platform is the same as the one set by setPlatform method.
     * @param service the service to use for the value object retrieval.
     * @return a composite sequence with the identifier represented by this Arg object if its platform id matches
     * the id set by the setPlatform method, or null.
     */
    public CompositeSequenceValueObject getVoForPlatform(CompositeSequenceService service){
        assert(this.arrayDesign != null);
        CompositeSequenceValueObject vo = this.getValueObject( service );
        if(!Objects.equals( vo.getArrayDesign().getId(), this.arrayDesign.getId() )) throwNotFound();
        return vo;
    }

    @Override
    public abstract CompositeSequence getPersistentObject( CompositeSequenceService service );
}
