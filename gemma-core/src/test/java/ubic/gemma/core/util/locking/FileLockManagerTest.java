package ubic.gemma.core.util.locking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import ubic.gemma.core.util.concurrent.ThreadUtils;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileLockManagerTest {

    private final FileLockManager fileLockManager = new FileLockManagerImpl();

    @AfterEach
    public void tearDown() throws IOException {
        assertThat( fileLockManager.getAllLockInfos() ).isEmpty();
    }

    /**
     * The path→lock mapping must survive as long as any holder does — regardless of WHICH
     * {@code Path} instance each caller used and of what the garbage collector decides.
     * <p>
     * The JVM's file-lock table is global, so two {@link ReadWriteFileLock} instances for one
     * file throw {@link OverlappingFileLockException} the moment both lock. The manager once
     * kept the mapping in a {@code WeakHashMap}, where the entry was reachable only through the
     * FIRST acquirer's key object: a cache probe would create the entry and close, a later
     * caller would hold the lock through an equal-but-distinct {@code Path}, GC would evict the
     * entry mid-hold, and the next probe would mint a second instance against the held file —
     * observed as every {@code /data/raw} request 500ing instantly while a disconnected
     * caller's build still held the exclusive lock (2026-08-19). The {@code System.gc()} calls
     * below made that reproduce; with a strong map they must be irrelevant.
     */
    @Test
    public void testMappingSurvivesTheFirstAcquirersKeyBeingCollected() throws Exception {
        Path dir = Files.createTempDirectory( "test" );
        // distinct but equal Path instances, as distinct requests produce
        Path first = dir.resolve( "contested" );
        Path second = dir.resolve( "contested" );
        Path third = dir.resolve( "contested" );

        // the probe: creates the mapping, releases immediately
        fileLockManager.acquirePathLock( first, false ).close();
        //noinspection UnusedAssignment
        first = null; // the map entry must not depend on this instance staying reachable

        // the builder: holds the lock through its own equal Path instance
        try ( LockedPath ignored = fileLockManager.acquirePathLock( second, true ) ) {
            System.gc();
            System.gc();
            // the next probe, on its own thread as in production (same-thread would exercise the
            // unrelated write-to-read downgrade of ReentrantReadWriteLock instead): it must join
            // the SAME lock instance and time out on contention — never mint a second instance
            // and die on the JVM-global overlap check
            Throwable[] fromProbe = new Throwable[1];
            Thread probe = ThreadUtils.newThread( () -> {
                try {
                    fileLockManager.tryAcquirePathLock( third, false, 10, TimeUnit.MILLISECONDS ).close();
                } catch ( Throwable t ) {
                    fromProbe[0] = t;
                }
            } );
            probe.start();
            probe.join();
            assertThat( fromProbe[0] ).isInstanceOf( TimeoutException.class );
        }

        // once the holder is gone, the same path locks fine again
        fileLockManager.acquirePathLock( third, false ).close();
    }

    /**
     * Relies on the {@code /proc/locks} pseudo-file, which only exists on Linux.
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    public void testGetLockInfo() throws IOException {
        Path dir = Files.createTempDirectory( "test" );
        try ( LockedPath lock = fileLockManager.acquirePathLock( dir.resolve( "foo" ), false ) ) {
            assertThat( fileLockManager.getLockInfo( lock.getPath() ) )
                    .satisfies( lockInfo -> {
                        assertThat( lockInfo.getProcInfo() )
                                .singleElement()
                                .satisfies( procInfo -> {
                                    assertThat( procInfo.isMandatory() ).isFalse();
                                    assertThat( procInfo.isExclusive() ).isFalse();
                                    assertThat( procInfo.getStart() ).isEqualTo( 0 );
                                    assertThat( procInfo.getLength() ).isEqualTo( Long.MAX_VALUE );
                                } );
                    } );
        }
        try ( LockedPath lock = fileLockManager.acquirePathLock( dir.resolve( "foo" ), true ) ) {
            assertThat( fileLockManager.getLockInfo( lock.getPath() ) )
                    .satisfies( lockInfo -> {
                        assertThat( lockInfo.getProcInfo() )
                                .singleElement()
                                .satisfies( procInfo -> {
                                    assertThat( procInfo.isMandatory() ).isFalse();
                                    assertThat( procInfo.isExclusive() ).isTrue();
                                    assertThat( procInfo.getStart() ).isEqualTo( 0 );
                                    assertThat( procInfo.getLength() ).isEqualTo( Long.MAX_VALUE );
                                } );
                    } );
        }
    }

    @Test
    public void testReentrant() throws IOException {
        Path dir = Files.createTempDirectory( "test" );
        try ( LockedPath lock = fileLockManager.acquirePathLock( dir.resolve( "foo" ), false ) ) {
            assertThat( dir.resolve( "foo.lock" ) ).exists();
            assertThat( lock.isValid() ).isTrue();
            assertThat( fileLockManager.getAllLockInfos() )
                    .extracting( FileLockInfo::getPath )
                    .contains( dir.resolve( "foo" ) );
            assertThat( fileLockManager.getLockInfo( dir.resolve( "foo" ) ).getReadLockCount() ).isEqualTo( 1 );
            assertThat( fileLockManager.getLockInfo( dir.resolve( "foo" ) ).isWriteLocked() ).isFalse();
            try ( LockedPath lock2 = fileLockManager.acquirePathLock( dir.resolve( "foo" ), false ) ) {
                assertThat( fileLockManager.getLockInfo( dir.resolve( "foo" ) ).getReadLockCount() ).isEqualTo( 2 );
                // write lock is still held, this is blocking
                assertThatThrownBy( () -> lock2.toExclusive( 0, TimeUnit.MILLISECONDS ) )
                        .isInstanceOf( TimeoutException.class );
            }
            assertThat( fileLockManager.getLockInfo( dir.resolve( "foo" ) ).getReadLockCount() ).isEqualTo( 1 );
        }
        assertThat( dir.resolve( "foo" ) ).doesNotExist();
        assertThat( dir.resolve( "foo.lock" ) ).doesNotExist();

        assertThat( fileLockManager.getAllLockInfos() )
                .isEmpty();

        try ( LockedPath lock = fileLockManager.acquirePathLock( dir.resolve( "foo" ), true ) ) {
            assertThat( lock.isValid() ).isTrue();
            assertThat( lock.isShared() ).isFalse();
            try ( LockedPath lock2 = fileLockManager.acquirePathLock( dir.resolve( "foo" ), true ) ) {
                assertThat( lock2.isValid() ).isTrue();
                assertThat( lock2.isShared() ).isFalse();
                // this is creating a read lock on a file that is still being held by a write lock, it is not allowed by
                // the FileLock API
                assertThatThrownBy( lock2::toShared )
                        .isInstanceOf( OverlappingFileLockException.class );
            }
        }

        assertThat( fileLockManager.getAllLockInfos() )
                .isEmpty();
    }

    @Test
    public void testToShared() throws IOException {
        Path dir = Files.createTempDirectory( "test" );
        try ( LockedPath exclusiveLock = fileLockManager.acquirePathLock( dir.resolve( "foo" ), true ) ) {
            assertThat( exclusiveLock.isShared() ).isFalse();
            try ( LockedPath sharedLock = exclusiveLock.toShared() ) {
                assertThat( sharedLock.isShared() ).isTrue();
                assertThat( exclusiveLock.isValid() ).isFalse();
            }
        }
    }

    @Test
    public void testToExclusive() throws IOException {
        Path dir = Files.createTempDirectory( "test" );
        try ( LockedPath sharedLock = fileLockManager.acquirePathLock( dir.resolve( "foo" ), false ) ) {
            assertThat( sharedLock.isShared() ).isTrue();
            try ( LockedPath exclusiveLock = sharedLock.toExclusive() ) {
                assertThat( exclusiveLock.isValid() ).isTrue();
                assertThat( exclusiveLock.isShared() ).isFalse();
                assertThat( sharedLock.isValid() ).isFalse();
            }
        }
    }

    @Test
    public void testSteal() throws IOException {
        Path dir = Files.createTempDirectory( "test" );
        try ( LockedPath lock = fileLockManager.acquirePathLock( dir, true ) ) {
            try ( LockedPath stolenLock = lock.steal() ) {
                assertThat( stolenLock.getPath() ).isEqualTo( lock.getPath() );
                assertThat( stolenLock.isShared() ).isFalse();
                assertThat( stolenLock.isValid() ).isTrue();
                assertThat( lock.isValid() ).isFalse();
            }
        }
    }

    @Test
    public void testStealWithPath() throws IOException {
        Path dir = Files.createTempDirectory( "test" );
        try ( LockedPath lockOnDir = fileLockManager.acquirePathLock( dir, true ) ) {
            try ( LockedPath lock = lockOnDir.stealWithPath( dir.resolve( "test2" ) ) ) {
                assertThat( lock.getPath() ).isEqualTo( dir.resolve( "test2" ) );
                assertThat( lock.isShared() ).isFalse();
                assertThat( lockOnDir.isValid() ).isFalse();
            }
        }
    }

    @Test
    public void testConcurrent() throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory( "test" );

        try ( LockedPath lock = fileLockManager.acquirePathLock( dir, false ) ) {
            assertThat( lock.isValid() ).isTrue();
            Thread t = ThreadUtils.newThread( () -> {
                try ( LockedPath lock2 = fileLockManager.tryAcquirePathLock( dir, false, 0, TimeUnit.MILLISECONDS ) ) {
                    assertThat( lock2.isValid() ).isTrue();
                } catch ( IOException | InterruptedException | TimeoutException e ) {
                    throw new RuntimeException( e );
                }
            } );
            t.start();
            t.join();
        }

        try ( LockedPath lock = fileLockManager.acquirePathLock( dir, true ) ) {
            assertThat( lock.isValid() ).isTrue();
            Thread t = ThreadUtils.newThread( () -> {
                assertThatThrownBy( () -> {
                    try ( LockedPath ignored = fileLockManager.tryAcquirePathLock( dir, false, 0, TimeUnit.MILLISECONDS ) ) {

                    }
                } ).isInstanceOf( TimeoutException.class );
                assertThatThrownBy( () -> {
                    try ( LockedPath ignored = fileLockManager.tryAcquirePathLock( dir, true, 0, TimeUnit.MILLISECONDS ) ) {

                    }
                } ).isInstanceOf( TimeoutException.class );
            } );
            t.start();
            t.join();
        }
    }

    @Test
    public void testTwoManagers() throws IOException {
        FileLockManager manager1 = new FileLockManagerImpl();
        FileLockManager manager2 = new FileLockManagerImpl();
        Path dir = Files.createTempDirectory( "test" );
        try ( LockedPath lock = manager1.acquirePathLock( dir.resolve( "foo" ), true ) ) {
            assertThat( lock.isValid() ).isTrue();
            try ( LockedPath lock2 = manager2.acquirePathLock( dir.resolve( "foo" ), true ) ) {
                assertThat( lock2.isValid() ).isTrue();
            }
        }
    }
}
