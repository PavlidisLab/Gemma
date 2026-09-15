package ubic.gemma.persistence.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Common helpers for Spring {@link Cache} manipulation.
 * <p>
 * Pre-phase-2 this class branched per native cache type (EhCache 2 vs. Map vs. fallback).
 * Phase 2 retired EhCache 2 and reduced to "Map or generic Spring Cache fallback".
 * Phase 3 reintroduced a real {@link javax.cache.CacheManager}-backed Spring {@link CacheManager}
 * ({@link EhcacheConfig}), so the native cache is now a {@link javax.cache.Cache} (JSR-107)
 * for every application cache. The helpers below cover both backings:
 * {@link Map} (for tests / ad-hoc {@code ConcurrentMapCache}) and
 * {@link javax.cache.Cache} (production).
 */
public class CacheUtils {

    public static Cache getCache( CacheManager cacheManager, String cacheName ) throws RuntimeException {
        return Objects.requireNonNull( cacheManager.getCache( cacheName ), String.format( "Cache with name %s does not exist.", cacheName ) );
    }

    public static int getSize( Cache cache ) {
        Object native_ = cache.getNativeCache();
        if ( native_ instanceof Map ) {
            return ( ( Map<?, ?> ) native_ ).size();
        }
        if ( native_ instanceof javax.cache.Cache ) {
            int n = 0;
            for ( javax.cache.Cache.Entry<?, ?> ignored : ( javax.cache.Cache<?, ?> ) native_ ) {
                n++;
            }
            return n;
        }
        return 0;
    }

    public static boolean containsKey( Cache cache, Object key ) {
        Object native_ = cache.getNativeCache();
        if ( native_ instanceof Map ) {
            return ( ( Map<?, ?> ) native_ ).containsKey( key );
        }
        if ( native_ instanceof javax.cache.Cache ) {
            @SuppressWarnings("unchecked")
            javax.cache.Cache<Object, Object> jc = ( javax.cache.Cache<Object, Object> ) native_;
            return jc.containsKey( key );
        }
        return cache.get( key ) != null;
    }

    public static Collection<?> getKeys( Cache cache ) {
        Object native_ = cache.getNativeCache();
        if ( native_ instanceof Map ) {
            return ( ( Map<?, ?> ) native_ ).keySet();
        }
        if ( native_ instanceof javax.cache.Cache ) {
            Set<Object> keys = new LinkedHashSet<>();
            for ( javax.cache.Cache.Entry<?, ?> e : ( javax.cache.Cache<?, ?> ) native_ ) {
                keys.add( e.getKey() );
            }
            return keys;
        }
        return Collections.emptySet();
    }

    /**
     * Evict entries from the cache where the key matches a given predicate.
     * <p>
     * If the cache cannot enumerate keys, all entries are cleared.
     */
    public static void evictIf( Cache cache, Predicate<Object> predicate ) {
        Object native_ = cache.getNativeCache();
        if ( native_ instanceof Map ) {
            for ( Object key : new ArrayList<>( ( ( Map<?, ?> ) native_ ).keySet() ) ) {
                if ( predicate.test( key ) ) {
                    cache.evict( key );
                }
            }
        } else if ( native_ instanceof javax.cache.Cache ) {
            // Iterate once into a snapshot; avoid concurrent-modification surprises
            // since we evict via the Spring facade while iterating the JCache directly.
            Collection<Object> keys = new ArrayList<>();
            for ( javax.cache.Cache.Entry<?, ?> e : ( javax.cache.Cache<?, ?> ) native_ ) {
                keys.add( e.getKey() );
            }
            for ( Object key : keys ) {
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
