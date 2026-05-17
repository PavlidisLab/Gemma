package ubic.gemma.persistence.hibernate;

import java.util.List;

/**
 * Lightweight type-safe row-and-list transformer.
 * <p>
 * Pre-phase-2 this interface extended {@link org.hibernate.transform.ResultTransformer} so it could be passed
 * directly to {@code Query.setResultTransformer(...)}. Hibernate 6 removed {@code setResultTransformer} and split
 * the abstraction into {@code TupleTransformer<T>} + {@code ResultListTransformer<T>}. For now Gemma applies these
 * transformers in user code post-query (see {@link #applyTo(List)}); a richer migration to the new
 * {@code TupleTransformer}/{@code ResultListTransformer} interfaces can follow.
 *
 * @param <T> the type this transformer produces
 */
public interface TypedResultTransformer<T> {

    T transformTuple( Object[] tuple, String[] aliases );

    List<T> transformListTyped( List<T> collection );

    /**
     * Apply this transformer to a query result list. Each row is wrapped in a one-element tuple before being passed
     * to {@link #transformTuple(Object[], String[])}.
     */
    default List<T> applyTo( List<?> rows ) {
        java.util.List<T> transformed = new java.util.ArrayList<>( rows.size() );
        for ( Object row : rows ) {
            Object[] tuple = row instanceof Object[] ? ( Object[] ) row : new Object[] { row };
            T t = transformTuple( tuple, null );
            if ( t != null ) {
                transformed.add( t );
            }
        }
        return transformListTyped( transformed );
    }

    /** Execute the query, then apply this transformer to its list result. */
    default List<T> list( org.hibernate.query.Query<?> query ) {
        return applyTo( query.list() );
    }

    /** Execute the query as uniqueResult, then transform that single row. */
    default T uniqueResult( org.hibernate.query.Query<?> query ) {
        Object r = query.uniqueResult();
        if ( r == null ) {
            return null;
        }
        Object[] tuple = r instanceof Object[] ? ( Object[] ) r : new Object[] { r };
        T t = transformTuple( tuple, null );
        if ( t == null ) {
            return null;
        }
        List<T> transformed = transformListTyped( java.util.Collections.singletonList( t ) );
        return transformed.isEmpty() ? null : transformed.iterator().next();
    }
}
