package ubic.gemma.model.annotations;

import ubic.gemma.model.util.UninitializedCollectionException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * Indicate that a collection may be intentionally uninitialized.
 * <p>
 * Operations on an uninitialized collection will always raise a {@link UninitializedCollectionException}.
 * @author poirigui
 * @see ubic.gemma.model.util.UninitializedCollection
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MayBeUninitialized {

    /**
     * Indicate that the collection may be uninitialized but always has a size.
     * <p>
     * This means that {@link Collection#size()} and {@link Collection#isEmpty()} will work as usual.
     */
    boolean hasSize() default false;
}
