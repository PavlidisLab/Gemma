package ubic.gemma.persistence.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Common helpers for Spring {@link Cache} manipulation.
 * <p>
 * Pre-phase-2 this class also branched per native cache type (EhCache 2 vs.
 * Map vs. fallback). With EhCache 2 retired during the Spring 6 / Hibernate 6
 * migration, only the Map branch and the generic Spring Cache fallback
 * remain.
 */
public class CacheUtils {

    public static Cache getCache( CacheManager cacheManager, String cacheName ) throws RuntimeException {
        return Objects.requireNonNull( cacheManager.getCache( cacheName ), String.format( "Cache with name %s does not exist.", cacheName ) );
    }

    public static int getSize( Cache cache ) {
        if ( cache.getNativeCache() instanceof Map ) {
            return ( ( Map<?, ?> ) cache.getNativeCache() ).size();
        }
        return 0;
    }

    public static boolean containsKey( Cache cache, Object key ) {
        if ( cache.getNativeCache() instanceof Map ) {
            return ( ( Map<?, ?> ) cache.getNativeCache() ).containsKey( key );
        }
        return cache.get( key ) != null;
    }

    public static Collection<?> getKeys( Cache cache ) {
        if ( cache.getNativeCache() instanceof Map ) {
            return ( ( Map<?, ?> ) cache.getNativeCache() ).keySet();
        }
        return Collections.emptySet();
    }

    /**
     * Evict entries from the cache where the key matches a given predicate.
     * <p>
     * If the cache cannot enumerate keys, all entries are cleared.
     */
    public static void evictIf( Cache cache, Predicate<Object> predicate ) {
        if ( cache.getNativeCache() instanceof Map ) {
            for ( Object key : ( ( Map<?, ?> ) cache.getNativeCache() ).keySet() ) {
                if ( predicate.test( key ) ) {
                    cache.evict( key );
                }
            }
        } else {
            cache.clear();
        }
    }

    public static CacheKeyLock.LockAcquisition acquireReadLock( Cache cache, Object key ) throws InterruptedException {
        return new StaticCacheKeyLock( cache, key, true ).lockInterruptibly();
    }

    public static CacheKeyLock.LockAcquisition acquireWriteLock( Cache cache, Object key ) throws InterruptedException {
        return new StaticCacheKeyLock( cache, key, false ).lockInterruptibly();
    }
}
