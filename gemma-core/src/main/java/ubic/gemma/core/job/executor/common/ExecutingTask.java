/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.job.executor.common;

import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.Task;

import java.util.concurrent.Callable;

/**
 * Task Lifecycle Hooks ProgressUpdateAppender -
 *
 * @author anton
 */
public class ExecutingTask<T extends TaskResult> implements Callable<T> {

    private final Task<T, ?> task;
    private final String taskId;
    private final TaskCommand taskCommand;
    // Does not survive serialization.
    private transient ProgressUpdateCallback progressUpdateCallback;
    private transient TaskLifecycleHandler statusCallback;

    public ExecutingTask( Task<T, ?> task, TaskCommand taskCommand ) {
        this.task = task;
        this.taskId = taskCommand.getTaskId();
        this.taskCommand = taskCommand;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T call() {
        T result = null;

        if ( progressUpdateCallback != null ) {
            ProgressUpdateAppender.setProgressUpdateCallback( this.taskId, progressUpdateCallback );
        }

        statusCallback.onStart();

        try ( ProgressUpdateAppender.TaskContext taskContext = new ProgressUpdateAppender.TaskContext( this.taskId ) ) {
            // From here we are running as user who submitted the task.
            SecurityContextHolder.setContext( taskCommand.getSecurityContext() );
            result = this.task.execute();
        } catch ( Throwable e ) {
            // result is an exception
            result = ( T ) new TaskResult( taskId );
            result.setException( e );
        } finally {
            // SecurityContext is cleared at this point.
            SecurityContextHolder.clearContext();
        }

        if ( result.getException() == null ) {
            statusCallback.onFinish();
        } else {
            statusCallback.onFailure( result.getException() );
        }

        ProgressUpdateAppender.removeProgressUpdateCallback( this.taskId );

        return result;

    }

    public void setProgressUpdateCallback( ProgressUpdateCallback callback ) {
        this.progressUpdateCallback = callback;
    }

    public void setStatusCallback( TaskLifecycleHandler statusCallback ) {
        this.statusCallback = statusCallback;
    }

    // These hooks are used to update status of the running task.
    public interface TaskLifecycleHandler {
        void onFailure( Throwable e );

        void onFinish();

        void onStart();
    }
}