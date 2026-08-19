package ubic.gemma.core.util.locking;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.file.PathUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.core.util.runtime.ExtendedRuntime;

import org.springframework.lang.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author poirigui
 */
@Component
@Slf4j
public class FileLockManagerImpl implements FileLockManager {

    /**
     * 🛑 This map must NEVER evict an entry whose lock is still held. The JVM's file-lock table is
     * global: two {@link ReadWriteFileLock} instances for the same file throw
     * {@link java.nio.channels.OverlappingFileLockException} the moment both try to lock, which is
     * exactly what a {@code WeakHashMap} produced here — its entry was reachable only through the
     * FIRST acquirer's {@code Path} object, so when that caller closed (a cache probe, typically)
     * while a LATER caller still held the lock through an equal-but-distinct {@code Path}, GC evicted
     * the entry mid-hold and the next request minted a second instance against the held file
     * (observed 2026-08-19: every {@code /data/raw} probe 500ing instantly while a disconnected
     * caller's build still held the exclusive lock). The cost of never evicting is one small idle
     * entry per distinct file path touched over the JVM's lifetime, which is bounded and inert; the
     * cost of evicting wrongly was a deterministic 500.
     */
    private static final Map<Path, ReadWriteFileLock> fileLocks = new ConcurrentHashMap<>();

    @Override
    public Collection<FileLockInfo> getAllLockInfos() throws IOException {
        Map<Long, List<ubic.gemma.core.util.runtime.FileLockInfo>> lockMetadata = Arrays.stream( ExtendedRuntime.getRuntime().getFileLockInfo() )
                .collect( Collectors.groupingBy( ubic.gemma.core.util.runtime.FileLockInfo::getInode, Collectors.toList() ) );
        // ConcurrentHashMap iteration is safe against concurrent computeIfAbsent from request
        // threads; snapshot only so the stream sees one consistent moment.
        List<Map.Entry<Path, ReadWriteFileLock>> snapshot = new ArrayList<>( fileLocks.entrySet() );
        return snapshot.stream()
                // only display files with active locks
                .filter( e -> e.getValue().getChannelHoldCount() > 0 )
                .map( e -> createLockInfo( e.getKey(), e.getValue(), lockMetadata ) )
                .collect( Collectors.toList() );
    }

    @Override
    @SuppressWarnings("resource")
    public Stream<FileLockInfo> getAllLockInfosByWalking( Path directory, int maxDepth ) throws IOException {
        Map<Long, List<ubic.gemma.core.util.runtime.FileLockInfo>> lockMetadata = Arrays.stream( ExtendedRuntime.getRuntime().getFileLockInfo() )
                .collect( Collectors.groupingBy( ubic.gemma.core.util.runtime.FileLockInfo::getInode, Collectors.toList() ) );
        return Files.walk( directory, maxDepth )
                .map( p -> {
                    if ( fileLocks.containsKey( p ) ) {
                        return createLockInfo( p, fileLocks.get( p ), lockMetadata );
                    } else if ( Files.exists( resolveLockPath( p ) ) ) {
                        return createLockInfo( p, null, lockMetadata );
                    } else {
                        // we don't know about the lock and there is no lock file, ignore
                        return null;
                    }
                } )
                .filter( Objects::nonNull );
    }

    @Override
    public ubic.gemma.core.util.locking.FileLockInfo getLockInfo( Path path ) throws IOException {
        Map<Long, List<ubic.gemma.core.util.runtime.FileLockInfo>> lockMetadata = Arrays.stream( ExtendedRuntime.getRuntime().getFileLockInfo() )
                .collect( Collectors.groupingBy( ubic.gemma.core.util.runtime.FileLockInfo::getInode, Collectors.toList() ) );
        return createLockInfo( path, fileLocks.get( path ), lockMetadata );
    }

