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

    // Does not survive serialization.
    private transient TaskLifecycleHandler lifecycleHandler;

    public ExecutingTask( Task<T, ?> task, String taskId ) {
        this.task = task;
        this.taskId = taskId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T call() {
        T result;

        if ( lifecycleHandler == null ) {
            throw new IllegalStateException( "No lifecycle handler has been configured for this executing task." );
        }

        lifecycleHandler.onStart();

        try ( ProgressUpdateAppender.ProgressUpdateContext progressUpdateContext = new ProgressUpdateAppender.ProgressUpdateContext( lifecycleHandler::onProgress ) ) {
            // From here we are running as user who submitted the task.
            result = this.task.call();
        } catch ( Exception e ) {
            // result is an exception
            result = ( T ) new TaskResult( taskId );
            result.setException( e );
        }

        if ( result.getException() == null ) {
            lifecycleHandler.onSuccess();
        } else {
            lifecycleHandler.onFailure( result.getException() );
        }

        lifecycleHandler.onComplete();

        return result;
    }

    public void setLifecycleHandler( TaskLifecycleHandler lifecycleHandler ) {
        this.lifecycleHandler = lifecycleHandler;
    }

    // These hooks are used to update status of the running task.
    public interface TaskLifecycleHandler {

        /**
         * Whenever the task execution begins.
         */
        void onStart();

        /**
         * When progress is made on the task.
         * @param message
         */
        void onProgress( String message );

        /**
         * On failure.
         * @param e
         */
        void onFailure( Exception e );

        /**
         * On successful completion.
         */
        void onSuccess();

        /**
         * On completion, regardless of failure.
         */
        void onComplete();
    }
}