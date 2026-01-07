package ubic.gemma.model.annotations;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.util.UninitializedCollectionException;

import java.lang.annotation.*;
import java.util.Collection;

/**
 * Indicate that a collection or an entity may be intentionally uninitialized.
 * <p>
 * Operations on an uninitialized collection will always raise a {@link UninitializedCollectionException}.
 * <p>
 * Operations on an uninitialized entity will usually raise a {@link org.hibernate.LazyInitializationException} if
 * manipulated outside the boundary of a {@link org.hibernate.Session}.
 * <p>
 * It is safe to access the {@link Identifiable#getId()} of an uninitialized entity and the size of a collection marked
 * with {@link #hasSize()}.
 * <p>
 * Use this annotation to indicate that a method safely accepts or returns uninitialized entities, we might use this in
 * the future to perform static analysis akin to a nullability checker.
 *
 * @author poirigui
 * @see org.hibernate.proxy.HibernateProxy
 * @see ubic.gemma.model.util.ModelUtils#isInitialized(Object)
 * @see ubic.gemma.model.util.UninitializedCollection
 */
@Documented
@Target({ ElementType.TYPE_USE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MayBeUninitialized {

    /**
     * Indicate that the collection may be uninitialized but always has a size.
     * <p>
     * This means that {@link Collection#size()} and {@link Collection#isEmpty()} will work as usual.
     */
    boolean hasSize() default false;
}
