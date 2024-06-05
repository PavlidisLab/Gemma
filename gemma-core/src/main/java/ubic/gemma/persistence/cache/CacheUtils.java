package ubic.gemma.persistence.cache;

import net.sf.ehcache.Ehcache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

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

    public static int getSize( Cache cache ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return ( ( Ehcache ) cache.getNativeCache() ).getSize();
        } else if ( cache.getNativeCache() instanceof Map ) {
            return ( ( Map<?, ?> ) cache.getNativeCache() ).size();
        } else {
            return 0;
        }
    }

    /**
     * Check if a cache contains a given key.
     */
    public static boolean containsKey( Cache cache, Object key ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return ( ( Ehcache ) cache.getNativeCache() ).isKeyInCache( key );
        } else if ( cache.getNativeCache() instanceof Map ) {
            return ( ( Map<?, ?> ) cache.getNativeCache() ).containsKey( key );
        } else {
            return cache.get( key ) != null;
        }
    }

    /**
     * Obtain the keys of all elements of a cache.
     */
    public static Collection<?> getKeys( Cache cache ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return ( ( Ehcache ) cache.getNativeCache() ).getKeys();
        } else if ( cache.getNativeCache() instanceof Map ) {
            return ( ( Map<?, ?> ) cache.getNativeCache() ).keySet();
        } else {
            return Collections.emptySet();
        }
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
     * Acquire a read-only lock on the given key in the cache.
     * @return an acquired read lock on the given cache key
     * @throws InterruptedException if the current thread is interrupted while waiting for the lock
     */
    public static CacheKeyLock.LockAcquisition acquireReadLock( Cache cache, Object key ) throws InterruptedException {
        return createLock( cache, key, true ).lockInterruptibly();
    }

    /**
     * Acquire an exclusive write lock on the given key in the cache.
     * @return an acquired write lock on the given cache key
     * @throws InterruptedException if the current thread is interrupted while waiting for the lock
     */
    public static CacheKeyLock.LockAcquisition acquireWriteLock( Cache cache, Object key ) throws InterruptedException {
        return createLock( cache, key, false ).lockInterruptibly();
    }

    private static CacheKeyLock createLock( Cache cache, Object key, boolean readOnly ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return new EhcacheKeyLock( ( Ehcache ) cache.getNativeCache(), key, readOnly );
        } else {
            return new StaticCacheKeyLock( cache, key, readOnly );
        }
    }
}
