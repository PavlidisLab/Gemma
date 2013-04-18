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
package ubic.gemma.job.executor.webapp;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.infrastructure.common.MessageReceiver;
import ubic.gemma.infrastructure.common.MessageSender;
import ubic.gemma.infrastructure.jms.JMSHelper;
import ubic.gemma.infrastructure.jms.JmsMessageReceiver;
import ubic.gemma.infrastructure.jms.JmsMessageSender;
import ubic.gemma.job.ConflictingTaskException;
import ubic.gemma.job.SubmittedTask;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.common.*;
import ubic.gemma.job.grid.util.JMSBrokerMonitor;
import ubic.gemma.tasks.Task;
import ubic.gemma.util.ConfigUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles the execution of tasks in threads that can be checked by clients later.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class TaskRunningServiceImpl implements TaskRunningService {
    private static final Log log = LogFactory.getLog( TaskRunningServiceImpl.class );

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

    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator( Executors
            .newFixedThreadPool( 20 ) );

    private final Map<String, SubmittedTask> submittedTasks = new ConcurrentHashMap<String, SubmittedTask>();

    @Override
    public Collection<SubmittedTask> getSubmittedTasks() {
        return submittedTasks.values();
    }

    @Override
    public SubmittedTask getSubmittedTask( String taskId ) {
        return submittedTasks.get( taskId );
    }

    @Override
    public String submitLocalJob( BackgroundJob job ) throws ConflictingTaskException {
        checkJob( job );

        TaskCommand taskCommand = job.getCommand();
        checkTaskCommand( taskCommand );

        final String taskId = taskCommand.getTaskId();

        if ( log.isDebugEnabled() ) {
            log.debug( "Submitting local task with id: " + taskId );
        }

        SubmittedTaskLocal submittedTask = new SubmittedTaskLocal( taskCommand, taskPostProcessing );

        ExecutingTask<TaskResult> executingTask = constructExecutingTask( job, taskId, submittedTask );

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

    private ExecutingTask<TaskResult> constructExecutingTask( BackgroundJob job, String taskId,
            final SubmittedTaskLocal submittedTask ) {
        ExecutingTask<TaskResult> executingTask = new ExecutingTask<TaskResult>( job );

        executingTask.setStatusCallback( new ExecutingTask.TaskLifecycleHandler() {
            @Override
            public void onStart() {
                submittedTask.updateStatus( SubmittedTask.Status.RUNNING, new Date() );
            }

            @Override
            public void onFinish() {
                submittedTask.updateStatus( SubmittedTask.Status.COMPLETED, new Date() );
            }

            @Override
            public void onFailure( Throwable e ) {
                log.error( e, e );
                submittedTask.updateStatus( SubmittedTask.Status.FAILED, new Date() );
            }
        } );

        executingTask.setProgressAppender( new LogBasedProgressAppender( taskId, new ProgressUpdateCallback() {
            private final Queue<String> progressUpdates = submittedTask.getProgressUpdates();

            @Override
            public void addProgressUpdate( String message ) {
                progressUpdates.add( message );
            }
        } ) );
        return executingTask;
    }

    private void checkJob( BackgroundJob job ) {
        checkNotNull( job, "Must provide a job." );
    }

    private void checkTaskCommand( TaskCommand taskCommand ) {
        checkNotNull( taskCommand.getTaskId(), "Must have taskId." );
        checkNotNull( taskCommand.getSecurityContext(), "Must have SecurityContext." );
        checkNotNull( taskCommand.getSecurityContext().getAuthentication(), "Must have Authentication." );
    }

    @Override
    public String submitLocalTask( TaskCommand taskCommand ) throws ConflictingTaskException {
        final Task task = taskCommandToTaskMatcher.match( taskCommand );
        // TODO: fix once submitLocalJob is deprecated.
        BackgroundJob<TaskCommand, TaskResult> job = new BackgroundJob<TaskCommand, TaskResult>( taskCommand ) {
            @Override
            protected TaskResult processJob() {
                return task.execute();
            }
        };

        return submitLocalJob( job );
    }

    /**
     * We check if there are listeners on task submission queue to decide if remote tasks can be served.
     */
    // TODO: throw exception if remote worker is unavailable and allow client to submit locally? Or rename the method.
    @Override
    public String submitRemoteTask( final TaskCommand taskCommand ) throws ConflictingTaskException {
        String taskId = taskCommand.getTaskId();
        assert ( taskId != null );

        if ( ConfigUtils.isRemoteTasksEnabled() && jmsBrokerMonitor.canServiceRemoteTasks() ) {
            jmsHelper.sendMessage( taskSubmissionQueue, taskCommand );

            SubmittedTask submittedTask = constructSubmittedTaskProxy( taskCommand, taskId );
            submittedTasks.put( taskId, submittedTask );
        } else {
            this.submitLocalTask( taskCommand );
        }
        return taskId;
    }

    /**
     * @param taskCommand
     * @param taskId
     * @return
     */
    private SubmittedTask constructSubmittedTaskProxy( TaskCommand taskCommand, String taskId ) {
        String resultQueueName = ConfigUtils.getString( "gemma.remoteTasks.resultQueuePrefix" ) + taskId;
        String statusQueueName = ConfigUtils.getString( "gemma.remoteTasks.lifeCycleQueuePrefix" ) + taskId;
        String progressQueueName = ConfigUtils.getString( "gemma.remoteTasks.progressUpdatesQueuePrefix" ) + taskId;

        MessageReceiver<TaskResult> resultReceiver = new JmsMessageReceiver<TaskResult>( jmsHelper, resultQueueName );

        MessageReceiver<TaskStatusUpdate> statusUpdateReceiver = new JmsMessageReceiver<TaskStatusUpdate>( jmsHelper,
                statusQueueName );

        MessageReceiver<String> progressUpdateReceiver = new JmsMessageReceiver<String>( jmsHelper, progressQueueName );

        MessageSender<TaskControl> taskControlSender = new JmsMessageSender<TaskControl>( jmsHelper, taskControlQueue );

        return new SubmittedTaskProxy( taskCommand, resultReceiver, statusUpdateReceiver, progressUpdateReceiver,
                taskControlSender );
    }
}
