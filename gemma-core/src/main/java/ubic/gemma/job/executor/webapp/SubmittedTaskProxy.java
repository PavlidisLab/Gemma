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

import ubic.gemma.infrastructure.common.MessageReceiver;
import ubic.gemma.infrastructure.common.MessageSender;
import ubic.gemma.job.SubmittedTask;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.common.TaskControl;
import ubic.gemma.job.executor.common.TaskStatusUpdate;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * SubmittedTask implementation representing the task running on remote worker host.
 * The implementation relies on JMS Senders/Receivers to send/receive data from the remote worker application.
 *
 * This proxy is synced with remote state only when client is accessing its methods
 * otherwise the state is stored in messages on the jms queues.
 *
 */
public class SubmittedTaskProxy<T extends TaskResult> extends SubmittedTaskAbstract<T> implements SubmittedTask<T> {

    private Queue<String> progressUpdates = new ConcurrentLinkedQueue<String>();
    private TaskResult taskResult;

    // These are used to get remote state of submitted task.
    private MessageReceiver<TaskResult> resultReceiver;
    private MessageReceiver<TaskStatusUpdate> statusUpdateReceiver;
    private MessageReceiver<String> progressUpdateReceiver;
    // This is used to send 'cancel' or 'add email notification' request.
    private MessageSender<TaskControl> taskControlSender;

    // Two separate locks: result and updates shouldn't block each other.
    // Use-case where one thread is waiting on the result while other is trying to check status.
    private final Object resultLock = new Object();
    private final Object lifeCycleStateLock = new Object();

    public SubmittedTaskProxy( TaskCommand taskCommand,
                               MessageReceiver<TaskResult> resultReceiver,
                               MessageReceiver<TaskStatusUpdate> statusUpdateReceiver,
                               MessageReceiver<String> progressUpdateReceiver,
                               MessageSender<TaskControl> taskControlSender ) {
        super( taskCommand );

        this.resultReceiver = resultReceiver;
        this.statusUpdateReceiver = statusUpdateReceiver;
        this.progressUpdateReceiver = progressUpdateReceiver;
        this.taskControlSender = taskControlSender;
    }

    @Override
    public Queue<String> getProgressUpdates() {
        synchronized (lifeCycleStateLock) {
            syncProgressUpdates();
            return progressUpdates;
        }
    }

    @Override
    public boolean isDone() {
        synchronized (lifeCycleStateLock) {
            syncLifeCycle();
            return super.isDone();
        }
    }

    @Override
    public Date getStartTime() {
        synchronized (lifeCycleStateLock) {
            syncLifeCycle();
            return this.startTime;
        }
    }

    @Override
    public Date getSubmissionTime() {
        synchronized (lifeCycleStateLock) {
            syncLifeCycle();
            return this.submissionTime;
        }
    }

    @Override
    public Date getFinishTime() {
        synchronized (lifeCycleStateLock) {
            syncLifeCycle();
            return this.finishTime;
        }
    }

    //TODO: Can task return null? It shouldn't. Should return TaskResult with getAnswer null. BUT DOUBLE CHECK
    @Override
    public T getResult() throws ExecutionException, InterruptedException {
        synchronized (resultLock) {
            if (taskResult == null) {
                syncResult(); // blocks until result is available.
            }
            return (T) taskResult;
        }
    }

    //TODO: Support case where TaskCommand has not yet reached RemoteTaskRunningService i.e. it's still in JMS queue.
    @Override
    public void requestCancellation() {
        taskControlSender.send( new TaskControl( taskId, TaskControl.Request.CANCEL ) );
    }

    @Override
    public Status getStatus() {
        synchronized (lifeCycleStateLock) {
            syncLifeCycle();
            return this.status;
        }
    }

    /**
     * Since this a submitted task proxy it always is running remotely.
     * @return true
     */
    @Override
    public boolean isRunningRemotely() {
        return true;
    }

    @Override
    public boolean isEmailAlert() {
        synchronized (lifeCycleStateLock) {
            return emailAlert;
        }
    }

    @Override
    public void addEmailAlert() {
        synchronized (lifeCycleStateLock) {
            if ( emailAlert ) return; // Trying to prevent multiple email notifications being added.
            emailAlert = true;
            taskControlSender.send( new TaskControl( taskId, TaskControl.Request.ADD_EMAIL_NOTIFICATION ) );
        }
    }

    // This blocks until result is received.
    private void syncResult() {
        this.taskResult = resultReceiver.blockingReceive();
    }

    private void syncProgressUpdates() {
        String progressMessage;
        while ( (progressMessage = progressUpdateReceiver.receive()) != null ) {
            this.progressUpdates.add( progressMessage );
        }
    }

    private void syncLifeCycle() {
        TaskStatusUpdate statusUpdate;
        while ((statusUpdate = statusUpdateReceiver.receive()) != null) {
            applyStatusUpdate( statusUpdate );
        }
    }

    private void applyStatusUpdate( TaskStatusUpdate statusUpdate ) {
        setTimeStampAndStatus( statusUpdate.getStatus(), statusUpdate.getStatusChangeTime() );
    }
}