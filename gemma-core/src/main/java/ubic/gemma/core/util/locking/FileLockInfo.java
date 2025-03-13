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
    List<ProcessInfo> procInfo;

    /**
     * Process-level information about the file lock.
     */
    @Value
    public static class ProcessInfo {
        String id;
        boolean mandatory;
        boolean exclusive;
        int pid;
        String majorDevice;
        String minorDevice;
        long inode;
        long start;
        long length;
    }
}
