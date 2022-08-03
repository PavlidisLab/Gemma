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
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.common.ExecutingTask;
import ubic.gemma.core.job.executor.common.TaskCommandToTaskMatcher;
import ubic.gemma.core.job.executor.common.TaskPostProcessing;
import ubic.gemma.core.tasks.Task;

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

    @Override
    public SubmittedTask getSubmittedTask( String taskId ) {
        return submittedTasks.get( taskId );
    }

    @Override
    public Collection<SubmittedTask<? extends TaskResult>> getSubmittedTasks() {
        return submittedTasks.values();
    }

    @Override
    public <T extends Task> String submitTask( T task ) {
        this.checkTask( task );

        TaskCommand taskCommand = task.getTaskCommand();
        this.checkTaskCommand( taskCommand );

        final String taskId = task.getTaskCommand().getTaskId();
        assert ( taskId != null );

        if ( TaskRunningServiceImpl.log.isDebugEnabled() ) {
            TaskRunningServiceImpl.log.debug( "Submitting local task with id: " + taskId );
        }

        final SubmittedTaskLocal submittedTask = new SubmittedTaskLocal( task.getTaskCommand(), taskPostProcessing, executorService );

        final ExecutingTask<TaskResult> executingTask = new ExecutingTask<TaskResult>( task, taskCommand.getTaskId() );

        executingTask.setLifecycleHandler( new ExecutingTask.TaskLifecycleHandler() {
            @Override
            public void onFailure( Exception e ) {
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

        ListenableFuture<TaskResult> future = executorService.submit( new DelegatingSecurityContextCallable<>( executingTask, taskCommand.getSecurityContext() ) );
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
    public <C extends TaskCommand> String submitTaskCommand( final C taskCommand ) {
        this.checkTaskCommand( taskCommand );
        final Task<? extends TaskResult, ? extends TaskCommand> task = taskCommandToTaskMatcher.match( taskCommand );
        return this.submitTask( task );
    }

    private void checkTask( Task task ) {
        checkNotNull( task, "Must provide a task." );
    }

    private void checkTaskCommand( TaskCommand taskCommand ) {
        checkNotNull( taskCommand.getTaskId(), "Must have taskId." );
    }
}
