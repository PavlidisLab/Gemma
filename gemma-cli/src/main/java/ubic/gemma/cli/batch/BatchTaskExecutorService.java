package ubic.gemma.cli.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import ubic.gemma.core.util.concurrent.AbstractDelegatingExecutorService;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A task executor that automatically reports errors in batch tasks.
 */
@Slf4j
public class BatchTaskExecutorService extends AbstractDelegatingExecutorService implements AutoCloseable {

    private final BatchTaskProgressReporter progressReporter;

    private final AtomicInteger batchTaskCounter = new AtomicInteger( 0 );
    private final AtomicInteger completedBatchTasks = new AtomicInteger( 0 );

    public BatchTaskExecutorService( ExecutorService delegate, BatchTaskProgressReporter progressReporter ) {
        super( delegate );
        this.progressReporter = progressReporter;
    }

    public BatchTaskProgressReporter getProgressReporter() {
        return progressReporter;
    }

    @Override
    protected Runnable wrap( Runnable runnable ) {
        String batchObject = String.format( "Batch task #%d", batchTaskCounter.incrementAndGet() );
        return () -> {
            try {
                runnable.run();
                if ( !progressReporter.wasResultAdded() ) {
                    progressReporter.addSuccessObject( batchObject );
                }
            } catch ( Throwable e ) {
                // Throwable, not Exception: an Error recorded nothing, so a run whose tasks all failed that way
                // exited 0
                if ( !progressReporter.wasErrorObjectAdded() ) {
                    progressReporter.addErrorObject( batchObject, e.getMessage(), e );
                }
                throw e;
            } finally {
                completedBatchTasks.incrementAndGet();
                progressReporter.clearThreadLocals();
            }
        };
    }

    @Override
    protected <T> Callable<T> wrap( Callable<T> callable ) {
        String batchObject = String.format( "Batch task #%d", batchTaskCounter.incrementAndGet() );
        return () -> {
            try {
                T result = callable.call();
                if ( !progressReporter.wasResultAdded() ) {
                    progressReporter.addSuccessObject( batchObject );
                }
                return result;
            } catch ( Throwable e ) {
                if ( !progressReporter.wasErrorObjectAdded() ) {
                    progressReporter.addErrorObject( batchObject, e.getMessage(), e );
                }
                throw e;
            } finally {
                completedBatchTasks.incrementAndGet();
                progressReporter.clearThreadLocals();
            }
        };
    }

    /**
     * Obtain the number of completed batch tasks.
     */
    public int getRemainingTasks() {
        Assert.state( isShutdown(), "Executor service is still running, cannot calculate the number of remaining tasks." );
        return batchTaskCounter.get() - completedBatchTasks.get();
    }

    public void shutdownAndAwaitTermination() throws InterruptedException {
        shutdown();
        if ( isTerminated() ) {
            return;
        }
        int remainingTasks = getRemainingTasks();
        if ( remainingTasks > 0 ) {
            log.info( String.format( "Awaiting for %d batch tasks to finish...", remainingTasks ) );
        }
        progressReporter.setEstimatedMaxTasks( progressReporter.getCompletedTasks() + remainingTasks );
        while ( !awaitTermination( 100, TimeUnit.MILLISECONDS ) ) {
            progressReporter.reportProgress();
        }
    }

    /**
     * Shut the executor down (queued tasks still run) and close the progress reporter.
     * <p>
     * This used to close only the reporter. On a path that skipped every shutdown call, the executor's idle
     * non-daemon threads then kept the JVM from exiting.
     */
    @Override
    public void close() {
        shutdown();
        try {
            progressReporter.close();
        } catch ( IOException e ) {
            log.error( "Failed to close batch task executor.", e );
        }
    }
}
