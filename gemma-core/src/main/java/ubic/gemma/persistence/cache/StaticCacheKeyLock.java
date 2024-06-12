package ubic.gemma.persistence.cache;

import lombok.EqualsAndHashCode;
import org.springframework.cache.Cache;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the {@link CacheKeyLock} interface that uses a static week map to store locks by key.
 * <p>
 * Locks are guaranteed to be kept around as long as there is a {@link ubic.gemma.persistence.cache.CacheKeyLock.LockAcquisition}
 * referring to it, otherwise they might be freed by the GC.
 * @author poirigui
 * @see WeakHashMap
 */
@EqualsAndHashCode(of = { "lock", "readOnly" })
public class StaticCacheKeyLock implements CacheKeyLock {

    /**
     * Using a WeakHashMap to avoid memory leaks when a cache key is no longer used.
     * <p>
     * Entries in this mapping are kept alive by the existence of {@link StaticLockAcquisition}.
     */
    private static final Map<Cache, Map<Object, ReadWriteLock>> lockByKey = new WeakHashMap<>();

    private final ReadWriteLock lock;
    private final boolean readOnly;

    private final LockAcquisition acquisition;

    public StaticCacheKeyLock( Cache cache, Object key, boolean readOnly ) {
        synchronized ( lockByKey ) {
            this.lock = lockByKey.computeIfAbsent( cache, k -> new WeakHashMap<>() )
                    .computeIfAbsent( key, k -> new ReentrantReadWriteLock() );
        }
        this.readOnly = readOnly;
        this.acquisition = new StaticLockAcquisition( cache, key );
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public LockAcquisition lock() {
        if ( readOnly ) {
            lock.readLock().lock();
        } else {
            lock.writeLock().lock();
        }
        return acquisition;
    }

    @Override
    public LockAcquisition lockInterruptibly() throws InterruptedException {
        if ( readOnly ) {
            lock.readLock().lockInterruptibly();
        } else {
            lock.writeLock().lockInterruptibly();
        }
        return acquisition;
    }

    private class StaticLockAcquisition implements LockAcquisition {

        // we need a strong references on both the cache and the key to ensure that it remains in the lockByKey weak
        // mapping as long as there is an acquisition
        @SuppressWarnings({ "unused", "FieldCanBeLocal" })
        private final Cache cache;
        private final Object key;

        public StaticLockAcquisition( Cache cache, Object key ) {
            this.cache = cache;
            this.key = key;
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public void unlock() {
            if ( readOnly ) {
                lock.readLock().unlock();
            } else {
                lock.writeLock().unlock();
            }
        }
    }
}
