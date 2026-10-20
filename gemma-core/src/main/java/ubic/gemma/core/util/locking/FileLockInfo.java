package ubic.gemma.core.util.locking;

import lombok.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Metadata about a file lock.
 * @author poirigui
 */
@Value
public class FileLockInfo {
    /**
     * Path subject to locking.
     */
    Path path;
    /**
     * Path being locked, which is usually {@link #getPath()} with a {@code .lock} suffix.
     */
    Path lockfilePath;
    /**
     * @see ReentrantReadWriteLock#getReadHoldCount()
     */
    int readHoldCount;
    /**
     * @see ReentrantReadWriteLock#getReadLockCount()
     */
    int readLockCount;
    /**
     * @see ReentrantReadWriteLock#getWriteHoldCount()
     */
    int writeHoldCount;
    /**
     * @see ReentrantReadWriteLock#isWriteLocked()
     */
    boolean isWriteLocked;
    /**
     * Number of reentrant holds on the {@link java.nio.channels.FileChannel}.
     * <p>
     * When that number goes down to zero, the channel is closed.
     */
    int channelHoldCount;
    /**
     * Process-level information about the file lock.
     */
    List<ubic.gemma.core.util.runtime.FileLockInfo> procInfo;
}
