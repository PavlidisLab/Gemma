package ubic.gemma.model.util;

import org.hibernate.Hibernate;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utilities for working with Gemma models.
 * <p>
 * Hibernate-specific logic should be encapsulated here as much as possible.
 *
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

    public enum EqualityOutcome {
        EQUAL,
        NOT_EQUAL,
        /**
         * Equality could not be determined.
         */
        UNDETERMINED
    }

    /**
     * Compare two object without initializing them.
     */
    public static EqualityOutcome equals( Object object, Object other ) {
        if ( object == other ) {
            return EqualityOutcome.EQUAL;
        }
        if ( isInitialized( object ) && isInitialized( other ) ) {
            return Objects.equals( object, other ) ? EqualityOutcome.EQUAL : EqualityOutcome.NOT_EQUAL;
        } else if ( object instanceof Collection ) {
            if ( other instanceof Collection ) {
                return equals( ( Collection<?> ) object, ( Collection<?> ) other );
            } else {
                return EqualityOutcome.NOT_EQUAL;
            }
        } else {
            return EqualityOutcome.UNDETERMINED;
        }
    }

    /**
     * Compare two potentially uninitialized collections.
     * <p>
     * At least one input is uninitialized.
     */
    private static EqualityOutcome equals( Collection<?> a, Collection<?> b ) {
        if ( a instanceof List ) {
            if ( b instanceof List ) {
                if ( isSized( a ) && isSized( b ) ) {
                    if ( a.isEmpty() && b.isEmpty() ) {
                        return EqualityOutcome.EQUAL;
                    } else if ( a.size() != b.size() ) {
                        return EqualityOutcome.NOT_EQUAL;
                    } else {
                        return EqualityOutcome.UNDETERMINED;
                    }
                } else {
                    return EqualityOutcome.UNDETERMINED;
                }
            } else {
                return EqualityOutcome.NOT_EQUAL;
            }
        } else if ( a instanceof Set ) {
            if ( b instanceof Set ) {
                if ( isSized( a ) && isSized( b ) ) {
                    if ( a.isEmpty() && b.isEmpty() ) {
                        return EqualityOutcome.EQUAL;
                    } else if ( a.size() != b.size() ) {
                        return EqualityOutcome.NOT_EQUAL;
                    } else {
                        return EqualityOutcome.UNDETERMINED;
                    }
                } else {
                    return EqualityOutcome.UNDETERMINED;
                }
            } else {
                return EqualityOutcome.NOT_EQUAL;
            }
        } else {
            // since at least one input is uninitialized, the outcome is undetermined
            return EqualityOutcome.UNDETERMINED;
        }
    }

    private static boolean isSized( Collection<?> collection ) {
        return !( collection instanceof UninitializedCollection ) || ( ( UninitializedCollection<?> ) collection ).sized();
    }
}
