package ubic.gemma.persistence.util;

import net.sf.ehcache.Ehcache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by tesarst on 04/04/17.
 * Provides common methods for cache manipulation.
 */
public class CacheUtils {

    /**
     * Obtain a cache by its name, raising an exception if unavailable.
     * @throws RuntimeException if the cache identified by name is missing
     */
    public static Cache getCache( CacheManager cacheManager, String cacheName ) throws RuntimeException {
        return Objects.requireNonNull( cacheManager.getCache( cacheName ), String.format( "Cache with name %s does not exist.", cacheName ) );
    }

    /**
     * Evict entries from the cache where the key is matching a given predicate.
     * <p>
     * If keys cannot be enumerated by the cache provider, all entries are cleared.
     */
    public static void evictIf( Cache cache, Predicate<Object> predicate ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            for ( Object key : ( ( Ehcache ) cache.getNativeCache() ).getKeys() ) {
                if ( predicate.test( key ) ) {
                    cache.evict( key );
                }
            }
        } else {
            cache.clear();
        }
    }

    /**
     * Compute a cache entry only if it is missing, otherwise return the existing value.
     * <p>
     * If supported, a write lock is used such that the function is invoked only once for a given key.
     */
    public static <T> T computeIfMissing( Cache cache, Object key, Supplier<T> supplier ) {
        try {
            if ( cache.getNativeCache() instanceof Ehcache ) {
                ( ( Ehcache ) cache.getNativeCache() ).acquireWriteLockOnKey( key );
            }
            Cache.ValueWrapper value = cache.get( key );
            if ( value != null ) {
                //noinspection unchecked
                return ( T ) value.get();
            } else {
                T newVal = supplier.get();
                cache.put( key, newVal );
                return newVal;
            }
        } finally {
            if ( cache.getNativeCache() instanceof Ehcache ) {
                ( ( Ehcache ) cache.getNativeCache() ).releaseWriteLockOnKey( key );
            }
        }
    }
}
