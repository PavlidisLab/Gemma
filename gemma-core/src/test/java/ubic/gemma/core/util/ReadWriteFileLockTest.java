package ubic.gemma.core.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReadWriteFileLockTest {

    @Test
    public void test() throws IOException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ) );

        lock.writeLock().lock();
        Files.write( lock.getPath(), Collections.singleton( "Hello world!" ), StandardCharsets.UTF_8 );
        lock.writeLock().unlock();

        lock.readLock().lock();
        assertThat( Files.readAllLines( lock.getPath(), StandardCharsets.UTF_8 ) )
                .containsExactly( "Hello world!" );
        lock.readLock().unlock();

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

    @Test
    public void testConcurrentReadWriteAccess() throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ) );
        lock.writeLock().lock();
        Files.write( lock.getPath(), Collections.singleton( "Hello world!" ), StandardCharsets.UTF_8 );

        AtomicBoolean result = new AtomicBoolean();
        AtomicBoolean result2 = new AtomicBoolean();
        AtomicBoolean result3 = new AtomicBoolean();
        Thread t = new Thread( () -> {
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

        Thread t2 = new Thread( () -> {
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

    @Test
    public void testReentrantLocking() throws IOException {
        Path tempDir = Files.createTempDirectory( "test" );
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ) );
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
        ReadWriteFileLock lock = ReadWriteFileLock.open( tempDir.resolve( "test.txt" ) );
        lock.readLock().lock();
        lock.readLock().lock();

        Thread t = new Thread( () -> {
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
}