    private FileLockInfo createLockInfo( Path path, @Nullable ReadWriteFileLock lock, Map<Long, List<ubic.gemma.core.util.runtime.FileLockInfo>> procInfosPerInode ) {
        List<ubic.gemma.core.util.runtime.FileLockInfo> procInfosForInode;
        Path lockfilePath = resolveLockPath( path );
        try {
            Long inode = ( Long ) Files.getAttribute( lockfilePath, "unix:ino" );
            procInfosForInode = procInfosPerInode.getOrDefault( inode, Collections.emptyList() );
        } catch ( NoSuchFileException e ) {
            // simply means there is no lock file, no need to warn
            procInfosForInode = Collections.emptyList();
        } catch ( IOException e ) {
            log.warn( "Failed to get inode number for " + path + ", will not populate process info.", e );
            procInfosForInode = Collections.emptyList();
        }
        if ( lock == null ) {
            return new FileLockInfo( path, lockfilePath, 0, 0, 0, false,
                    0, procInfosForInode );
        }
        return new FileLockInfo( path, lockfilePath, lock.getReadHoldCount(), lock.getReadLockCount(), lock.getWriteHoldCount(),
                lock.isWriteLocked(), lock.getChannelHoldCount(), procInfosForInode );
    }

    @Override
    public LockedPath acquirePathLock( Path path, boolean exclusive ) {
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, this::createReadWriteLock );
        log.debug( "Acquiring " + ( exclusive ? "exclusive" : "shared" ) + " lock on " + path + "..." );
        if ( exclusive ) {
            rwLock.writeLock().lock();
            log.debug( "Got exclusive lock on " + path + "." );
            return new LockedPathImpl( path, rwLock.writeLock(), false );
        } else {
            rwLock.readLock().lock();
            log.debug( "Got shared lock on " + path + "." );
            return new LockedPathImpl( path, rwLock.readLock(), true );
        }
    }

    @Override
    public LockedPath tryAcquirePathLock( Path path, boolean exclusive, long timeout, TimeUnit timeUnit ) throws TimeoutException, InterruptedException {
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, this::createReadWriteLock );
        log.debug( "Acquiring " + ( exclusive ? "exclusive" : "shared" ) + " lock on " + path + "..." );
        if ( exclusive ) {
            if ( rwLock.writeLock().tryLock( timeout, timeUnit ) ) {
                log.debug( "Got exclusive lock on " + path + "." );
                return new LockedPathImpl( path, rwLock.writeLock(), false );
            } else {
                throw new TimeoutException( "Could not acquire exclusive lock on " + path + " within " + timeout + " " + timeUnit + "." );
            }
        } else {
            if ( rwLock.readLock().tryLock( timeout, timeUnit ) ) {
                log.debug( "Got shared lock on " + path + "." );
                return new LockedPathImpl( path, rwLock.readLock(), true );
            } else {
                throw new TimeoutException( "Could not acquire shared lock on " + path + " within " + timeout + " " + timeUnit + "." );
            }
        }
    }

    @Override
    public InputStream newInputStream( Path path, OpenOption... openOptions ) throws IOException {
        try ( LockedPath lockedPath = acquirePathLock( path, false ) ) {
            InputStream inputStream = Files.newInputStream( path, openOptions );
            return new LockedPathInputStream( inputStream, lockedPath );
        }
    }

    @Override
    public Reader newBufferedReader( Path path ) throws IOException {
        try ( LockedPath lockedPath = acquirePathLock( path, false ) ) {
            BufferedReader bufferedReader = Files.newBufferedReader( path );
            return new LockedPathReader( bufferedReader, lockedPath );
        }
    }

    @Override
    public OutputStream newOutputStream( Path path, OpenOption... openOptions ) throws IOException {
        try ( LockedPath lockedPath = acquirePathLock( path, true ) ) {
            OutputStream outputStream = Files.newOutputStream( path, openOptions );
            return new LockedPathOutputStream( outputStream, lockedPath );
        }
    }

    @Override
    public Writer newBufferedWriter( Path path, OpenOption... openOptions ) throws IOException {
        try ( LockedPath lockedPath = acquirePathLock( path, true ) ) {
            BufferedWriter bufferedWriter = Files.newBufferedWriter( path, openOptions );
            return new LockedPathWriter( bufferedWriter, lockedPath );
        }
    }

    private ReadWriteFileLock createReadWriteLock( Path path ) {
        Path lockPath = resolveLockPath( path );
        try {
            PathUtils.createParentDirectories( lockPath );
            if ( !Files.isWritable( lockPath.getParent() ) ) {
                throw new IOException( "The directory " + lockPath.getParent() + " is not writable, cannot create a lockfile within it" );
            }
            // delete on close only if the lock file is not the same as the path
            return ReadWriteFileLock.open( lockPath, !lockPath.equals( path ) );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private Path resolveLockPath( Path path ) {
        return path.resolveSibling( path.getFileName() + ".lock" );
    }

    private static class LockedPathInputStream extends FilterInputStream {
        private final LockedPath lockedPath;

        public LockedPathInputStream( InputStream inputStream, LockedPath lockedPath ) {
            super( inputStream );
            this.lockedPath = lockedPath.steal();
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                lockedPath.close();
            }
        }
    }

    private static class LockedPathReader extends FilterReader {
        private final LockedPath lockedPath;

        public LockedPathReader( BufferedReader bufferedReader, LockedPath lockedPath ) {
            super( bufferedReader );
            this.lockedPath = lockedPath.steal();
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                lockedPath.close();
            }
        }
    }

    private static class LockedPathOutputStream extends FilterOutputStream {
        private final LockedPath lockedPath;

        public LockedPathOutputStream( OutputStream outputStream, LockedPath steal ) {
            super( outputStream );
            lockedPath = steal.steal();
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                lockedPath.close();
            }
        }
    }

    private static class LockedPathWriter extends FilterWriter {
        private final LockedPath lockedPath;

        public LockedPathWriter( BufferedWriter bufferedWriter, LockedPath lockedPath ) {
            super( bufferedWriter );
            this.lockedPath = lockedPath.steal();
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                lockedPath.close();
            }
        }
    }

    private class LockedPathImpl implements LockedPath {

        private final Path path;
        private final Lock lock;
        private final boolean shared;

        /**
         * Indicate if this lock was closed.
         */
        private boolean closed = false;
        /**
         * Indicate if this lock was stolen and thus not to be closed.
         */
        private boolean stolen = false;

        public LockedPathImpl( Path path, Lock lock, boolean shared ) {
            this.path = path;
            this.lock = lock;
            this.shared = shared;
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public boolean isValid() {
            return !closed && !stolen;
        }

        @Override
        public boolean isShared() {
            return shared;
        }

        @Override
        public synchronized void close() {
            if ( !closed && !stolen ) {
                lock.unlock();
                closed = true;
            }
        }

        @Override
        public Path closeAndGetPath() {
            close();
            return path;
        }

        @Override
        public LockedPath toExclusive() {
            Assert.state( isValid(), "This lock is not valid." );
            Assert.state( shared, "This lock is already exclusive." );
            return acquirePathLock( closeAndGetPath(), true );
        }

        @Override
        public LockedPath toExclusive( long timeout, TimeUnit timeUnit ) throws InterruptedException, TimeoutException {
            Assert.state( isValid(), "This lock is not valid." );
            Assert.state( shared, "This lock is already exclusive." );
            return tryAcquirePathLock( closeAndGetPath(), true, timeout, timeUnit );
        }

        @Override
        public LockedPath toShared() {
            Assert.state( isValid(), "This lock is not valid." );
            Assert.state( !shared, "This lock is already shared." );
            return acquirePathLock( closeAndGetPath(), false );
        }

        @Override
        public synchronized LockedPath steal() {
            Assert.state( isValid(), "This lock is not valid." );
            try {
                return new LockedPathImpl( path, lock, shared );
            } finally {
                stolen = true;
            }
        }

        @Override
        public synchronized LockedPath stealWithPath( Path file ) {
            Assert.state( isValid(), "This lock is not valid." );
            try {
                return new LockedPathImpl( file, lock, shared );
            } finally {
                stolen = true;
            }
        }

        @Override
        public String toString() {
            return path + " " + ( shared ? "[shared]" : "[exclusive]" );
        }
    }
}
