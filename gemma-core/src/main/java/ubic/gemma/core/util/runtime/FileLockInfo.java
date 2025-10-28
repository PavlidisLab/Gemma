package ubic.gemma.core.util.runtime;

import lombok.Value;

/**
 * Metadata about a file lock.
 * @author poirigui
 */
@Value
public class FileLockInfo {
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
