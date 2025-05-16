package ubic.gemma.model.util;

import org.hibernate.Hibernate;

/**
 * Utilities for working with Gemma models.
 * <p>
 * Hibernate-specific logic should be encapsulated here as much as possible.
 * @author poirigui
 */
public class ModelUtils {

    /**
     * Check if an object is initialized.
     * <p>
     * This method will check both for {@link UninitializedCollection} and Hibernate lazy proxies with {@link Hibernate#isInitialized(Object)}.
     */
    public static boolean isInitialized( Object object ) {
        if ( object instanceof UninitializedCollection ) {
            return false;
        }
        return Hibernate.isInitialized( object );
    }
}
