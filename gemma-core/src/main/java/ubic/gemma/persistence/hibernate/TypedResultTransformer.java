package ubic.gemma.persistence.hibernate;

import org.hibernate.transform.ResultTransformer;

import java.util.List;

/**
 * Overrides Hibernate {@link ResultTransformer} interface to include type safety.
 * @param <T> the type this transformer produces
 */
public interface TypedResultTransformer<T> extends ResultTransformer {

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
