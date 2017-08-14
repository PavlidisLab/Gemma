package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

/**
 * Created by tesarst on 25/05/17.
 * Mutable argument type base class for Composite Sequence arguments.
 */
public class CompositeSequenceArg
        extends MutableArg<String, CompositeSequence, CompositeSequenceService, CompositeSequenceValueObject> {

    private ArrayDesign arrayDesign;

    private CompositeSequenceArg( String s ) {
        this.value = s;
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, "name", "Composite Sequence" );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Composite Sequence argument
     * @return instance of CompositeSequenceArg.
     */
    @SuppressWarnings("unused")
    public static CompositeSequenceArg valueOf( final String s ) {
        return new CompositeSequenceArg( s );
    }

    @Override
    public CompositeSequence getPersistentObject( CompositeSequenceService service ) {
        assert ( arrayDesign != null );
        return check( this.value == null ? null : service.findByName( arrayDesign, this.value ) );
    }

    /**
     * Sets the platform for which the persistent object should be retrieved.
     */
    public void setPlatform( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }
}
