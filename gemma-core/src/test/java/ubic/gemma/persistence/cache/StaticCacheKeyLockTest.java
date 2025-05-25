package ubic.gemma.persistence.cache;

import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import ubic.gemma.core.util.concurrent.Executors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class StaticCacheKeyLockTest {

    @Test
    public void test() {
        Cache cache = new ConcurrentMapCache( "test" );

        cache.put( "foo", "bar" );
        StaticCacheKeyLock lock1 = new StaticCacheKeyLock( cache, "foo", true );
        StaticCacheKeyLock lock2 = new StaticCacheKeyLock( cache, "foo", true );
        assertEquals( lock1, lock2 );

        cache.evict( "foo" );
        StaticCacheKeyLock lock3 = new StaticCacheKeyLock( cache, "foo", true );
        assertEquals( lock1, lock3 );
        assertEquals( lock2, lock3 );

        cache.put( "foo", "bar" );
        StaticCacheKeyLock lock4 = new StaticCacheKeyLock( cache, "foo", true );
        assertEquals( lock1, lock4 );
        assertEquals( lock2, lock4 );
    }

    @Test
    public void testLock() throws InterruptedException {
        ExecutorService e = Executors.newSingleThreadExecutor();
        Cache cache = new ConcurrentMapCache( "testcache" );
        Future<Boolean> result;
        CacheKeyLock t1Lock = new StaticCacheKeyLock( cache, "a", false );
        try ( CacheKeyLock.LockAcquisition ignored = t1Lock.lock() ) {
            assertThat( ignored.getKey() ).isEqualTo( "a" );
            assertThat( ignored.isReadOnly() ).isFalse();
            result = e.submit( () -> {
                // will block until acquired
                CacheKeyLock t2Lock = new StaticCacheKeyLock( cache, "a", false );
                try ( CacheKeyLock.LockAcquisition ignored2 = t2Lock.lock() ) {
                    return true;
                }
            } );
            Thread.sleep( 100 );
            assertThat( result ).isNotDone();
            e.shutdownNow(); // will *not* cause t2 to get interrupted
            assertThat( result ).isNotCancelled();
            assertThat( result ).isNotDone();
        }
        // lock is released, t2 can acquire it and return true
        assertThat( result ).succeedsWithin( 10, TimeUnit.MILLISECONDS );
    }

    @Test
    public void testLockInterruptibly() throws InterruptedException {
        ExecutorService e = Executors.newSingleThreadExecutor();
        Cache cache = new ConcurrentMapCache( "testcache" );
        StaticCacheKeyLock t1Lock = new StaticCacheKeyLock( cache, "a", false );
        try ( CacheKeyLock.LockAcquisition ignored = t1Lock.lockInterruptibly() ) {
            Future<Boolean> result = e.submit( () -> {
                // will block until acquired or interrupted
                CacheKeyLock t2Lock = new StaticCacheKeyLock( cache, "a", false );
                try ( CacheKeyLock.LockAcquisition ignored2 = t2Lock.lockInterruptibly() ) {
                    return true;
                }
            } );
            Thread.sleep( 100 );
            assertThat( result ).isNotDone();
            e.shutdownNow(); // will cause t2 to get interrupted
            assertThat( result ).failsWithin( 10, TimeUnit.MILLISECONDS )
                    .withThrowableOfType( ExecutionException.class )
                    .withCauseInstanceOf( InterruptedException.class );
        }
    }
}