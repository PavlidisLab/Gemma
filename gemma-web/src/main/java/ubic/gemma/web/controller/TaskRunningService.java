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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.progress.ProgressManager;

/**
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="taskRunningService"
 * @spring.property name="progressManager" ref="progressManager"
 */
public class TaskRunningService {

    static Log log = LogFactory.getLog( TaskRunningService.class.getName() );

    private Future monitorFuture;

    ProgressManager progressManager;

    final Map<Object, Future> submittedTasks = new ConcurrentHashMap<Object, Future>();

    final Map<Object, Object> finishedTasks = new ConcurrentHashMap<Object, Object>();

    final Map<Object, Future> cancelledTasks = new ConcurrentHashMap<Object, Future>();

    final Map<Object, Throwable> failedTasks = new ConcurrentHashMap<Object, Throwable>();

    ExecutorService executorService;

    /**
     * 
     *
     */
    public TaskRunningService() {
        executorService = Executors.newCachedThreadPool();
        startUp();
    }

    public synchronized void cancelTask( Object taskId ) {
        log.info( "Cancelling " + taskId );
        if ( submittedTasks.containsKey( taskId ) ) {
            Future toCancel = submittedTasks.get( taskId );
            boolean cancelled = toCancel.cancel( true );
            if ( cancelled ) {
                handleCancel( taskId, toCancel );
            } else {
                throw new RuntimeException( "couldn't cancel " + taskId );
            }
        } else {
            log.warn( "Attempt to cancel a task that has not been submitted" );
            return;
        }
    }

    /**
     * Call to determine if a task is done. If it is done, the results is retrieved.
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
     * Use this to shut down the service.
     */
    public void shutDown() {
        if ( monitorFuture != null ) {
            log.info( "Shutting down task monitor" );
            monitorFuture.cancel( true );
            monitorFuture = null;
        }
    }

    /**
     * Start the service running.
     */
    public void startUp() {
        if ( monitorFuture == null || monitorFuture.isCancelled() || monitorFuture.isDone() ) {
            log.info( "Starting the service" );
            // start the monitoring thread
            // ExecutorService monitorService = Executors.newSingleThreadExecutor();
            // FutureTask monitor = getMonitorTask();
            // monitorFuture = monitorService.submit( monitor );
        } else {
            log.info( "Service seems to be running, will not start" );
        }
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
                            log.debug( taskId + " was interuppted...hmm. Treating it as cancelled." );
                            handleCancel( taskId, task );
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

    // /**
    // * This is the task that periodically checks submitted jobs for ones that have completed.
    // *
    // * @return
    // */
    // FutureTask<Object> getMonitorTask() {
    // return new FutureTask<Object>( new Callable<Object>() {
    //
    // Log log = LogFactory.getLog( TaskRunningService.class.getName() );
    //
    // public Object call() throws Exception {
    // while ( true ) {
    //
    // // flagged for removal
    // Collection<Object> nonRunningTaskIds = new HashSet<Object>();
    //
    // for ( Object key : submittedTasks.keySet() ) {
    //
    // Future task = submittedTasks.get( key );
    //
    // if ( task.isCancelled() ) {
    // nonRunningTaskIds.add( key );
    // log.debug( "Task with key " + key + " was cancelled" );
    // cancelledTasks.put( key, task );
    // ProgressManager.signalCancelled( key );
    // } else if ( task.isDone() ) {
    // nonRunningTaskIds.add( key );
    // log.debug( "Task with key " + key + " is done" );
    // finishedTasks.put( key, task );
    // ProgressManager.signalDone( key );
    // }
    // }
    //
    // for ( Object id : nonRunningTaskIds ) {
    // submittedTasks.remove( id );
    // }
    //
    // Thread.sleep( 200 );
    //
    // }
    //
    // }
    // } );
    // }
}
