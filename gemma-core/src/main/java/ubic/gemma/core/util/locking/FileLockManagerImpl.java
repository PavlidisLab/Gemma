package ubic.gemma.core.util.locking;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.file.PathUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author poirigui
 */
@Component
@CommonsLog
public class FileLockManagerImpl implements FileLockManager {

    private static final Map<Path, ReadWriteFileLock> fileLocks = Collections.synchronizedMap( new WeakHashMap<>() );

    @Override
    public Collection<FileLockInfo> getAllLockInfos() throws IOException {
        Map<Long, List<FileLockInfo.ProcessInfo>> lockMetadata = readLocksMetadata( getPid() ).stream()
                .collect( Collectors.groupingBy( FileLockInfo.ProcessInfo::getInode, Collectors.toList() ) );
        return fileLocks.entrySet().stream()
                // only display files with active locks
                .filter( e -> e.getValue().getChannelHoldCount() > 0 )
                .map( e -> createLockInfo( e.getKey(), e.getValue(), lockMetadata ) )
                .collect( Collectors.toList() );
    }

    @Override
    @SuppressWarnings("resource")
    public Stream<FileLockInfo> getAllLockInfosByWalking( Path directory, int maxDepth ) throws IOException {
        Map<Long, List<FileLockInfo.ProcessInfo>> lockMetadata = readLocksMetadata( getPid() ).stream()
                .collect( Collectors.groupingBy( FileLockInfo.ProcessInfo::getInode, Collectors.toList() ) );
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
    public FileLockInfo getLockInfo( Path path ) throws IOException {
        Map<Long, List<FileLockInfo.ProcessInfo>> lockMetadata = readLocksMetadata( getPid() ).stream()
                .collect( Collectors.groupingBy( FileLockInfo.ProcessInfo::getInode, Collectors.toList() ) );
        return createLockInfo( path, fileLocks.get( path ), lockMetadata );
    }

    private FileLockInfo createLockInfo( Path path, @Nullable ReadWriteFileLock lock, Map<Long, List<FileLockInfo.ProcessInfo>> procInfosPerInode ) {
        List<FileLockInfo.ProcessInfo> procInfosForInode;
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

    private static final Path PROC_SELF_FILE = Paths.get( "/proc/self" );

    private int getPid() throws IOException {
        if ( !Files.exists( PROC_SELF_FILE ) ) {
            log.warn( "No /proc/self file found, cannot determine current process PID." );
            return -1;
        }
        return Integer.parseInt( Files.readSymbolicLink( PROC_SELF_FILE ).getFileName().toString() );
    }

    private static final Path PROC_LOCKS_FILE = Paths.get( "/proc/locks" );
    // we only care about POSIX locks
    private static final Pattern PROC_LOCKS_PATTERN = Pattern.compile( "^(.+): POSIX {2}(ADVISORY|MANDATORY) {2}(READ|WRITE) (\\d+) (.+):(.+):(\\d+) (\\d+) (\\d+|EOF)$" );

    /**
     * Read system-wide lock metadata from /proc/locks.
     * <p>
     * This implementation has been tested on Fedora 41 and Rocky Linux 9.
     * <p>
     * FIXME: locks held through NFS do not show up under /proc/locks.
     */
    private List<FileLockInfo.ProcessInfo> readLocksMetadata( int myPid ) throws IOException {
        if ( !Files.exists( PROC_LOCKS_FILE ) ) {
            log.warn( "No /proc/locks file found, returning empty list." );
            return Collections.emptyList();
        }
        List<FileLockInfo.ProcessInfo> result = new ArrayList<>();
        for ( String line : Files.readAllLines( PROC_LOCKS_FILE ) ) {
            Matcher matcher = PROC_LOCKS_PATTERN.matcher( line );
            if ( !matcher.matches() )
                continue;
            String id = matcher.group( 1 );
            boolean mandatory = "MANDATORY".equals( matcher.group( 2 ) );
            boolean exclusive = "WRITE".equals( matcher.group( 3 ) );
            int pid = Integer.parseInt( matcher.group( 4 ) );
            String majorDevice = matcher.group( 5 );
            String minorDevice = matcher.group( 6 );
            long inode = Long.parseLong( matcher.group( 7 ) );
            long start = Long.parseLong( matcher.group( 8 ) );
            long length = "EOF".equals( matcher.group( 9 ) ) ? Long.MAX_VALUE : ( Long.parseLong( matcher.group( 9 ) ) - start + 1 );
            result.add( new FileLockInfo.ProcessInfo( id, mandatory, exclusive, pid, pid == myPid, majorDevice, minorDevice, inode, start, length ) );
        }
        return result;
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
