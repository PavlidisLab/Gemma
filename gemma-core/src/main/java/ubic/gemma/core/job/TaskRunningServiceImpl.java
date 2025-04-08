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
package ubic.gemma.core.job;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.core.job.notification.TaskPostProcessing;
import ubic.gemma.core.metrics.binder.ThreadPoolExecutorMetrics;
import ubic.gemma.core.util.SimpleThreadFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Handles the execution of tasks in threads that can be checked by clients later.
 *
 * @author pavlidis
 */
// Valid, inspection is not parsing the context file for some reason
@Component("taskRunningService")
@ParametersAreNonnullByDefault
public class TaskRunningServiceImpl implements TaskRunningService, InitializingBean, DisposableBean {
    private static final Log log = LogFactory.getLog( TaskRunningServiceImpl.class );

    @Autowired
    private TaskCommandToTaskMatcher taskCommandToTaskMatcher;
    @Autowired
    private TaskPostProcessing taskPostProcessing;

    @Value("${gemma.backgroundTasks.numberOfThreads}")
    private int numberOfThreads;

    private ExecutorService executorService;
    private final Map<String, SubmittedTask> submittedTasks = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        executorService = Executors.newFixedThreadPool( numberOfThreads, new SimpleThreadFactory( "gemma-background-tasks-thread-" ) );
    }

    @Override
    public void destroy() throws Exception {
        executorService.shutdown();
        if ( !executorService.isTerminated() ) {
            log.warn( "There are still running tasks, will wait at most 5 minutes before shutting them down." );
        }
        if ( !executorService.awaitTermination( 5, TimeUnit.MINUTES ) ) {
            log.info( "TaskRunningService executor was still running after 5 minutes, interrupting pending tasks..." );
            executorService.shutdownNow();
        }
    }

    @Override
    public SubmittedTask getSubmittedTask( String taskId ) {
        return submittedTasks.get( taskId );
    }

    @Override
    public Collection<SubmittedTask> getSubmittedTasks() {
        return submittedTasks.values();
    }

    @Override
    public <T extends Task<?>> String submitTask( T task ) {
        this.checkTask( task );

        TaskCommand taskCommand = task.getTaskCommand();
        this.checkTaskCommand( taskCommand );

        final String taskId = task.getTaskCommand().getTaskId();
        assert ( taskId != null );

        if ( TaskRunningServiceImpl.log.isDebugEnabled() ) {
            TaskRunningServiceImpl.log.debug( "Submitting local task with id: " + taskId );
        }

        final SubmittedTaskLocal submittedTask = new SubmittedTaskLocal( task.getTaskCommand(), taskPostProcessing, executorService );

        final ExecutingTask executingTask = new ExecutingTask( task );

        executingTask.setLifecycleHandler( new TaskLifecycleHandler() {
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
            public void onStart() {
                submittedTask.updateStatus( SubmittedTask.Status.RUNNING, new Date() );
            }

            @Override
            public void onProgress( String message ) {
                submittedTask.getProgressUpdates().add( message );
            }
        } );

        Callable<TaskResult> callable = new DelegatingSecurityContextCallable<>( executingTask, taskCommand.getSecurityContext() );
        CompletableFuture<TaskResult> future = CompletableFuture.supplyAsync( () -> {
            try {
                return callable.call();
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }
        }, executorService );
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
        final Task<C> task = taskCommandToTaskMatcher.match( taskCommand );
        return this.submitTask( task );
    }

    private void checkTask( Task<?> task ) {
        Assert.notNull( task, "Must provide a task." );
    }

    private void checkTaskCommand( TaskCommand taskCommand ) {
        Assert.notNull( taskCommand.getTaskId(), "Must have taskId." );
    }

    @Override
    public void bindTo( MeterRegistry meterRegistry ) {
        if ( executorService instanceof ThreadPoolExecutor ) {
            new ThreadPoolExecutorMetrics( ( ThreadPoolExecutor ) executorService, "gemmaBackgroundTasks" )
                    .bindTo( meterRegistry );
        } else {
            log.warn( "The background task executor is not a ThreadPoolExecutor, cannot bind metrics." );
        }
    }
}
