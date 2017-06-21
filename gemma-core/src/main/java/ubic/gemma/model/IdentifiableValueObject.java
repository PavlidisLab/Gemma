package ubic.gemma.model;

import ubic.gemma.model.common.Identifiable;

/**
 * Created by tesarst on 31/05/17.
 * Interface for value objects representing persistent objects
 */
public abstract class IdentifiableValueObject<O extends Identifiable> {

    protected Long id = null;

    /**
     * Required when using the implementing classes as a spring beans.
     */
    public IdentifiableValueObject() {
    }

    /**
     * Constructor that sets the common property of all identifiable objects, the ID.
     *
     * @param id the id of the original object.
     */
    public IdentifiableValueObject( Long id ) {
        this.id = id;
    }

    final public Long getId() {
        return id;
    }

    /**
     * Only used by the spring java-beans in jsp files. Should be called immediately after the no-arg constructor.
     * @param id the id of this object.
     */
    public void setId( Long id ) {
        this.id = id;
    }
}
