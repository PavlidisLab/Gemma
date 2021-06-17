package ubic.gemma.persistence.persister;

/**
 * Special type of {@link Persister} with caching capabilities.
 * @param <T>
 */
public interface CachingPersister<T> extends Persister<T> {

    void clearCache();
}
