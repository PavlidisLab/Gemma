package ubic.gemma.persistence.util;

import lombok.Value;
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
     * Acquire an exclusive write lock on the given key in the cache.
     * <p>
     * This can be used for preventing other threads from performing the same expensive operations.
     */
    public static Lock acquireWriteLock( Cache cache, Object key ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return new EhcacheWriteLock( ( Ehcache ) cache.getNativeCache(), key );
        } else {
            return new NoopWriteLock();
        }
    }

    public interface Lock extends AutoCloseable {

        @Override
        void close();
    }

    @Value
    private static class EhcacheWriteLock implements Lock {

        Ehcache cache;
        Object key;

        public EhcacheWriteLock( Ehcache cache, Object key ) {
            this.cache = cache;
            this.key = key;
            lock();
        }

        @Override
        public void close() {
            unlock();
        }

        private void lock() {
            cache.acquireWriteLockOnKey( key );
        }

        private void unlock() {
            cache.releaseWriteLockOnKey( key );
        }
    }

    private static class NoopWriteLock implements Lock {

        @Override
        public void close() {
            // noop
        }
    }
}
