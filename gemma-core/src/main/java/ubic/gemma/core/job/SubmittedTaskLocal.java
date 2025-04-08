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

import ubic.gemma.core.job.notification.EmailNotificationContext;
import ubic.gemma.core.job.notification.TaskPostProcessing;

import java.util.Date;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * SubmittedTask implementation representing the task running on local TaskRunningService.
 */
class SubmittedTaskLocal implements SubmittedTask {

    private final String taskId;
    private final TaskCommand taskCommand;
    // The time at which the job was first submitted to the running queue.
    private final Date submissionTime;
    private final TaskPostProcessing taskPostProcessing;
    private final Deque<String> progressUpdates = new LinkedBlockingDeque<>();
    private final Executor executor;
    private boolean emailAlert;
    // The time the job was actually started.
    private Date startTime;
    // The time at which job completed/failed.
    private Date finishTime;
    private Status status;
    private CompletableFuture<TaskResult> future;

    public SubmittedTaskLocal( TaskCommand taskCommand, TaskPostProcessing taskPostProcessing, Executor executor ) {
        this.taskId = getTaskCommand().getTaskId();
        this.taskCommand = taskCommand;

        // This can be changed by the user AFTER the task was submitted.
        this.emailAlert = getTaskCommand().isEmailAlert();

        this.status = Status.QUEUED;
        this.submissionTime = new Date();
        this.taskPostProcessing = taskPostProcessing;
        this.executor = executor;
    }

    @Override
    public synchronized Queue<String> getProgressUpdates() {
        return this.progressUpdates;
    }

    @Override
    public String getLastProgressUpdates() {
        return this.progressUpdates.peekLast();
    }

    @Override
    public synchronized Date getSubmissionTime() {
        return this.submissionTime;
    }

    @Override
    public synchronized Date getStartTime() {
        return this.startTime;
    }

    @Override
    public synchronized Date getFinishTime() {
        return this.finishTime;
    }

    @Override
    public synchronized Status getStatus() {
        return status;
    }

    @Override
    public TaskResult getResult() throws ExecutionException, InterruptedException {
        // blocking call.
        return this.future.get();
    }

    @Override
    public synchronized void requestCancellation() {
        boolean isCancelled = this.future.cancel( true );
        if ( isCancelled ) {
            status = Status.CANCELLING;
        }
    }

    @Override
    public synchronized void addEmailAlert() {
        if ( emailAlert )
            return;
        emailAlert = true;
        assert taskPostProcessing != null : "Task postprocessing was null";
        taskPostProcessing.addEmailNotification( future,
                new EmailNotificationContext( getTaskCommand().getTaskId(), getTaskCommand().getSubmitter(),
                        getTaskCommand().getTaskClass().getSimpleName() ), executor );
    }

    @Override
    public synchronized boolean isEmailAlert() {
        return this.emailAlert;
    }

    /**
     * @return false
     */
    @Override
    public boolean isRunningRemotely() {
        return false;
    }

    @Override
    public synchronized boolean isDone() {
        return ( this.status.equals( Status.COMPLETED ) || this.status.equals( Status.FAILED ) );
    }

    @SuppressWarnings("unused")
        // Possible external use
    CompletableFuture<TaskResult> getFuture() {
        return future;
    }

    /*
     * Package-private methods, used by TaskRunningService
     */
    void setFuture( CompletableFuture<TaskResult> future ) {
        this.future = future;
    }

    synchronized void updateStatus( Status s, Date timeStamp ) {
        this.setTimeStampAndStatus( s, timeStamp );
    }

    @Override
    public String getTaskId() {
        return this.taskId;
    }

    @Override
    public TaskCommand getTaskCommand() {
        return this.taskCommand;
    }

    void setTimeStampAndStatus( Status status, Date timeStamp ) {
        this.status = status;
        switch ( status ) {
            case RUNNING:
                startTime = timeStamp;
                break;
            case FAILED:
            case COMPLETED:
                finishTime = timeStamp;
                break;
            case CANCELLING:
            case QUEUED:
            case UNKNOWN:
            default:
                break;
        }
    }
}