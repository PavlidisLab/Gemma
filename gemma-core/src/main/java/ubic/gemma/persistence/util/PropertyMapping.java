package ubic.gemma.persistence.util;

import javax.annotation.Nullable;

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
     * Obtain the full property in the HQL space.
     */
    default String getProperty() {
        if ( getObjectAlias() != null ) {
            return getObjectAlias() + "." + getPropertyName();
        } else {
            return getPropertyName();
        }
    }

    /**
     * Obtain the original property, if available.
     */
    @Nullable
    String getOriginalProperty();

    String toOriginalString();
}
