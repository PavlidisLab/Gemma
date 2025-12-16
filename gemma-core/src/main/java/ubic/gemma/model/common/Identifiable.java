package ubic.gemma.model.common;

import javax.annotation.Nullable;

/**
 * Interface for objects that have a numerical id.
 * <p>
 * This is generally used by persistent entities, but it is not limited to them. Other main usage is for identifiable
 * VOs.
 * <p>
 * It intentionally does not implement the {@link java.io.Serializable} and subclasses should refrain from doing so.
 * Instead, value objects deriving {@link IdentifiableValueObject} should be used to create serializable
 * representations.
 * <p>
 * Hash code should never be based on the identifier as it may change throughout the lifetime of the object. On the
 * other hand, equality can take advantage of the identifier when it is not null to speed-up comparisons.
 *
 * @author tesart
 * @see AbstractIdentifiable
 * @see IdentifiableValueObject
 * @see ubic.gemma.persistence.util.IdentifiableUtils
 */
public interface Identifiable {

    /**
     * Obtain the identifier of the object.
     */
    @Nullable
    Long getId();
}
