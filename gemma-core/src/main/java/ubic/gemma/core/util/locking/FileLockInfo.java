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
    List<ProcessInfo> procInfo;

    /**
     * Process-level information about the file lock.
     */
    @Value
    public static class ProcessInfo {
        /**
         * Unique identifier for the lock.
         */
        String id;
        /**
         * Indicate if this lock is mandatory (i.e. being enforced by the OS).
         */
        boolean mandatory;
        /**
         * Indicate if this lock is exclusive.
         */
        boolean exclusive;
        /**
         * PID of the process holding the lock.
         */
        int pid;
        /**
         * Indicate if this process info belongs to the current process.
         */
        boolean self;
        String majorDevice;
        String minorDevice;
        long inode;
        long start;
        long length;
    }
}
