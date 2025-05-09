package ubic.gemma.core.util.locking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLockInterruptionException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A read/write lock to synchronize access to a file.
 * <p>
 * The implementation uses {@link ReentrantReadWriteLock} to guard a {@link java.nio.channels.FileLock}, providing
 * locking capabilities at the OS-level.
 * <p>
 * Only one {@link ReadWriteFileLock} may be used for a given file at a time throughout the JVM.
 * @author poirigui
 */
class ReadWriteFileLock implements ReadWriteLock {

    /**
     * Open a read/write file lock.
     * @param path          a path to lock
     * @param deleteOnClose if true, the file will be deleted when the lock is released and no other threads or process
     *                      is holding it
     */
    public static ReadWriteFileLock open( Path path, boolean deleteOnClose ) {
        return new ReadWriteFileLock( new ReentrantReadWriteLock(), path, deleteOnClose );
    }

    private final Path path;
    private final FileLock readLock;
    private final FileLock writeLock;

    @Nullable
    private volatile FileChannel channel;
    /**
     * Number of holders for the channel.
     * <p>
     * When it drops to zero, the channel is closed.
     */
    private final AtomicInteger channelHolders = new AtomicInteger( 0 );

    private ReadWriteFileLock( ReentrantReadWriteLock rwLock, Path path, boolean deleteOnClose ) {
        this.path = path;
        this.readLock = new FileLock( rwLock.readLock(), true, deleteOnClose );
        this.writeLock = new FileLock( rwLock.writeLock(), false, deleteOnClose );
    }

    public Path getPath() {
        return path;
    }

    private FileChannel getChannel() throws IOException {
        FileChannel fc = channel;
        if ( fc == null ) {
            fc = FileChannel.open( path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE );
            channel = fc;
        }
        return fc;
    }

    @Nonnull
    @Override
    public Lock readLock() {
        return readLock;
    }

    @Nonnull
    @Override
    public Lock writeLock() {
        return writeLock;
    }

    public int getReadHoldCount() {
        return readLock.holders.get();
    }

    public int getReadLockCount() {
        return readLock.fileLockHolders.get();
    }

    public int getWriteHoldCount() {
        return writeLock.fileLockHolders.get();
    }

    public boolean isWriteLocked() {
        return writeLock.fileLockHolders.get() > 0;
    }

    public int getChannelHoldCount() {
        return channelHolders.get();
    }

    private class FileLock implements Lock {

        private final Lock lock;
        private final boolean shared;
        private final boolean deleteOnClose;
        /**
         * Total holders for this lock for this thread.
         */
        private final ThreadLocal<Integer> holders = ThreadLocal.withInitial( () -> 0 );

        @Nullable
        private volatile java.nio.channels.FileLock fileLock;
        /**
         * Total holders for the file lock.
         * <p>
         * When it drops to zero, the lock is released.
         */
        private final AtomicInteger fileLockHolders = new AtomicInteger( 0 );

        private FileLock( Lock lock, boolean shared, boolean deleteOnClose ) {
            this.lock = lock;
            this.shared = shared;
            this.deleteOnClose = deleteOnClose;
        }

