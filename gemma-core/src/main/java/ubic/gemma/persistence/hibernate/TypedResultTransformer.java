package ubic.gemma.persistence.hibernate;

import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

import java.util.List;

/**
 * Lightweight type-safe row-and-list transformer.
 * <p>
 * Pre-phase-2 this interface extended Hibernate's legacy {@code ResultTransformer} so it could be passed directly to
 * {@code Query.setResultTransformer(...)}. Hibernate 6 split the abstraction into
 * {@link TupleTransformer}{@code <T>} + {@link ResultListTransformer}{@code <T>} and added matching setters to
 * {@link org.hibernate.query.Query}. This interface now extends both, so {@link #list(org.hibernate.query.Query)} and
 * {@link #uniqueResult(org.hibernate.query.Query)} wire the transformer into the query directly. That means
 * {@code transformTuple} receives populated {@code aliases} (the column aliases declared in the HQL/SQL SELECT),
 * which is critical for {@code AliasToBean}-style initialisers that pick fields by alias.
 *
 * @param <T> the type this transformer produces
 */
public interface TypedResultTransformer<T> extends TupleTransformer<T>, ResultListTransformer<T> {

    @Override
    T transformTuple( Object[] tuple, String[] aliases );

    /**
     * Legacy name for the {@link ResultListTransformer#transformList} hook. Existing implementers override this;
     * the {@code transformList} default below delegates to it so the new Hibernate 6 transformer pipeline picks
     * up the same logic.
     */
    List<T> transformListTyped( List<T> collection );

    @Override
    default List<T> transformList( List<T> collection ) {
        return transformListTyped( collection );
    }

    /**
     * Apply this transformer to a list of rows that were fetched without the transformer wired into the query.
     * Each row is wrapped in a one-element tuple and {@code aliases} is passed as {@code null}. Used by callers
     * that have already executed the query (e.g. {@code query.list()}) and want to transform the rows after the
     * fact — most useful for transformers that don't rely on column aliases (entity-only unwrappers). Transformers
     * that DO need aliases should be wired via {@link #list(org.hibernate.query.Query)} or
     * {@link #uniqueResult(org.hibernate.query.Query)} instead.
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

    /** Execute the query with this transformer wired in, returning the typed list. */
    default List<T> list( org.hibernate.query.Query<?> query ) {
        //noinspection unchecked
        org.hibernate.query.Query<T> typed = query.setTupleTransformer( this ).setResultListTransformer( this );
        return typed.list();
    }

    /** Execute the query with this transformer wired in, returning the single typed row (or null). */
    default T uniqueResult( org.hibernate.query.Query<?> query ) {
        //noinspection unchecked
        org.hibernate.query.Query<T> typed = query.setTupleTransformer( this ).setResultListTransformer( this );
        return typed.uniqueResult();
    }
}
