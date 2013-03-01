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
package ubic.gemma.job.executor.common;

import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.Task;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Task Lifecycle Hooks ProgressUpdateAppender -
 * 
 * @author anton
 * @version $Id$
 */
public class ExecutingTask<T extends TaskResult> implements Callable<T>, Serializable {

    private Task<T, ?> task;
    private BackgroundJob<?, T> job;

    // Does not survive serialization.
    private transient TaskLifecycleHandler statusCallback;
    private transient ProgressUpdateAppender progressAppender;

    private String taskId;
    private TaskCommand taskCommand;

    private Throwable taskExecutionException;

    public ExecutingTask( Task<T, ?> task, TaskCommand taskCommand ) {
        this.task = task;
        this.taskId = taskCommand.getTaskId();
        this.taskCommand = taskCommand;
    }

    public ExecutingTask( BackgroundJob job ) {
        this.job = job;
        this.taskCommand = job.getCommand();
        this.taskId = job.getCommand().getTaskId();
    }

    // These hooks can be used to update status of the running task.
    public interface TaskLifecycleHandler {
        public void onStart();

        public void onFinish();

        public void onFailure( Throwable e );
    }

    public void setStatusCallback( TaskLifecycleHandler statusCallback ) {
        this.statusCallback = statusCallback;
    }

    public void setProgressAppender( ProgressUpdateAppender progressAppender ) {
        this.progressAppender = progressAppender;
    }

    @Override
    public final T call() throws Exception {
        setup();
        // From here we are running as user who submitted the task.

        statusCallback.onStart();

        T result = null;
        try {
            if ( this.task != null ) {
                result = this.task.execute();
            } else {
                result = this.job.processJob();
            }
        } catch ( Throwable e ) {
            statusCallback.onFailure( e );
            taskExecutionException = e;
        } finally {
            cleanup();
        }
        // SecurityContext is cleared at this point.

        if ( taskExecutionException == null ) {
            statusCallback.onFinish();
            return result;
        } else {
            result = ( T ) new TaskResult( taskId );
            result.setException( taskExecutionException );
            return result;
        }
    }

    private void setup() {
        progressAppender.initialize();

        SecurityContextHolder.setContext( taskCommand.getSecurityContext() ); // TODO: one idea is to have
                                                                              // SecurityContextAwareExecutorClass.
    }

    private void cleanup() {
        SecurityContextHolder.clearContext();

        progressAppender.tearDown();
    }
}