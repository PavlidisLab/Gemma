package ubic.gemma.model.common;

import javax.annotation.Nullable;

/**
 * Interface for objects that have a human-readable name and description.
 * @see AbstractDescribable
 * @see DescribableValueObject
 */
public interface Describable extends Identifiable {

    /**
     * Obtain a short, human-readable name of the object.
     */
    String getName();

    /**
     * Obtain a human-readable description of the object.
     */
    @Nullable
    String getDescription();
}
