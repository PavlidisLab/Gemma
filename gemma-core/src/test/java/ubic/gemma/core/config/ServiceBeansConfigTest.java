package ubic.gemma.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceBeansConfigTest {

    /**
     * The file-writing executor runs its tasks on daemon threads, and nothing waited for them before the CLI's
     * {@code System.exit}: the DEA archive written after an analysis was halted mid-write. Closing the context, which
     * GemmaCLI's shutdown hook does, must wait for a pending task.
     */
    @Test
    void closingTheContextWaitsForPendingDataFileTasks() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        Map<String, Object> properties = new HashMap<>();
        properties.put( "gemma.hosturl", "https://gemma.msl.ubc.ca" );
        properties.put( "gemma.localTasks.corePoolSize", "1" );
        properties.put( "gemma.expressionDataFileTasks.shutdownTimeoutSeconds", "30" );
        ctx.getEnvironment().getPropertySources().addFirst( new MapPropertySource( "test", properties ) );
        ctx.register( ServiceBeansConfig.class );
        ctx.refresh();

        AsyncTaskExecutor executor = ctx.getBean( "expressionDataFileTaskExecutor", AsyncTaskExecutor.class );
        CountDownLatch started = new CountDownLatch( 1 );
        CountDownLatch release = new CountDownLatch( 1 );
        AtomicBoolean completed = new AtomicBoolean();
        executor.submit( () -> {
            started.countDown();
            release.await();
            completed.set( true );
            return null;
        } );
        assertThat( started.await( 10, TimeUnit.SECONDS ) ).isTrue();

        Thread closer = new Thread( ctx::close, "context-closer" );
        closer.start();
        try {
            closer.join( 500 );
            assertThat( closer.isAlive() ).as( "closing the context waits for the pending task" ).isTrue();
        } finally {
            release.countDown();
        }
        closer.join( 10_000 );
        assertThat( closer.isAlive() ).isFalse();
        assertThat( completed ).isTrue();
    }
}
