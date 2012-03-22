/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.job;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import ubic.gemma.job.grid.util.SpaceMonitor;
import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.grid.util.SpacesUtilImpl;
import ubic.gemma.job.progress.ProgressJob;
import ubic.gemma.job.progress.ProgressManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.MailEngine;

/**
 * Handles the execution of tasks in threads that can be check by clients later.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class TaskRunningServiceImpl implements TaskRunningService {

    private static class SubmittedTask {
        private TaskCommand command;
        private Future<?> future;

        public SubmittedTask( Future<?> future, TaskCommand command ) {
            super();
            this.future = future;
            this.command = command;
        }

        /**
         * @return the command
         */
        public TaskCommand getCommand() {
            return command;
        }

        public Future<?> getFuture() {
            return future;
        }
    }

    private static Log log = LogFactory.getLog( TaskRunningServiceImpl.class );

    /**
     * How long we will hold onto results after a task has finished before giving up.
     */
    private static final int MAX_TRACKING_MINUTES = 10;

    /**
     * How often we look for tasks to cleanup (milliseconds). This should be set to be longer than the grid monitor task
     * interval. The longer it is, the longer it will be before dead tasks are noticed.
     */
    private static final int TASK_CLEANUP_FREQUENCY = 30000;

    private final Map<String, TaskCommand> cancelledTasks = new ConcurrentHashMap<String, TaskCommand>();

    private final Map<String, TaskResult> failedTasks = new ConcurrentHashMap<String, TaskResult>();

    private final Map<String, TaskResult> finishedTasks = new ConcurrentHashMap<String, TaskResult>();

    @Autowired
    private MailEngine mailEngine;

    @Autowired
    private SpacesUtil spacesUtil;

    @Autowired
    private SpaceMonitor spaceMonitor;

    private final Map<String, SubmittedTask> submittedTasks = new ConcurrentHashMap<String, SubmittedTask>();

    @Autowired
    private UserManager userService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#addEmailAlert(java.lang.String)
     */
    @Override
    public void addEmailAlert( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );

        if ( !this.submittedTasks.containsKey( taskId ) ) {
            throw new IllegalArgumentException(
                    "No such task is currently running. Maybe it finished already? No email will be sent." );
        }

        this.submittedTasks.get( taskId ).getCommand().setEmailAlert( true );

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        /*
         * Start a thread to monitor finished tasks that have not been retrieved
         */
        Thread sweepThread = new Thread( new Runnable() {

            public void run() {
                while ( !Thread.interrupted() ) {

                    synchronized ( this ) {
                        try {
                            this.wait( TASK_CLEANUP_FREQUENCY );
                        } catch ( InterruptedException e ) {
                            return;
                        }
                    }
                    Thread.yield();

                    sweepUp();
                }

            }
        } );

        sweepThread.setDaemon( true );
        sweepThread.setName( "TaskSweepup" );

        sweepThread.start();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#cancelTask(java.lang.String)
     */
    @Override
    public synchronized boolean cancelTask( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );

        log.debug( "Cancelling " + taskId );
        boolean cancelled = false;

        if ( submittedTasks.containsKey( taskId ) ) {
            SubmittedTask toCancel = submittedTasks.get( taskId );

            if ( toCancel.getCommand().isWillRunOnGrid() || SpacesUtilImpl.taskIsRunningOnGrid( taskId ) ) {
                log.info( "Cancelling grid task " + taskId );
                if ( spacesUtil.cancel( taskId ) ) {
                    log.debug( "Space reports  " + taskId + "  is no longer alive" );
                    cancelled = true;
                    if ( !toCancel.getFuture().isDone() ) toCancel.getFuture().cancel( true );
                }

                if ( !toCancel.getFuture().isDone() ) {
                    log.debug( "Couldn't cancel but job is now done/stopped anyway" );
                    cancelled = true;
                    toCancel.getFuture().cancel( true );
                }

            } else {
                log.info( "Cancelling local task  " + taskId );
                cancelled = true;
                if ( !toCancel.getFuture().isDone() ) toCancel.getFuture().cancel( true );
            }

            if ( cancelled ) {
                /*
                 * Note that we do this notification stuff here, not in the callable that is watching it. Don't do it
                 * twice.
                 */
                if ( !toCancel.getFuture().isDone() ) {
                    throw new RuntimeException( "Task " + taskId + " is still running despite apparent cancellation" );
                }

                handleCancel( taskId, toCancel.getCommand() );

            } else {
                /*
                 * This might be because the job doesn't really support cancellation, or could be some other problem.
                 */
                log.warn( "Couldn't cancel " + taskId );
            }
        } else {
            log.warn( "Attempt to cancel a task (" + taskId + ") that has not been submitted or is already gone." );
        }

        return cancelled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#checkResult(java.lang.String)
     */
    @Override
    public synchronized TaskResult checkResult( String taskId ) throws Exception {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );

        log.debug( "entering" );
        if ( this.finishedTasks.containsKey( taskId ) ) {
            log.debug( "Job is finished" );
            return clearFinished( taskId );
        } else if ( this.cancelledTasks.containsKey( taskId ) ) {
            log.debug( "Job was cancelled" );
            clearCancelled( taskId );
            return null;
        } else if ( this.failedTasks.containsKey( taskId ) ) {
            clearFailed( taskId );
            return null;
        } else if ( this.submittedTasks.containsKey( taskId ) ) {
            log.debug( "Job is apparently still running?" );
            return null;
        } else {
            log.debug( "Job isn't running for " + taskId + " , we don't know what happened to it." );
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#getCancelledTasks()
     */
    @Override
    public Collection<TaskCommand> getCancelledTasks() {
        return cancelledTasks.values();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#getFailedTasks()
     */
    @Override
    public Collection<TaskResult> getFailedTasks() {
        return failedTasks.values();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#getFinishedTasks()
     */
    @Override
    public Collection<TaskResult> getFinishedTasks() {
        return finishedTasks.values();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#getSubmittedTasks()
     */
    @Override
    public Collection<TaskCommand> getSubmittedTasks() {
        Collection<TaskCommand> commands = new HashSet<TaskCommand>();
        for ( SubmittedTask st : submittedTasks.values() ) {
            commands.add( st.getCommand() );
        }
        return commands;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.TaskRunningService#submitTask(ubic.gemma.job.BackgroundJob)
     */
    @Override
    public synchronized void submitTask( BackgroundJob<? extends TaskCommand> job ) throws ConflictingTaskException {

        if ( job == null ) throw new IllegalArgumentException( "Must provide a job" );

        checkEligibility( job );

        final TaskCommand taskCommand = job.getCommand();
        final String taskId = taskCommand.getTaskId();
        log.debug( "Submitting " + taskId );
        ProgressManager.createProgressJob( taskCommand );

        // Run the task in its own thread.
        ExecutorService service = Executors.newSingleThreadExecutor();

        /*
         * execute the job ...
         */
        final FutureTask<TaskResult> future = new FutureTask<TaskResult>( job );

        Future<?> runningTask = service.submit( future );
        taskCommand.setSubmissionTime();
        submittedTasks.put( taskId, new SubmittedTask( runningTask, taskCommand ) );

        // Wait for the task to complete in another thread.
        ExecutorService waiting = Executors.newSingleThreadExecutor();
        waiting.submit( new FutureTask<Object>( new Callable<Object>() {
            public Object call() throws Exception {

                TaskResult result = null;
                try {
                    result = future.get(); // Blocks.
                    log.debug( "Got result " + result + " from task " + taskId );

                    handleFinished( taskCommand, result );

                    return result;
                } catch ( CancellationException e ) {
                    // I think this will never happen????
                    log.warn( "Cancellation received for " + taskId );
                    handleCancel( taskId, taskCommand );
                } catch ( InterruptedException e ) {
                    log.warn( "interruption received for " + taskId );
                    handleCancel( taskId, taskCommand );
                } catch ( ExecutionException e ) {
                    /*
                     * This is reached when a task is cancelled.
                     * 
                     * If it's from the grid, it's down three levels...
                     */

                    if ( e.getCause() instanceof InterruptedException
                            || e.getCause().getCause() instanceof InterruptedException
                            || e.getCause() instanceof CancellationException
                            || e.getCause().getCause() instanceof CancellationException
                            || ExceptionUtils.getFullStackTrace( e ).contains( "InterruptedException" ) ) {
                        if ( cancelledTasks.containsKey( taskId ) ) {
                            log.debug( "Looks like " + taskId + " was cancelled" );
                        } else {
                            /*
                             * Don't call handleCancel - it was probably already called.
                             */
                            log.debug( taskId + " was interuppted."
                                    + " Treating it as cancelled (assuming it was already handled)" );
                        }
                    } else {
                        log.error( "Error thrown for " + taskId + ", stack trace follows:", e );
                        handleFailed( taskCommand, e );
                    }
                } catch ( Exception e ) {
                    log.error( "Error thrown for " + taskId + ", stack trace follows:", e );
                    handleFailed( taskCommand, e );
                }
                return null;
            }

        } ) );

        waiting.shutdown();

        // we return immediately
    }

    /**
     * Test whether the job can be run at this time. Checks for already-running tasks of the same type owned by the same
     * user etc.
     * 
     * @param job
     * @throws ConflictingTaskException
     */
    private void checkEligibility( BackgroundJob<? extends TaskCommand> job ) throws ConflictingTaskException {

        if ( job == null ) {
            throw new IllegalArgumentException( "Must provide a job" );
        }

        TaskCommand command = job.getCommand();
        String user = command.getSubmitter();

        for ( String taskId : submittedTasks.keySet() ) {

            SubmittedTask submittedTask = submittedTasks.get( taskId );
            TaskCommand runningCommand = submittedTask.getCommand();

            if ( runningCommand.getSubmitter().equals( user ) && runningCommand.getClass().equals( command.getClass() ) ) {

                // same user has another task running of the same type ...

                /*
                 * This won't be filled in for the just-submitted space task ... but we'd like to check it.
                 */
                if ( command.getTaskInterface() != null ) {

                    String intf = runningCommand.getTaskInterface();
                    String method = runningCommand.getTaskMethod();

                    if ( command.getTaskInterface().equals( intf ) && command.getTaskMethod().equals( method ) ) {
                        checkSubmittedTaskStatus( runningCommand.getTaskId() );
                        throw new ConflictingTaskException( command, runningCommand );
                    }

                }

                /*
                 * Only matters if we don't have the interface information yet.
                 */

                if ( command.getEntityId() == null || command.getEntityId().equals( runningCommand.getEntityId() ) ) {
                    // _probably_ this should be disallowed. But it is _possible_ that it's spurious -- really need the
                    // exact job
                    checkSubmittedTaskStatus( runningCommand.getTaskId() );
                    throw new ConflictingTaskException( command, runningCommand );
                }

            }

        }

    }

    /**
     * Check the finish time. If it's too old, stop waiting for retrieval. Send an email if necessary. This isn't always
     * that big of a deal, especially for tasks that run non-interactively.
     * 
     * @param taskId
     */
    private void checkFinishedTaskStatus( String taskId ) {
        TaskResult o = finishedTasks.get( taskId );

        Date time = o.getFinishTime();
        if ( time.before( DateUtils.addHours( new Date(), MAX_TRACKING_MINUTES ) ) ) {
            log.debug( "Finished task result not retrieved, timing out: " + taskId );
            if ( o.isSendEmailIfNotRetrieved() ) {
                emailNotifyCompletionOfTask( taskId, o );
            }

            finishedTasks.remove( taskId );
        }
    }

    /**
     * Check if a task has been running or queued for too long, and cancel it if necessary. Email alert will always be
     * sent in that case.
     * 
     * @param taskId
     */
    private void checkSubmittedTaskStatus( String taskId ) {
        SubmittedTask t = submittedTasks.get( taskId );

        TaskCommand command = t.getCommand();
        Date subTime = command.getSubmissionTime();
        Date startTime = command.getStartTime();

        boolean willRunOnGrid = command.isWillRunOnGrid();

        if ( willRunOnGrid ) {

            /*
             * Make sure the grid hasn't failed on us.
             */
            if ( !SpacesUtilImpl.isSpaceRunning() ) {
                if ( startTime == null || !SpacesUtilImpl.taskIsRunningOnGrid( taskId ) ) {
                    ProgressManager.updateJob( taskId, "The compute grid has failed? Cancelling task " + taskId );
                    command.setEmailAlert( true );
                    cancelTask( taskId );
                    return;
                }
            }
            if ( spaceMonitor.getLastStatusWasOK() ) {
                // reset the flag.
                command.setMayHaveFailed( false );
            } else {

                /*
                 * Something might be wrong.
                 */

                if ( command.isMayHaveFailed() ) {
                    log.error( "Job seems to have failed due to a problem with the grid: " + taskId
                            + " -- space monitor reports bad status " );
                    ProgressManager.updateJob( taskId, "The compute grid has failed? Cancelling task " + taskId );
                    command.setEmailAlert( true );
                    cancelTask( taskId );
                }

                /*
                 * This generates false positives, so we require two consecutive bad checks. This assumes that the
                 * monitor checks are more frequent than the sweepup check.
                 */
                log.warn( "Possible grid problem for job " + taskId + " -- space monitor reports bad status " );
                ProgressManager.updateJob( taskId, "Possible grid problem for job " + taskId
                        + " -- space monitor reports bad status -- hoping for recovery" );
                command.setMayHaveFailed( true );
            }
        }

        if ( startTime == null ) {
            log.debug( "Job is still queued: " + taskId + " " + command.getTaskInterface() );
            Integer maxQueueWait = command.getMaxQueueMinutes();
            assert maxQueueWait != null;
            if ( subTime.before( DateUtils.addMinutes( new Date(), -maxQueueWait ) ) ) {
                log.warn( "Submitted task has been queued for too long (max=" + maxQueueWait + "minutes), cancelling: "
                        + taskId );

                ProgressManager.updateJob( taskId,
                        "The job was queued for too long, so it was cancelled after waiting up to " + maxQueueWait
                                + " minutes." );

                submittedTasks.get( taskId ).getCommand().setEmailAlert( true );
                cancelTask( taskId );
                return;
            }

        } else if ( startTime.before( DateUtils.addMinutes( new Date(), -command.getMaxRuntime() ) ) ) {
            log.warn( "Running task is taking too long, cancelling: " + taskId + " " + command.getTaskInterface() );
            submittedTasks.get( taskId ).getCommand().setEmailAlert( true );
            ProgressManager.updateJob( taskId,
                    "The job took too long to run, so it was cancelled after " + command.getMaxRuntime() + " minutes." );
            cancelTask( taskId );
        }
    }

    /**
     * @param taskId
     * @return
     */
    private void clearCancelled( String taskId ) throws Exception {
        cancelledTasks.remove( taskId );
        ProgressManager.signalCancelled( taskId );
    }

    /**
     * @param taskId
     * @throws Throwable
     */
    private void clearFailed( String taskId ) throws Exception {
        TaskResult result = failedTasks.get( taskId );
        failedTasks.remove( taskId );
        Exception e = result.getException();
        if ( e != null ) {
            log.debug( "Job failed, rethrowing the exception: " + e.getMessage() );
            throw e;
        }

    }

    /**
     * @param taskId
     * @return
     */
    private TaskResult clearFinished( Object taskId ) {
        TaskResult finished = finishedTasks.get( taskId );
        finishedTasks.remove( taskId );
        return finished;
    }

    /**
     * @param taskId
     * @param result. If the submitter is blank, this doesn't do anything.
     */
    private void emailNotifyCompletionOfTask( String taskId, TaskResult result ) {
        if ( StringUtils.isNotBlank( result.getSubmitter() ) ) {
            User user = userService.findByUserName( result.getSubmitter() );

            assert user != null;

            String emailAddress = user.getEmail();

            if ( emailAddress != null ) {
                log.debug( "Sending email notification to " + emailAddress );
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo( emailAddress );
                msg.setFrom( ConfigUtils.getAdminEmailAddress() );
                msg.setSubject( "Gemma task completed" );
                ProgressJob job = ProgressManager.getJob( taskId );

                String messages = "";
                if ( job != null ) {
                    messages = "Event logs:\n";
                    messages = messages + job.getJobInfo().getMessages();
                }

                msg.setText( "A job you started on Gemma is completed (taskid=" + taskId + ", "
                        + result.getTaskInterface() + ")\n\n" + messages + "\n" );

                /*
                 * TODO provide a link to something relevant something like:
                 */
                String url = ConfigUtils.getBaseUrl() + "user/tasks.html?taskId=" + taskId;

                mailEngine.send( msg );
            }
        }
    }

    /**
     * @param taskId
     * @param command
     */
    private void handleCancel( String taskId, TaskCommand command ) {
        cancelledTasks.put( taskId, command );
        submittedTasks.remove( taskId );
        ProgressManager.signalCancelled( taskId );
    }

    /**
     * @param taskId
     * @param e
     */
    private void handleFailed( final TaskCommand command, Exception e ) {
        TaskResult result = new TaskResult( command, null );
        result.setFailed( true );
        result.setException( e );
        failedTasks.put( command.getTaskId(), result );
        submittedTasks.remove( command.getTaskId() );

        if ( command.isEmailAlert() ) {
            this.emailNotifyCompletionOfTask( command.getTaskId(), result );
        }

        ProgressManager.signalFailed( command.getTaskId(), e );
    }

    /**
     * @param taskId
     * @param result
     */
    private void handleFinished( final TaskCommand command, TaskResult result ) {
        finishedTasks.put( command.getTaskId(), result );
        submittedTasks.remove( command.getTaskId() );

        if ( command.isEmailAlert() ) {
            this.emailNotifyCompletionOfTask( command.getTaskId(), result );
        }

        ProgressManager.signalDone( command.getTaskId() );
    }

    /**
     * Clean up leftover results from jobs that finished but have not had results picked up; or which have been queued
     * for too long.
     */
    private void sweepUp() {
        // can cause npe error, breaking hot deploy
        if ( log != null ) log.debug( "Running task result cleanup" );

        if ( finishedTasks.size() > 0 ) log.debug( finishedTasks.size() + " finished tasks in the hold" );
        if ( failedTasks.size() > 0 ) log.debug( failedTasks.size() + " failed tasks in the hold" );
        if ( submittedTasks.size() > 0 ) log.debug( submittedTasks.size() + " started or queued tasks in the pipe" );
        if ( cancelledTasks.size() > 0 ) log.debug( cancelledTasks.size() + " cancelled tasks in the hold" );

        for ( String taskId : submittedTasks.keySet() ) {
            checkSubmittedTaskStatus( taskId );
        }

        for ( String taskId : finishedTasks.keySet() ) {
            checkFinishedTaskStatus( taskId );
        }

        for ( String taskId : failedTasks.keySet() ) {
            TaskResult o = failedTasks.get( taskId );
            Date time = o.getFinishTime();
            if ( time.before( DateUtils.addMinutes( new Date(), -MAX_TRACKING_MINUTES ) ) ) {
                log.debug( "Failed task result not retrieved, timing out: " + taskId );
                failedTasks.remove( taskId );
            }
        }

        for ( String taskId : cancelledTasks.keySet() ) {
            TaskCommand o = cancelledTasks.get( taskId );
            Date subTime = o.getSubmissionTime();
            if ( subTime.before( DateUtils.addMinutes( new Date(), -MAX_TRACKING_MINUTES ) ) ) {
                log.debug( "Cancelled task result not retrieved, timing out: " + taskId + " " + o.getTaskInterface() );
                cancelledTasks.remove( taskId );
            }
        }
    }

}
