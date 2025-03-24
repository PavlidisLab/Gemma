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
package ubic.gemma.core.job;

import ubic.gemma.core.job.progress.ProgressUpdateContext;

import java.util.concurrent.Callable;

/**
 * Task Lifecycle Hooks ProgressUpdateAppender -
 *
 * @author anton
 */
class ExecutingTask implements Callable<TaskResult> {

    private final Task<?> task;
    private final String taskId;

    // Does not survive serialization.
    private transient TaskLifecycleHandler lifecycleHandler;

    public ExecutingTask( Task<?> task, String taskId ) {
        this.task = task;
        this.taskId = taskId;
    }

    @Override
    public final TaskResult call() {
        TaskResult result;

        if ( lifecycleHandler == null ) {
            throw new IllegalStateException( "No lifecycle handler has been configured for this executing task." );
        }

        lifecycleHandler.onStart();

        try ( ProgressUpdateContext ignored = ProgressUpdateContext.createContext( lifecycleHandler::onProgress ) ) {
            // From here we are running as user who submitted the task.
            result = this.task.call();
        } catch ( Exception e ) {
            // result is an exception
            result = new TaskResult( taskId );
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
         */
        void onProgress( String message );

        /**
         * On failure.
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