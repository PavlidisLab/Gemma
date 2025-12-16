package ubic.gemma.model.common;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Base implementations for value objects representing persistent objects
 * <p>
 * Created by tesarst on 31/05/17.
 *
 * @author tesarst
 * @author poirigui
 */
@ValueObject
@EqualsAndHashCode(of = { "id" })
public abstract class IdentifiableValueObject<O extends Identifiable> implements Identifiable, Serializable {

    protected Long id = null;

    /**
     * Default empty constructor for bean-style initialization.
     */
    protected IdentifiableValueObject() {
        super();
    }

    /**
     * Constructor that sets the common property of all identifiable objects, the ID.
     *
     * @param id the id of the original object.
     */
    protected IdentifiableValueObject( Long id ) {
        this.id = id;
    }

    /**
     * Constructor for a given entity.
     */
    protected IdentifiableValueObject( O identifiable ) {
        this( identifiable.getId() );
    }

    /**
     * Copy constructor.
     */
    protected IdentifiableValueObject( IdentifiableValueObject<O> vo ) {
        this( vo.getId() );
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

    @Override
    public String toString() {
        return String.format( "%s%s", getClass().getSimpleName(),
                id != null ? " Id=" + id : "" );
    }
}