        @Override
        public void lock() {
            lock.lock();
            if ( fileLock != null ) {
                incrementHolders();
                return;
            }
            try {
                fileLock = getChannel().lock( 0, Long.MAX_VALUE, shared );
                incrementHolders();
            } catch ( IOException e ) {
                lock.unlock();
                throw new RuntimeException( "Failed to acquire " + ( shared ? "shared" : "exclusive" ) + " lock on " + path + ".", e );
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
            if ( fileLock != null ) {
                incrementHolders();
                return;
            }
            try {
                fileLock = getChannel().lock( 0, Long.MAX_VALUE, shared );
                incrementHolders();
            } catch ( FileLockInterruptionException e ) {
                lock.unlock();
                throw new InterruptedException( e.getMessage() );
            } catch ( IOException e ) {
                lock.unlock();
                throw new RuntimeException( "Failed to acquire " + ( shared ? "shared" : "exclusive" ) + " lock on " + path + ".", e );
            }
        }

        @Override
        public boolean tryLock() {
            if ( !lock.tryLock() ) {
                return false;
            }

            if ( fileLock != null ) {
                incrementHolders();
                return true;
            }

            try {
                java.nio.channels.FileLock fl = getChannel().tryLock( 0, Long.MAX_VALUE, shared );
                if ( fl == null ) {
                    lock.unlock();
                    return false;
                }
                fileLock = fl;
                incrementHolders();
                return true;
            } catch ( IOException e ) {
                lock.unlock();
                throw new RuntimeException( "Failed to acquire " + ( shared ? "shared" : "exclusive" ) + " lock on " + path + ".", e );
            }
        }

        @Override
        public boolean tryLock( long l, TimeUnit timeUnit ) throws InterruptedException {
            long startNs = System.nanoTime();
            if ( !lock.tryLock( l, timeUnit ) ) {
                return false;
            }

            if ( fileLock != null ) {
                incrementHolders();
                return true;
            }

            // use the remaining time to acquire a file lock
            while ( ( System.nanoTime() - startNs ) < timeUnit.toNanos( l ) ) {
                if ( Thread.interrupted() ) {
                    lock.unlock();
                    throw new InterruptedException( "Current thread was interrupted while waiting for a " + ( shared ? "shared" : "exclusive" ) + " lock on " + path + "." );
                }
                try {
                    java.nio.channels.FileLock fl = getChannel().tryLock( 0, Long.MAX_VALUE, shared );
                    if ( fl != null ) {
                        fileLock = fl;
                        incrementHolders();
                        return true;
                    }
                } catch ( IOException e ) {
                    lock.unlock();
                    throw new RuntimeException( "Failed to acquire " + ( shared ? "shared" : "exclusive" ) + " lock on " + path + ".", e );
                }
            }

            // timed out
            lock.unlock();
            return false;
        }

        @Override
        public void unlock() {
            try {
                decrementHolders();
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to release lock on " + path + ".", e );
            } finally {
                lock.unlock();
            }
        }

        @Nonnull
        @Override
        public Condition newCondition() {
            return lock.newCondition();
        }

        private void incrementHolders() {
            FileChannel c = channel;
            if ( c == null || !c.isOpen() ) {
                throw new IllegalStateException( "Channel is not open." );
            }
            channelHolders.incrementAndGet();
            java.nio.channels.FileLock fl = fileLock;
            if ( fl == null || !fl.isValid() ) {
                throw new IllegalStateException( "There is no valid file lock to be held." );
            }
            fileLockHolders.incrementAndGet();
            holders.set( holders.get() + 1 );
        }

        /**
         * @throws IllegalMonitorStateException if the number of holders goes below zero
         */
        private void decrementHolders() throws IllegalMonitorStateException, IOException {
            int th = holders.get() - 1;
            if ( th < 0 ) {
                holders.remove();
                throw new IllegalMonitorStateException();
            } else if ( th == 0 ) {
                holders.remove();
            } else {
                holders.set( th );
            }
            java.nio.channels.FileLock fl = fileLock;
            if ( fileLockHolders.decrementAndGet() == 0 ) {
                if ( fl == null ) {
                    throw new IllegalStateException();
                }
                fileLock = null;
                fl.release();
            }
            FileChannel c = channel;
            if ( channelHolders.decrementAndGet() == 0 ) {
                if ( c == null ) {
                    throw new IllegalStateException();
                }
                channel = null;
                if ( deleteOnClose ) {
                    boolean canDelete;
                    try ( java.nio.channels.FileLock l = c.tryLock( 0, Long.MAX_VALUE, false ) ) {
                        canDelete = l != null;
                    } finally {
                        c.close();
                    }
                    if ( canDelete ) {
                        Files.delete( path );
                    }
                } else {
                    c.close();
                }
            }
        }
    }
}
