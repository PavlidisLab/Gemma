package ubic.gemma.persistence.cache;

import org.springframework.cache.Cache;

/**
 * Represents a lock over a cache key.
 * @see Cache
 */
public interface CacheKeyLock {

    /**
     * Represents an acquired lock on a cache key.
     */
    interface LockAcquisition extends AutoCloseable {

        /**
         * Obtain the key that is locked.
         */
        Object getKey();

        /**
         * Indicate if this lock acquisition is read-only.
         */
        boolean isReadOnly();

        /**
         * Release the lock on the cache key previously acquired with {@link #lock()} or {@link #lockInterruptibly()}.
         */
        void unlock();

        @Override
        default void close() {
            unlock();
        }
    }

    /**
     * Indicate if this lock is read-only.
     */
    boolean isReadOnly();

    /**
     * Acquire a lock on the cache key.
     */
    LockAcquisition lock();

    /**
     * Acquire a lock interruptibly on a cache key.
     * @throws InterruptedException if the current thread was interrupted prior to or while waiting on the lock
     */
    LockAcquisition lockInterruptibly() throws InterruptedException;
}
