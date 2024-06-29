package ubic.gemma.persistence.util;

import ubic.gemma.core.lang.Nullable;

/**
 * Represents a mapping between a query/criteria property and some original property space.
 * @author poirigui
 */
public interface PropertyMapping {

    /**
     * Alias in the query/criteria space, if applicable.
     * <p>
     * Null implies a reference to the root alias.
     */
    @Nullable
    String getObjectAlias();

    /**
     * Property name in the query/criteria space.
     */
    String getPropertyName();

    /**
     * Obtain the original property, if available.
     */
    @Nullable
    String getOriginalProperty();

    /**
     * Render this with its original property.
     * <p>
     * If no original property are attached to this mapping, this method should return the same as {@link #toString()}.
     */
    String toOriginalString();

    /**
     * Render this with its {@link #getObjectAlias()} and {@link #getPropertyName()}.
     */
    String toString();
}
