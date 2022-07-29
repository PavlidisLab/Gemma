/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.job.executor.webapp;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.infrastructure.common.MessageReceiver;
import ubic.gemma.core.infrastructure.common.MessageSender;
import ubic.gemma.core.infrastructure.jms.JMSHelper;
import ubic.gemma.core.infrastructure.jms.JmsMessageReceiver;
import ubic.gemma.core.infrastructure.jms.JmsMessageSender;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.common.*;
import ubic.gemma.core.job.grid.util.JMSBrokerMonitor;
import ubic.gemma.core.tasks.Task;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles the execution of tasks in threads that can be checked by clients later.
 *
 * @author pavlidis
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
// Valid, inspection is not parsing the context file for some reason
@Component
public class TaskRunningServiceImpl implements TaskRunningService {
    private static final Log log = LogFactory.getLog( TaskRunningServiceImpl.class );
    private final ListeningExecutorService executorService = MoreExecutors
            .listeningDecorator( Executors.newFixedThreadPool( 20 ) );
    private final Map<String, SubmittedTask<? extends TaskResult>> submittedTasks = new ConcurrentHashMap<>();
    @Autowired
    private TaskCommandToTaskMatcher taskCommandToTaskMatcher;
    @Autowired
    private TaskPostProcessing taskPostProcessing;
    @Autowired
    private JMSBrokerMonitor jmsBrokerMonitor;
    @Autowired
    private JMSHelper jmsHelper;
    @Resource(name = "taskSubmissionQueue")
    private javax.jms.Queue taskSubmissionQueue;
    @Resource(name = "taskControlQueue")
    private javax.jms.Queue taskControlQueue;

    @Override
    public SubmittedTask getSubmittedTask( String taskId ) {
        return submittedTasks.get( taskId );
    }

    @Override
    public Collection<SubmittedTask<? extends TaskResult>> getSubmittedTasks() {
        return submittedTasks.values();
    }

    @Override
    public <C extends TaskCommand> String submitLocalTask( C taskCommand ) {
        this.checkTaskCommand( taskCommand );

        final Task<? extends TaskResult, ? extends TaskCommand> task = taskCommandToTaskMatcher.match( taskCommand );

        return this.submitLocalTask( task );
    }

    @Override
    public <T extends Task> String submitLocalTask( T task ) {
        this.checkTask( task );

        TaskCommand taskCommand = task.getTaskCommand();
        this.checkTaskCommand( taskCommand );

        final String taskId = task.getTaskCommand().getTaskId();

        if ( TaskRunningServiceImpl.log.isDebugEnabled() ) {
            TaskRunningServiceImpl.log.debug( "Submitting local task with id: " + taskId );
        }

        final SubmittedTaskLocal submittedTask = new SubmittedTaskLocal( task.getTaskCommand(), taskPostProcessing, executorService );

        final ExecutingTask<TaskResult> executingTask = new ExecutingTask<TaskResult>( task, taskCommand );

        executingTask.setLifecycleHandler( new ExecutingTask.TaskLifecycleHandler() {
            @Override
            public void onFailure( Throwable e ) {
                TaskRunningServiceImpl.log.error( e, e );
                submittedTask.updateStatus( SubmittedTask.Status.FAILED, new Date() );
            }

            @Override
            public void onSuccess() {
                submittedTask.updateStatus( SubmittedTask.Status.COMPLETED, new Date() );
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onStart() {
                submittedTask.updateStatus( SubmittedTask.Status.RUNNING, new Date() );
            }

            @Override
            public void onProgress( String message ) {
                submittedTask.getProgressUpdates().add( message );
            }
        } );

        ListenableFuture<TaskResult> future = executorService.submit( executingTask );
        submittedTask.setFuture( future );

        // Adding post-processing steps, they will run on future completion.
        // Currently we have only email notification.
        if ( taskCommand.isEmailAlert() ) {
            submittedTask.addEmailAlert();
        }

        submittedTasks.put( taskId, submittedTask );
        return taskId;
    }

    /**
     * We check if there are listeners on task submission queue to decide if remote tasks can be served.
     */
    @Override
    public <C extends TaskCommand> String submitRemoteTask( final C taskCommand ) {
        String taskId = taskCommand.getTaskId();
        assert ( taskId != null );

        if ( Settings.isRemoteTasksEnabled() && jmsBrokerMonitor.canServiceRemoteTasks() ) {
            jmsHelper.sendMessage( taskSubmissionQueue, taskCommand );

            SubmittedTask<TaskResult> submittedTask = this.constructSubmittedTaskProxy( taskCommand, taskId );
            submittedTasks.put( taskId, submittedTask );
        } else {
            if ( taskCommand.isRemoteOnly() ) {
                throw new IllegalStateException( "Can't run 'remote-only' task locally." );
            }
            this.submitLocalTask( taskCommand );
        }
        return taskId;
    }

    private void checkTask( Task task ) {
        checkNotNull( task, "Must provide a task." );
    }

    private void checkTaskCommand( TaskCommand taskCommand ) {
        checkNotNull( taskCommand.getTaskId(), "Must have taskId." );
    }

    private SubmittedTask<TaskResult> constructSubmittedTaskProxy( TaskCommand taskCommand, String taskId ) {
        String resultQueueName = Settings.getString( "gemma.remoteTasks.resultQueuePrefix" ) + taskId;
        String statusQueueName = Settings.getString( "gemma.remoteTasks.lifeCycleQueuePrefix" ) + taskId;
        String progressQueueName = Settings.getString( "gemma.remoteTasks.progressUpdatesQueuePrefix" ) + taskId;

        MessageReceiver<TaskResult> resultReceiver = new JmsMessageReceiver<>( jmsHelper, resultQueueName );

        MessageReceiver<TaskStatusUpdate> statusUpdateReceiver = new JmsMessageReceiver<>( jmsHelper, statusQueueName );

        MessageReceiver<String> progressUpdateReceiver = new JmsMessageReceiver<>( jmsHelper, progressQueueName );

        MessageSender<TaskControl> taskControlSender = new JmsMessageSender<>( jmsHelper, taskControlQueue );

        return new SubmittedTaskProxy( taskCommand, resultReceiver, statusUpdateReceiver, progressUpdateReceiver,
                taskControlSender );
    }
}
