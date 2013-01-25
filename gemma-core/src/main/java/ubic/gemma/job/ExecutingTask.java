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

import ubic.gemma.tasks.Task;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.Callable;

public class ExecutingTask<T extends TaskResult> implements Callable<T>, Serializable {

    private Task<T, ?> task;
    private BackgroundJob<?,T> job;

    // Does not survive serialization.
    private transient TaskLifecycleCallback statusCallback;
    private transient ProgressUpdateAppender progressAppender;

    // Written by TaskControlListener thread and read by task thread.
    private volatile boolean emailAlert;
    private Queue<String> localProgressQueue;

    // Pass taskId to progressAppender
    // Must be called before executing the task.
    // If not called then we don't care about progress updates.
    ExecutingTask (Task task) {
        this.task = task;
    }

    ExecutingTask (BackgroundJob job) {
        this.job = job;
    }

    public Queue<String> getLocalProgressQueue() {
        return localProgressQueue;
    }

    public void setLocalProgressQueue( Queue<String> localProgressQueue ) {
        this.localProgressQueue = localProgressQueue;
    }

    // These hooks can be used to setup/tear down progress appender.
    interface ProgressUpdateAppender {
        public void initialize();
        public void tearDown();
    }

    // These hooks can be used to update status of the running task.
    interface TaskLifecycleCallback {
        public void beforeRun();
        public void afterRun();
        public void onFailure( Throwable e );
    }

    void setStatusCallback(TaskLifecycleCallback statusCallback) {
        this.statusCallback = statusCallback;
    }

    void setProgressAppender(ProgressUpdateAppender progressAppender) {
        this.progressAppender = progressAppender;
    }

    @Override
    public final T call() throws Exception {
        statusCallback.beforeRun();
        setup();

        T result;
        try {
            if (this.task != null) {
                result = this.task.execute();
            } else {
                result = this.job.processJob();
            }
        } catch (Exception e) {
            statusCallback.onFailure( e );
            throw e;
        } finally {
            cleanup();
        }

        statusCallback.afterRun();
        return result;
    }

    public void addEmailAlert() {
        this.emailAlert = true;
    }

    public boolean isEmailAlert() {
        return this.emailAlert;
    }

    private void setup() {
        progressAppender.initialize();
    }

    private void cleanup() {
        progressAppender.tearDown();
    }
}