package ubic.gemma.persistence.hibernate;

import org.hibernate.transform.ResultTransformer;
import ubic.gemma.core.lang.Nullable;

import java.util.List;

/**
 * Overrides Hibernate {@link ResultTransformer} interface to include type safety.
 * @param <T> the type this transformer produces
 */
public interface TypedResultTransformer<T> extends ResultTransformer {

    @Nullable
    @Override
    T transformTuple( Object[] tuple, String[] aliases );

    /**
     * @deprecated Use {@link #transformListTyped(List)} instead.
     */
    @Override
    @Deprecated
    default List<T> transformList( List collection ) {
        //noinspection unchecked
        return transformListTyped( collection );
    }

    List<T> transformListTyped( List<T> collection );
}
