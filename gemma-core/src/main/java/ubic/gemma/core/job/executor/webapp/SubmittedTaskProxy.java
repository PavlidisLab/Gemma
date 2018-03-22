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
package ubic.gemma.core.job.executor.webapp;

import ubic.gemma.core.infrastructure.common.MessageReceiver;
import ubic.gemma.core.infrastructure.common.MessageSender;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.common.TaskControl;
import ubic.gemma.core.job.executor.common.TaskStatusUpdate;

import java.util.Date;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * SubmittedTask implementation representing the task running on remote worker host. The implementation relies on JMS
 * Senders/Receivers to send/receive data from the remote worker application. This proxy is synced with remote state
 * only when client is accessing its methods otherwise the state is stored in messages on the jms queues.
 *
 * @author anton
 */
public class SubmittedTaskProxy<T extends TaskResult> extends SubmittedTaskAbstract<T> {

    // Two separate locks: result and updates shouldn't block each other.
    // Use-case where one thread is waiting on the result while other is trying to check status.
    private final Object resultLock = new Object();
    private final Object lifeCycleStateLock = new Object();
    private final Deque<String> progressUpdates = new LinkedBlockingDeque<>();
    // These are used to get remote state of submitted task.
    private final MessageReceiver<TaskResult> resultReceiver;
    private final MessageReceiver<TaskStatusUpdate> statusUpdateReceiver;
    private final MessageReceiver<String> progressUpdateReceiver;
    // This is used to send 'cancel' or 'add email notification' request.
    private final MessageSender<TaskControl> taskControlSender;
    private TaskResult taskResult;

    public SubmittedTaskProxy( TaskCommand taskCommand, MessageReceiver<TaskResult> resultReceiver,
            MessageReceiver<TaskStatusUpdate> statusUpdateReceiver, MessageReceiver<String> progressUpdateReceiver,
            MessageSender<TaskControl> taskControlSender ) {
        super( taskCommand );

        this.resultReceiver = resultReceiver;
        this.statusUpdateReceiver = statusUpdateReceiver;
        this.progressUpdateReceiver = progressUpdateReceiver;
        this.taskControlSender = taskControlSender;
    }

    @Override
    public Queue<String> getProgressUpdates() {
        synchronized ( lifeCycleStateLock ) {
            this.syncProgressUpdates();
            return progressUpdates;
        }
    }

    @Override
    public String getLastProgressUpdates() {
        return this.progressUpdates.peekLast();
    }

    @Override
    public Date getSubmissionTime() {
        synchronized ( lifeCycleStateLock ) {
            this.syncLifeCycle();
            return this.submissionTime;
        }
    }

    @Override
    public Date getStartTime() {
        synchronized ( lifeCycleStateLock ) {
            this.syncLifeCycle();
            return this.startTime;
        }
    }

    @Override
    public Date getFinishTime() {
        synchronized ( lifeCycleStateLock ) {
            this.syncLifeCycle();
            return this.finishTime;
        }
    }

    @Override
    public Status getStatus() {
        synchronized ( lifeCycleStateLock ) {
            this.syncLifeCycle();
            return this.status;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getResult() {
        synchronized ( resultLock ) {
            if ( taskResult == null ) {
                this.syncResult(); // blocks until result is available.
            }
            return ( T ) taskResult;
        }
    }

    @Override
    public void requestCancellation() {
        taskControlSender.send( new TaskControl( taskId, TaskControl.Request.CANCEL ) );
    }

    @Override
    public void addEmailAlert() {
        synchronized ( lifeCycleStateLock ) {
            if ( emailAlert )
                return; // Trying to prevent multiple email notifications being added.
            emailAlert = true;
            taskControlSender.send( new TaskControl( taskId, TaskControl.Request.ADD_EMAIL_NOTIFICATION ) );
        }
    }

    @Override
    public boolean isEmailAlert() {
        synchronized ( lifeCycleStateLock ) {
            return emailAlert;
        }
    }

    /**
     * Since this a submitted task proxy it always is running remotely.
     *
     * @return true
     */
    @Override
    public boolean isRunningRemotely() {
        return true;
    }

    @Override
    public boolean isDone() {
        synchronized ( lifeCycleStateLock ) {
            this.syncLifeCycle();
            return super.isDone();
        }
    }

    // This blocks until result is received.
    private void syncResult() {
        this.taskResult = resultReceiver.blockingReceive();
    }

    private void syncProgressUpdates() {
        String progressMessage;
        while ( ( progressMessage = progressUpdateReceiver.receive() ) != null ) {
            this.progressUpdates.add( progressMessage );
        }
    }

    private void syncLifeCycle() {
        TaskStatusUpdate statusUpdate;
        while ( ( statusUpdate = statusUpdateReceiver.receive() ) != null ) {
            this.applyStatusUpdate( statusUpdate );
        }
    }

    private void applyStatusUpdate( TaskStatusUpdate statusUpdate ) {
        this.setTimeStampAndStatus( statusUpdate.getStatus(), statusUpdate.getStatusChangeTime() );
    }
}