package ubic.gemma.core.util.concurrent;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ThreadUtilsTest {

    @Test
    public void testNewThread() throws InterruptedException {
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_THREADLOCAL );
        AtomicBoolean done = new AtomicBoolean();
        ThreadContext.put( "foo", "bar" );
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext( ctx );
        Thread t = ThreadUtils.newThread( () -> {
            assertEquals( "bar", ThreadContext.get( "foo" ) );
            assertSame( ctx, SecurityContextHolder.getContext() );
            done.set( true );
        } );
        t.start();
        t.join();
        assertTrue( done.get() );
    }
}