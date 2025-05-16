package ubic.gemma.model.common;

/**
 * Interface for objects that have a numerical id.
 * <p>
 * This is generally used by persistent entities, but it is not limited to them. Other main usage is for identifiable
 * VOs.
 * @see AbstractIdentifiable
 * @see IdentifiableValueObject
 * @author tesart
 */
public interface Identifiable {

    Long getId();
}
