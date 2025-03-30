package ubic.gemma.cli.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A task executor that automatically reports errors in batch tasks.
 */
@CommonsLog
class BatchTaskExecutorService extends AbstractDelegatingExecutorService {

    private final BatchTaskProgressReporter progressReporter;

    private final AtomicInteger batchTaskCounter = new AtomicInteger( 0 );
    private final AtomicInteger completedBatchTasks = new AtomicInteger( 0 );

    BatchTaskExecutorService( ExecutorService delegate, BatchTaskProgressReporter progressReporter ) {
        super( delegate );
        this.progressReporter = progressReporter;
    }

    @Override
    protected Runnable wrap( Runnable runnable ) {
        Object batchObject = String.format( "Batch task #%d", batchTaskCounter.incrementAndGet() );
        return () -> {
            try {
                runnable.run();
                if ( !progressReporter.wasSuccessObjectAdded() && !progressReporter.wasErrorObjectAdded() ) {
                    progressReporter.addSuccessObject( batchObject );
                }
            } catch ( Exception e ) {
                if ( !progressReporter.wasErrorObjectAdded() ) {
                    progressReporter.addErrorObject( batchObject, e );
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
        Object batchObject = String.format( "Batch task #%d", batchTaskCounter.incrementAndGet() );
        return () -> {
            try {
                T result = callable.call();
                if ( !progressReporter.wasSuccessObjectAdded() && !progressReporter.wasErrorObjectAdded() ) {
                    progressReporter.addSuccessObject( batchObject );
                }
                return result;
            } catch ( Exception e ) {
                if ( !progressReporter.wasErrorObjectAdded() ) {
                    progressReporter.addErrorObject( batchObject, e );
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
    int getRemainingTasks() {
        Assert.state( isShutdown(), "Executor service is still running, cannot calculate the number of remaining tasks." );
        return batchTaskCounter.get() - completedBatchTasks.get();
    }
}
