package ubic.gemma.model.common;

/**
 * Base class for identifiable entities.
 * <p>
 * Provide basics for holding the ID and rendering the object as a string and forces implementation to overrride
 * hashCode() and equals().
 * @author poirigui
 */
public abstract class AbstractIdentifiable implements Identifiable {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * <b>Important note:</b> Never use the ID in the hashCode() implementation since it can be assigned when the
     * object is persisted.
     */
    public abstract int hashCode();

    /**
     * <b>Important note:</b> Two objects with the same class and non-null ID must be considered equal. If one or both
     * IDs are nulls, the rest of the state can be used to determine equality.
     */
    public abstract boolean equals( Object object );

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ( this.getId() == null ? "" : " Id=" + this.getId() );
    }
}
