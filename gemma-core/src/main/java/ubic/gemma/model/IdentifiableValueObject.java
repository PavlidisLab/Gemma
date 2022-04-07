package ubic.gemma.model;

import lombok.EqualsAndHashCode;
import ubic.gemma.model.common.Identifiable;

/**
 * Created by tesarst on 31/05/17.
 * Interface for value objects representing persistent objects
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Frontend use
@EqualsAndHashCode(of = { "id" })
public abstract class IdentifiableValueObject<O extends Identifiable> implements Identifiable {

    private Long id;

    /**
     * Constructor that sets the common property of all identifiable objects, the ID.
     * @param id the id of the original object.
     */
    protected IdentifiableValueObject( Long id ) {
        this.id = id;
    }

    protected IdentifiableValueObject( O identifiable ) {
        this.id = identifiable.getId();
    }

    protected IdentifiableValueObject( IdentifiableValueObject vo ) {
        this.id = vo.getId();
    }

    @Override
    final public Long getId() {
        return id;
    }

    /**
     * Only used by the spring java-beans in jsp files. Should be called immediately after the no-arg constructor.
     *
     * @param id the id of this object.
     */
    final public void setId( Long id ) {
        this.id = id;
    }
}
