package ubic.gemma.core.util.locking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import ubic.gemma.core.util.concurrent.ThreadUtils;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ubic.gemma.core.util.test.TestProcessUtils.startJavaProcess;

public class ReadWriteFileLockTest {

    /**
     * Relies on the {@code /proc/locks} pseudo-file, which only exists on Linux.
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    public void test() throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ), false );

        assertThat( tryLockInSubprocess( tempDir.resolve( "test.txt" ), true ) )
                .isTrue();

        lock.writeLock().lock();
        assertThatPosixLockExists( tempDir.resolve( "test.txt" ), false );
        assertThat( tryLockInSubprocess( tempDir.resolve( "test.txt" ), true ) )
                .isFalse();
        Files.write( lock.getPath(), Collections.singleton( "Hello world!" ), StandardCharsets.UTF_8 );
        lock.writeLock().unlock();
        assertThatPosixLockDoesNotExist( tempDir.resolve( "test.txt" ), true );

        lock.readLock().lock();
        assertThatPosixLockExists( tempDir.resolve( "test.txt" ), true );
        assertThat( tryLockInSubprocess( tempDir.resolve( "test.txt" ), true ) )
                .isTrue();
        assertThat( tryLockInSubprocess( tempDir.resolve( "test.txt" ), false ) )
                .isFalse();
        assertThat( Files.readAllLines( lock.getPath(), StandardCharsets.UTF_8 ) )
                .containsExactly( "Hello world!" );
        lock.readLock().unlock();
        assertThatPosixLockDoesNotExist( tempDir.resolve( "test.txt" ), true );
        assertThat( tryLockInSubprocess( tempDir.resolve( "test.txt" ), false ) )
                .isTrue();

        // make sure the write lock is re-usable
        lock.writeLock().lock();
        Files.write( lock.getPath(), Collections.singleton( "Hello world!" ), StandardCharsets.UTF_8 );
        lock.writeLock().unlock();

        // make sure the read lock is re-usable
        lock.readLock().lock();
        assertThat( Files.readAllLines( lock.getPath(), StandardCharsets.UTF_8 ) )
                .containsExactly( "Hello world!" );
        lock.readLock().unlock();
    }

    /**
     * Locks are not allowed to overlap within the same JVM.
     */
    @Test
    public void testOverlappingLocks() throws IOException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ), false );
        lock.readLock().lock();

        ReadWriteFileLock lock2 = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ), false );
        assertThatThrownBy( () -> lock2.readLock().lock() )
                .isInstanceOf( OverlappingFileLockException.class );

        lock.readLock().unlock();

        lock2.readLock().lock();
        lock2.readLock().unlock();
    }

    @Test
    public void testConcurrentReadWriteAccess() throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ), false );
        lock.writeLock().lock();
        Files.write( lock.getPath(), Collections.singleton( "Hello world!" ), StandardCharsets.UTF_8 );

        AtomicBoolean result = new AtomicBoolean();
        AtomicBoolean result2 = new AtomicBoolean();
        AtomicBoolean result3 = new AtomicBoolean();
        Thread t = ThreadUtils.newThread( () -> {
            result.set( lock.readLock().tryLock() );
            if ( result.get() ) {
                lock.readLock().unlock();
            }
            result2.set( lock.writeLock().tryLock() );
            if ( result2.get() ) {
                lock.writeLock().unlock();
            }
            result3.set( lock.writeLock().tryLock() );
            if ( result3.get() ) {
                lock.writeLock().unlock();
            }
        } );
        t.start();
        t.join();

        lock.writeLock().unlock();

        assertThat( result ).isFalse();
        assertThat( result2 ).isFalse();
        assertThat( result3 ).isFalse();

        lock.readLock().lock();

        Thread t2 = ThreadUtils.newThread( () -> {
            result.set( lock.readLock().tryLock() );
            if ( result.get() ) {
                lock.readLock().unlock();
            }
            result2.set( lock.writeLock().tryLock() );
            if ( result2.get() ) {
                lock.writeLock().unlock();
            }
            result3.set( lock.writeLock().tryLock() );
            if ( result3.get() ) {
                lock.writeLock().unlock();
            }
        } );
        t2.start();
        t2.join();
        lock.readLock().unlock();

        assertThat( result ).isTrue();
        assertThat( result2 ).isFalse();
        assertThat( result3 ).isFalse();
    }

    /**
     * Concurrent readers must not race the OS-level lock acquisition.
     * <p>
     * The {@link java.util.concurrent.locks.ReentrantReadWriteLock} admits any number of concurrent
     * readers, so the transitions of the underlying {@link java.nio.channels.FileLock} have to be
     * serialized separately. Before that serialization existed, two readers hitting an unheld lock
     * both called {@code channel.lock()} and the second threw {@link OverlappingFileLockException}
     * (seen in production on {@code GET /datasets/26/data/raw}, 2026-08-19); a reader acquiring while
     * the last holder released could also observe {@code fileLock == null} before the OS lock was
     * actually free, with the same result, or find a stale non-null lock and fail on
     * {@code isValid()}.
     * <p>
     * Every round re-crosses the zero-holder boundary, which is where all three interleavings live:
     * the barrier lines the readers up, each round's unlocks race the next round's locks, and any of
     * the historical failures surfaces as an exception out of {@code Future#get}.
     */
    @Test
    public void testConcurrentReadersDoNotRaceTheOsLock() throws Exception {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ), false );
        int nThreads = 8;
        ExecutorService pool = Executors.newFixedThreadPool( nThreads );
        try {
            for ( int round = 0; round < 200; round++ ) {
                CyclicBarrier barrier = new CyclicBarrier( nThreads );
                List<Future<?>> futures = new ArrayList<>( nThreads );
                for ( int i = 0; i < nThreads; i++ ) {
                    futures.add( pool.submit( () -> {
                        barrier.await();
                        lock.readLock().lock();
                        try {
                            assertThat( lock.getReadLockCount() ).isGreaterThan( 0 );
                        } finally {
                            lock.readLock().unlock();
                        }
                        return null;
                    } ) );
                }
                for ( Future<?> f : futures ) {
                    f.get( 30, java.util.concurrent.TimeUnit.SECONDS );
                }
            }
        } finally {
            pool.shutdownNow();
        }
        // everything was released, so an outside process can take the lock exclusively
        lock.readLock().lock();
        lock.readLock().unlock();
        assertThat( lock.getChannelHoldCount() ).isEqualTo( 0 );
    }

    @Test
    public void testReentrantLocking() throws IOException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ), false );
        lock.writeLock().lock();
        lock.writeLock().lock();
        lock.writeLock().unlock();
        lock.writeLock().unlock();
        assertThatThrownBy( () -> lock.writeLock().unlock() )
                .isInstanceOf( IllegalMonitorStateException.class );
    }

    @Test
    public void testReentrantLockingWithConcurrency() throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ), false );
        lock.readLock().lock();
        lock.readLock().lock();

        Thread t = ThreadUtils.newThread( () -> {
            lock.readLock().lock();
            lock.readLock().lock();
            lock.readLock().unlock();
            lock.readLock().unlock();
            assertThatThrownBy( () -> lock.readLock().unlock() )
                    .isInstanceOf( IllegalMonitorStateException.class );
        } );
        t.start();
        t.join();

        lock.readLock().unlock();
        lock.readLock().unlock();
        assertThatThrownBy( () -> lock.readLock().unlock() )
                .isInstanceOf( IllegalMonitorStateException.class );
    }

    /**
     * Try locking a given path in a subprocess.
     * @return true if the lock was acquired, false otherwise
     */
    private boolean tryLockInSubprocess( Path path, boolean shared ) throws IOException, InterruptedException {
        int ret = startJavaProcess( ReadWriteFileLockTestScript.class, path.toString(), shared ? "shared" : "exclusive" ).waitFor();
        switch ( ret ) {
            case 0:
                return true;
            case 2:
                return false;
            default:
                throw new RuntimeException( "LockFile failed with exit code " + ret + "." );
        }
    }

    private void assertThatPosixLockExists( Path path, boolean shared ) throws IOException {
        // TODO: get the PID, Java 9 has an API for that
        String pid = "\\d+";
        Long inode = ( Long ) Files.getAttribute( path, "unix:ino" );
        assertThat( Paths.get( "/proc/locks" ) )
                .content( StandardCharsets.UTF_8 )
                .containsPattern( ".*: POSIX  ADVISORY  " + ( shared ? "READ" : "WRITE" ) + " " + pid + " .+:.+:" + inode + " 0 EOF" );
    }

    private void assertThatPosixLockDoesNotExist( Path path, boolean shared ) throws IOException {
        // TODO: get the PID, Java 9 has an API for that
        String pid = "\\d+";
        Long inode = ( Long ) Files.getAttribute( path, "unix:ino" );
        assertThat( Paths.get( "/proc/locks" ) )
                .content( StandardCharsets.UTF_8 )
                .doesNotContainPattern( ".*: POSIX  ADVISORY  " + ( shared ? "READ" : "WRITE" ) + " " + pid + " .+:.+:" + inode + " 0 EOF" );
    }
}