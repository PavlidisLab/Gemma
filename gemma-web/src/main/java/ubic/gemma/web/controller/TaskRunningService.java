package ubic.gemma.web.controller;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.progress.ProgressManager;

/**
 * Handles the execution of tasks in threads that can be check by clients later.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="taskRunningService"
 * @spring.property name="progressManager" ref="progressManager"
 */
public class TaskRunningService {

    static Log log = LogFactory.getLog( TaskRunningService.class.getName() );
    
    private static final int KEY_LENGTH = 16;

    ProgressManager progressManager;

    final Map<Object, Future> submittedTasks = new ConcurrentHashMap<Object, Future>();

    final Map<Object, Object> finishedTasks = new ConcurrentHashMap<Object, Object>();

    final Map<Object, Future> cancelledTasks = new ConcurrentHashMap<Object, Future>();

    final Map<Object, Throwable> failedTasks = new ConcurrentHashMap<Object, Throwable>();

    /**
     * Signal that a task should be cancelled.
     * 
     * @param taskId
     */
    public synchronized void cancelTask( Object taskId ) {
        log.debug( "Cancelling " + taskId );
        if ( submittedTasks.containsKey( taskId ) ) {
            Future toCancel = submittedTasks.get( taskId );
            boolean cancelled = toCancel.cancel( true );
            if ( cancelled ) {
                /*
                 * Note that we do this notification stuff here, not in the callable that is watching it. Don't do it
                 * twice.
                 */
                handleCancel( taskId, toCancel );
            } else {
                throw new RuntimeException( "Couldn't cancel " + taskId );
            }
        } else {
            log.warn( "Attempt to cancel a task that has not been submitted" );
            return;
        }
    }

    /**
     * Determine if a task is done. If it is done, the results is retrieved. Results can only be retrieved once, after
     * which the servicer released them.
     * 
     * @param taskId
     * @return
     */
    public synchronized Object checkResult( Object taskId ) throws Throwable {
        log.debug( "entering" );
        if ( this.finishedTasks.containsKey( taskId ) ) {
            log.debug( "Job is finished" );
            return clearFinished( taskId );
        } else if ( this.cancelledTasks.containsKey( taskId ) ) {
            log.debug( "Job was cancelled" );
            return clearCancelled( taskId );
        } else if ( this.failedTasks.containsKey( taskId ) ) {
            clearFailed( taskId );
            return null;
        } else if ( this.submittedTasks.containsKey( taskId ) ) {
            log.debug( "Job is apparently still running?" );
            return null;
        } else {
            throw new IllegalStateException( "Job isn't running, we don't know what happened to it." );
        }
    }

    /**
     * @param taskId
     * @throws Throwable
     */
    private void clearFailed( Object taskId ) throws Throwable {
        Throwable e = failedTasks.get( taskId );
        log.debug( "Job failed, rethrowing the exception: " + e.getMessage() );
        failedTasks.remove( taskId );
        throw e;
    }

    /**
     * @param taskId
     * @return
     */
    private Object clearCancelled( Object taskId ) throws Throwable {
        Future cancelled = cancelledTasks.get( taskId );
        cancelledTasks.remove( taskId );
        try {
            return cancelled.get();
        } catch ( CancellationException e ) {
            ProgressManager.signalCancelled( taskId );
            throw e;
        } catch ( InterruptedException e ) {
            ProgressManager.signalFailed( taskId, e );
            throw new CancellationException( "Job was interrupted:" + e );
        } catch ( ExecutionException e ) {
            ProgressManager.signalFailed( taskId, e );
            throw e.getCause();
        }
    }

    /**
     * @param taskId
     * @return
     */
    private Object clearFinished( Object taskId ) {
        Object finished = finishedTasks.get( taskId );
        finishedTasks.remove( taskId );
        return finished;
    }

    /**
     * @param progressManager the progressManager to set
     */
    public void setProgressManager( ProgressManager progressManager ) {
        this.progressManager = progressManager;
    }

    /**
     * @param taskId
     * @param task
     */
    public synchronized void submitTask( final Object taskId, final FutureTask task ) {
        log.info( "Submitting " + taskId );

        // Run the task in its own thread.
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future runningTask = service.submit( task );
        submittedTasks.put( taskId, runningTask );

        // Wait for the task to complete in this thread.
        ExecutorService waiting = Executors.newSingleThreadExecutor();
        waiting.submit( new FutureTask<Object>( new Callable<Object>() {
            public Object call() throws Exception {
                Object result = null;
                try {
                    result = task.get();
                    log.debug( "Got result " + result + " from task " + taskId );
                    handleFinished( taskId, result );
                    return result;
                } catch ( CancellationException e ) {
                    // I think this will never happen.
                    log.info( "Cancellation received for " + taskId );
                    handleCancel( taskId, task );
                } catch ( ExecutionException e ) {
                    if ( e.getCause() instanceof InterruptedException ) {
                        if ( cancelledTasks.containsKey( taskId ) ) {
                            log.debug( "Looks like " + taskId + " was cancelled" );
                        } else {
                            /*
                             * Don't call handleCancel - it was porbably already called.
                             */
                            log.debug( taskId + " was interuppted."
                                    + " Treating it as cancelled (assuming it was already handled)" );
                        }
                    } else {
                        log.info( "Error thrown for " + taskId );
                        handleFailed( taskId, e );
                    }
                } catch ( Exception e ) {
                    log.info( "Error thrown for " + taskId );
                    handleFailed( taskId, e );
                }
                return null;
            }

        } ) );

        waiting.shutdown();

    }

    /**
     * @param taskId
     * @param toCancel
     */
    void handleCancel( Object taskId, Future toCancel ) {
        cancelledTasks.put( taskId, toCancel );
        submittedTasks.remove( taskId );
        ProgressManager.signalCancelled( taskId );
    }

    /**
     * @param taskId
     * @param e
     */
    void handleFailed( final Object taskId, Exception e ) {
        failedTasks.put( taskId, e.getCause() );
        submittedTasks.remove( taskId );
        ProgressManager.signalFailed( taskId, e );
    }

    /**
     * @param taskId
     * @param result
     */
    void handleFinished( final Object taskId, Object result ) {
        finishedTasks.put( taskId, result );
        submittedTasks.remove( taskId );
        ProgressManager.signalDone( taskId );
    }
    
    /**
     * @return
     */
    public static String generateTaskId() {
        return RandomStringUtils.randomAlphanumeric( KEY_LENGTH );
    }

}
