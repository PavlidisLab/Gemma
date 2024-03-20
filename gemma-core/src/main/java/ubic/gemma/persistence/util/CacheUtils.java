package ubic.gemma.persistence.util;

import net.sf.ehcache.Ehcache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    public static Lock acquireReadLock( Cache cache, Object key ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return new EhcacheLock( ( Ehcache ) cache.getNativeCache(), key, true );
        } else {
            return new CacheLock( cache, key, true );
        }
    }

    /**
     * Acquire an exclusive write lock on the given key in the cache.
     * <p>
     * This can be used for preventing other threads from performing the same expensive operations.
     */
    public static Lock acquireWriteLock( Cache cache, Object key ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            return new EhcacheLock( ( Ehcache ) cache.getNativeCache(), key, false );
        } else {
            return new CacheLock( cache, key, false );
        }
    }

    public interface Lock extends AutoCloseable {

        @Override
        void close();
    }

    private static class EhcacheLock implements Lock {

        private final Ehcache cache;
        private final Object key;
        private final boolean readOnly;

        public EhcacheLock( Ehcache cache, Object key, boolean readOnly ) {
            this.cache = cache;
            this.key = key;
            this.readOnly = readOnly;
            lock();
        }

        @Override
        public void close() {
            unlock();
        }

        private void lock() {
            if ( readOnly ) {
                cache.acquireReadLockOnKey( key );
            } else {
                cache.acquireWriteLockOnKey( key );
            }
        }

        private void unlock() {
            if ( readOnly ) {
                cache.releaseReadLockOnKey( key );
            } else {
                cache.releaseWriteLockOnKey( key );
            }
        }
    }

    private static class CacheLock implements Lock {

        /**
         * Using a WeakHashMap to avoid memory leaks when a cache key is no longer used.
         */
        private static final Map<Cache, Map<Object, ReadWriteLock>> lockByKey = new WeakHashMap<>();

        private final ReadWriteLock lock;
        private final boolean readOnly;

        public CacheLock( Cache cache, Object key, boolean readOnly ) {
            synchronized ( lockByKey ) {
                this.lock = lockByKey.computeIfAbsent( cache, k -> new WeakHashMap<>() )
                        .computeIfAbsent( key, k -> new ReentrantReadWriteLock() );
            }
            this.readOnly = readOnly;
            lock();
        }

        @Override
        public void close() {
            unlock();
        }

        private void lock() {
            if ( readOnly ) {
                lock.readLock().lock();
            } else {
                lock.writeLock().lock();
            }
        }

        private void unlock() {
            if ( readOnly ) {
                lock.readLock().unlock();
            } else {
                lock.writeLock().unlock();
            }
        }
    }
}
