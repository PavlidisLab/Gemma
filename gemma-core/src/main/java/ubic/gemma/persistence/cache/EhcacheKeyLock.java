package ubic.gemma.persistence.cache;

import lombok.EqualsAndHashCode;
import net.sf.ehcache.Ehcache;
import org.springframework.util.Assert;

/**
 * @author poirigui
 */
@EqualsAndHashCode(of = { "cache", "key", "readOnly" })
public class EhcacheKeyLock implements CacheKeyLock {

    private static final long DEFAULT_LOCK_CHECK_INTERVAL_MS = 30000L;

    private final Ehcache cache;
    private final Object key;
    private final boolean readOnly;

    private final LockAcquisition acquisition = new EhcacheLockAcquisition();

    private long lockCheckIntervalMillis = DEFAULT_LOCK_CHECK_INTERVAL_MS;

    public EhcacheKeyLock( Ehcache cache, Object key, boolean readOnly ) {
        this.cache = cache;
        this.key = key;
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * Set the interval to check for lock acquisition in milliseconds when {@link #lockInterruptibly()} is used.
     * <p>
     * This defaults to 30000 ms.
     */
    public void setLockCheckIntervalMillis( long lockCheckIntervalMillis ) {
        Assert.isTrue( lockCheckIntervalMillis >= 0 );
        this.lockCheckIntervalMillis = lockCheckIntervalMillis;
    }

    @Override
    public LockAcquisition lock() {
        if ( readOnly ) {
            cache.acquireReadLockOnKey( key );
        } else {
            cache.acquireWriteLockOnKey( key );
        }
        return acquisition;
    }

    @Override
    public LockAcquisition lockInterruptibly() throws InterruptedException {
        while ( true ) {
            if ( readOnly ) {
                if ( cache.tryReadLockOnKey( key, lockCheckIntervalMillis ) ) {
                    return acquisition;
                }
            } else {
                if ( cache.tryWriteLockOnKey( key, lockCheckIntervalMillis ) ) {
                    return acquisition;
                }
            }
        }
    }

    private class EhcacheLockAcquisition implements LockAcquisition {

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
                cache.releaseReadLockOnKey( key );
            } else {
                cache.releaseWriteLockOnKey( key );
            }
        }
    }
}
