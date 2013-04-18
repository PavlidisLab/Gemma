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
import ubic.gemma.job.EmailNotificationContext;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.common.TaskPostProcessing;

import java.util.Date;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * SubmittedTask implementation representing the task running on local TaskRunningService.
 */
public class SubmittedTaskLocal<T extends TaskResult> extends SubmittedTaskAbstract<T> {

    private TaskPostProcessing taskPostProcessing;

    private ListenableFuture<T> future;

    private Deque<String> progressUpdates = new LinkedBlockingDeque<String>();

    public SubmittedTaskLocal( TaskCommand taskCommand, TaskPostProcessing taskPostProcessing ) {
        super( taskCommand );
        this.taskPostProcessing = taskPostProcessing;
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
    public synchronized Queue<String> getProgressUpdates() {
        return this.progressUpdates;
    }

    @Override
    public String getLastProgressUpdates() {
        return this.progressUpdates.peekLast();
    }

    @Override
    public T getResult() throws ExecutionException, InterruptedException {
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
    public synchronized Status getStatus() {
        return status;
    }

    /**
     * @return false
     */
    @Override
    public boolean isRunningRemotely() {
        return false;
    }

    @Override
    public synchronized boolean isEmailAlert() {
        return this.emailAlert;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void addEmailAlert() {
        if (emailAlert) return;
        emailAlert = true;
        taskPostProcessing.addEmailNotification( ( ListenableFuture<TaskResult> ) future, new EmailNotificationContext(
                taskCommand.getTaskId(), taskCommand.getSubmitter(), taskCommand.getTaskClass().getSimpleName() ) );
    }

    @Override
    public synchronized boolean isDone() {
        return super.isDone();
    }

    /*
     * Package-private methods, used by TaskRunningService
     */
    void setFuture( ListenableFuture<T> future ) {
        this.future = future;
    }

    ListenableFuture<T> getFuture() {
        return future;
    }

    synchronized void updateStatus( Status s, Date timeStamp ) {
        setTimeStampAndStatus( s, timeStamp );
    }
}