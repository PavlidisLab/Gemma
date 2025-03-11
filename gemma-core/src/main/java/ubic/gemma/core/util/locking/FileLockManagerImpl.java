package ubic.gemma.core.util.locking;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.file.PathUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author poirigui
 */
@Component
@CommonsLog
public class FileLockManagerImpl implements FileLockManager {

    private static final Map<Path, ReadWriteFileLock> fileLocks = Collections.synchronizedMap( new WeakHashMap<>() );

    @Override
    public Map<Path, FileLockInfo> getAllLockInfos() {
        return fileLocks.entrySet().stream()
                .filter( e -> e.getValue().getChannelHoldCount() > 0 )
                .collect( Collectors.toMap( Map.Entry::getKey, e -> createLockInfo( e.getKey(), e.getValue() ) ) );
    }

    @Override
    public FileLockInfo getLockInfo( Path path ) {
        return createLockInfo( path, fileLocks.get( path ) );
    }

    private FileLockInfo createLockInfo( Path path, @Nullable ReadWriteFileLock lock ) {
        if ( lock == null ) {
            return new FileLockInfo( path, 0, 0, 0, false, 0 );
        }
        return new FileLockInfo( path, lock.getReadHoldCount(), lock.getReadLockCount(), lock.getWriteHoldCount(),
                lock.isWriteLocked(), lock.getChannelHoldCount() );
    }

    @Override
    public LockedPath acquirePathLock( Path path, boolean exclusive ) {
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, FileLockManagerImpl::createReadWriteLock );
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
        ReadWriteLock rwLock = fileLocks.computeIfAbsent( path, FileLockManagerImpl::createReadWriteLock );
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

    private static ReadWriteFileLock createReadWriteLock( Path path ) {
        Path lockPath = path.resolveSibling( path.getFileName() + ".lock" );
        try {
            PathUtils.createParentDirectories( lockPath );
            return ReadWriteFileLock.open( lockPath );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
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
