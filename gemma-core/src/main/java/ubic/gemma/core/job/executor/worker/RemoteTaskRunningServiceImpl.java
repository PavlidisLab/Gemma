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
package ubic.gemma.core.job.executor.worker;

import com.google.common.util.concurrent.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.infrastructure.common.MessageSender;
import ubic.gemma.core.infrastructure.jms.JMSHelper;
import ubic.gemma.core.infrastructure.jms.JmsMessageSender;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.common.*;
import ubic.gemma.core.tasks.Task;
import ubic.gemma.persistence.util.Settings;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This will run on remote worker jvm CompletionService backed by ThreadPoolExecutor is used to execute tasks.
 * ApplicationContext is injected and is used to load classes implementing Tasks.
 */
@Component("remoteTaskRunningService")
public class RemoteTaskRunningServiceImpl implements RemoteTaskRunningService {
    private static final Log log = LogFactory.getLog( RemoteTaskRunningServiceImpl.class );
    private final Map<String, SubmittedTaskRemote> submittedTasks = new ConcurrentHashMap<>();
    /*
     * thread pool with 10 core threads and a maximum of 15 threads.
     */
    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
            new ThreadPoolExecutor( 10, 15, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>() ) );
    // Take completed Task and send its result to the SubmittedTaskProxy via JMS queue.
    private final FutureCallback<TaskResult> sendTaskResultCallback = new FutureCallback<TaskResult>() {
        @Override
        public void onSuccess( TaskResult taskResult ) {
            // Clean up.
            SubmittedTaskRemote task = submittedTasks.remove( taskResult.getTaskId() );
            task.sendTaskResult();
        }

        @Override
        public void onFailure( Throwable throwable ) {
            // I think this will happen if CancellationException is thrown
            RemoteTaskRunningServiceImpl.log.error( throwable );
        }
    };
    @Autowired
    private TaskPostProcessing taskPostProcessing;
    @Autowired
    private TaskCommandToTaskMatcher taskCommandToTaskMatcher;
    @Autowired
    private JMSHelper jmsHelper;

    @Override
    public void submit( final TaskCommand taskCommand ) {
        this.checkTaskCommand( taskCommand );

        final String taskId = taskCommand.getTaskId();
        final Task<?, ?> task = this.getTask( taskCommand );

        final List<String> localProgressUpdates = new LinkedList<>();
        final SubmittedTaskRemote submittedTask = this
                .constructSubmittedTaskRemote( taskCommand, taskId, localProgressUpdates );

        // Called from log appender that's attached to 'ubic.basecode' and 'ubic.gemma' loggers.
        final ProgressUpdateCallback progressUpdateCallback = new ProgressUpdateCallback() {
            @Override
            public void addProgressUpdate( String message ) {
                submittedTask.addProgressUpdate( message );
                // Keep progress updates locally as well.
                localProgressUpdates.add( message );
            }
        };

        @SuppressWarnings("unchecked") final ExecutingTask<TaskResult> executingTask = ( ExecutingTask<TaskResult> ) new ExecutingTask<>(
                task, taskCommand );
        executingTask.setProgressAppender( new LogBasedProgressAppender( taskId, progressUpdateCallback ) );
        executingTask.setStatusCallback( new ExecutingTask.TaskLifecycleHandler() {
            @Override
            public void onFailure( Throwable e ) {
                RemoteTaskRunningServiceImpl.log.error( e, e );
                submittedTask.updateStatus( new TaskStatusUpdate( SubmittedTask.Status.FAILED ) );
            }

            @Override
            public void onFinish() {
                submittedTask.updateStatus( new TaskStatusUpdate( SubmittedTask.Status.COMPLETED ) );
            }

            @Override
            public void onStart() {
                submittedTask.updateStatus( new TaskStatusUpdate( SubmittedTask.Status.RUNNING ) );
            }
        } );

        ListenableFuture<TaskResult> future = executorService.submit( executingTask );
        submittedTask.setFuture( future );

        // These are run on task completion.
        Futures.addCallback( future, sendTaskResultCallback, executorService );

        if ( taskCommand.isEmailAlert() ) {
            submittedTask.addEmailAlertNotificationAfterCompletion();
        }

        submittedTasks.put( taskId, submittedTask );
    }

    @Override
    public SubmittedTaskRemote getSubmittedTask( String taskId ) {
        return submittedTasks.get( taskId );
    }

    @Override
    public void shutdown() {
        this.executorService.shutdownNow();
    }

    private void checkTaskCommand( TaskCommand taskCommand ) {
        if ( taskCommand == null )
            throw new NullPointerException( "taskCommand cannot be null." );
        assert taskCommand.getTaskId() != null;
        assert taskCommand.getTaskClass() != null;
    }

    private Task<TaskResult, TaskCommand> getTask( TaskCommand taskCommand ) {
        @SuppressWarnings("unchecked") final Task<TaskResult, TaskCommand> task = ( Task<TaskResult, TaskCommand> ) taskCommandToTaskMatcher
                .match( taskCommand );
        if ( task == null )
            throw new IllegalArgumentException(
                    "Can't find bean for Task " + taskCommand.getTaskClass().getSimpleName() );
        task.setTaskCommand( taskCommand );
        return task;
    }

    private SubmittedTaskRemote constructSubmittedTaskRemote( TaskCommand taskCommand, String taskId,
            List<String> progressUpdates ) {
        String resultQueueName = Settings.getString( "gemma.remoteTasks.resultQueuePrefix" ) + taskId;
        String statusQueueName = Settings.getString( "gemma.remoteTasks.lifeCycleQueuePrefix" ) + taskId;
        String progressQueueName = Settings.getString( "gemma.remoteTasks.progressUpdatesQueuePrefix" ) + taskId;

        MessageSender<TaskResult> resultSender = new JmsMessageSender<>( jmsHelper, resultQueueName );

        MessageSender<TaskStatusUpdate> statusUpdateSender = new JmsMessageSender<>( jmsHelper, statusQueueName );

        MessageSender<String> progressUpdateReceiver = new JmsMessageSender<>( jmsHelper, progressQueueName );

        return new SubmittedTaskRemote( taskCommand, progressUpdates, resultSender, statusUpdateSender,
                progressUpdateReceiver, taskPostProcessing, executorService );
    }
}
