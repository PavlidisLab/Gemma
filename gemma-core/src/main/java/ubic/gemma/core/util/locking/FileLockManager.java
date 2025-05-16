package ubic.gemma.core.util.locking;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * Create shared and exclusive locks for files.
 * <p>
 * All file locking within Gemma should be mediated by this class.
 *
 * @author poirigui
 */
public interface FileLockManager {

    /**
     * Get the lock info for all paths known to the lock manager.
     */
    Collection<FileLockInfo> getAllLockInfos() throws IOException;

    /**
     * Get the lock info for all paths obtained by walking the given directory.
     */
    Stream<FileLockInfo> getAllLockInfosByWalking( Path directory, int maxDepth ) throws IOException;

    /**
     * Get the lock info for a given path.
     */
    FileLockInfo getLockInfo( Path path ) throws IOException;

    /**
     * Lock a given path.
     *
     * @param path      the path to lock
     * @param exclusive make the lock exclusive for the purpose of creating of modifying the path
     */
    LockedPath acquirePathLock( Path path, boolean exclusive ) throws IOException;

    /**
     * Attempt to lock a path.
     *
     * @param path      the path to lock
     * @param exclusive make the lock exclusive for the purpose of creating of modifying the path
     * @throws TimeoutException     if the lock acquisition timed out
     * @throws InterruptedException if the thread was interrupted while waiting for the lock
     */
    LockedPath tryAcquirePathLock( Path path, boolean exclusive, long timeout, TimeUnit timeUnit ) throws IOException, TimeoutException, InterruptedException;
}
