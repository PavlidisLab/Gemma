package ubic.gemma.persistence.util;

import org.hibernate.transform.ResultTransformer;

import java.util.List;

/**
 * Overrides Hibernate {@link ResultTransformer} interface to include type safety.
 * @param <T> the type this transformer produces
 */
public interface TypedResultTransformer<T> extends ResultTransformer {

    @Override
    T transformTuple( Object[] tuple, String[] aliases );

    @Override
    List<T> transformList( List collection );
}
