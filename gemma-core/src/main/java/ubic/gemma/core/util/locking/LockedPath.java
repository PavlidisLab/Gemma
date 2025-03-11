package ubic.gemma.core.util.locking;

import javax.annotation.WillClose;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A locked path.
 * @author poirigui
 */
public interface LockedPath extends AutoCloseable {

    /**
     * Retrieve the path being locked.
     */
    Path getPath();

    /**
     * Indicate if the lock is valid.
     */
    boolean isValid();

    /**
     * Indicate if the lock is shared.
     */
    boolean isShared();

    /**
     * Release the lock.
     * <p>
     * This does nothing if the lock is already closed or stolen.
     */
    @Override
    void close();

    /**
     * Release the lock and obtain the underlying {@link Path} object.
     * <p>
     * This does nothing if the lock is already closed or stolen.
     */
    @WillClose
    Path closeAndGetPath();

    /**
     * Convert this lock to an exclusive lock.
     * <p>
     * This lock will be closed as a result.
     * @throws IllegalStateException if this lock is already exclusive or no longer valid
     */
    @WillClose
    LockedPath toExclusive();

    /**
     * Try to convert this lock to an exclusive lock.
     * <p>
     * This lock will be closed as a result.
     * @throws IllegalStateException if this lock is already exclusive or no longer valid
     */
    @WillClose
    LockedPath toExclusive( long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException;

    /**
     * Convert this lock to a shared lock.
     * <p>
     * This lock will be closed as a result.
     * @throws IllegalStateException if this lock is already shared or no longer valid
     */
    @WillClose
    LockedPath toShared();

    /**
     * Steal this lock.
     * <p>
     * Once stolen, this lock will no-longer be released when closed.
     * @throws IllegalStateException if this lock is already stolen or no longer valid
     */
    @WillClose
    LockedPath steal();

    /**
     * Steal this lock with a different path. The lock will still be held on the original path, but will indicate a
     * different path.
     * @throws IllegalStateException if this lock is already stolen or no longer valid
     */
    @WillClose
    LockedPath stealWithPath( Path path );
}
