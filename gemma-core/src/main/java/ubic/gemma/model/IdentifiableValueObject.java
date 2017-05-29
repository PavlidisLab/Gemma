package ubic.gemma.model;

import ubic.gemma.model.common.Identifiable;

/**
 * Created by tesarst on 31/05/17.
 * Interface for value objects representing persistent objects
 */
public abstract class IdentifiableValueObject<O extends Identifiable> {

    protected Long id = null;

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
}
