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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

//TODO: synchronization! many threads access this

/**
 * Returned to client by local task running service.
 * cancel, getResult, isCanceled, isDone are forwarder to underlying Future
 *
 */
public class SubmittedTaskLocal<T extends TaskResult> implements SubmittedTask<T> {

    TaskRunningService taskRunningService;

    // We have to make sure only one thread writes this field.
    private volatile Status status = Status.QUEUED;

    private Queue<String> progressUpdates;
    private ListenableFuture<T> future;
    private TaskCommand taskCommand;

    private volatile boolean emailAlert;

    /**
     * The time at which the job was first submitted to the running queue.
     * The time the job was actually started.
     * The time at which job completed/failed.
     */
    private volatile Date submissionTime;
    private volatile Date startTime;
    private volatile Date finishTime;
    // TODO: one idea is to associate these to Status transitions in a more general way.

    public SubmittedTaskLocal( TaskCommand taskCommand, Queue<String> progressUpdates, TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
        this.taskCommand = taskCommand;
        this.progressUpdates = progressUpdates;
        setSubmissionTime( new Date() );

        // This can be changed by the user AFTER task was submitted. I'm trying to make TaskCommand immutable.
        this.emailAlert = taskCommand.isEmailAlert();
    }

    @Override
    public String getTaskId() {
        return this.taskCommand.getTaskId();
    }

    @Override
    public Date getSubmissionTime() {
        return this.submissionTime;
    }

    @Override
    public Date getStartTime() {
        return this.startTime;
    }

    @Override
    public Date getFinishTime() {
        return this.finishTime;
    }

    @Override
    public Queue<String> getProgressUpdates() {
        return this.progressUpdates;
    }

    /**
     * Blocks.
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public T getResult() throws ExecutionException, InterruptedException {
        return this.future.get();
    }

    @Override
    public void cancel() {
        boolean isCancelled = this.future.cancel (true);
        if (isCancelled) {
            status = Status.CANCELLED;
            finishTime = new Date();
        }
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public TaskCommand getCommand() {
        return this.taskCommand;
    }

    /**
     * This instance always refers to local task.
     *
     * @return false
     */
    @Override
    public boolean isRunningRemotely() {
        return false;
    }

    @Override
    public boolean isEmailAlert() {
        return this.emailAlert;
    }

    @Override
    public void addEmailAlert() {
        if ( emailAlert == true ) return;
        emailAlert = true;
        taskRunningService.addEmailNotificationFutureCallback( (ListenableFuture<TaskResult>) future );
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }

    void setFuture(ListenableFuture<T> future) {
        this.future = future;
    }

    void setStatus(Status status) {
        this.status = status;
    }

    public void setSubmissionTime(Date submissionTime) {
        this.submissionTime = submissionTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

}