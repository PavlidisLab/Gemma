package ubic.gemma.model.common;

import javax.annotation.Nullable;

/**
 * Interface for entities that have a name and description.
 *
 * @see AbstractDescribable
 * @see DescribableUtils
 */
public interface Describable extends Identifiable {

    /**
     * Obtain the name of the object.
     * <p>
     * It may be human-readable and case-insensitive. It is usually unique within a certain context (e.g. in a
     * collection).
     * <p>
     * It is non-null by default, but implementation may override this with a {@link Nullable} annotation. If null, it
     * should not be treated as equal to other {@link Describable} objects.
     */
    String getName();

    /**
     * Obtain a human-readable description of the object
     */
    @Nullable
    String getDescription();
}
