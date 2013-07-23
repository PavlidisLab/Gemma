/*
 * The gemma project
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
package ubic.gemma.job.executor.worker;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.infrastructure.common.MessageSender;
import ubic.gemma.job.EmailNotificationContext;
import ubic.gemma.job.SubmittedTask;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.executor.common.TaskPostProcessing;
import ubic.gemma.job.executor.common.TaskStatusUpdate;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author anton
 * @version $Id$
 */
public class SubmittedTaskRemote {
    private static Log log = LogFactory.getLog( SubmittedTaskRemote.class );

    private TaskCommand taskCommand;
    private List<String> progressUpdates;  // TODO: keep local copy of task log messages, to be used in email notification.
    private ListenableFuture<TaskResult> future;

    MessageSender<TaskResult> resultSender;
    MessageSender<TaskStatusUpdate> statusUpdateSender;
    MessageSender<String> progressUpdateSender;
    private TaskPostProcessing taskPostProcessing;

    public SubmittedTaskRemote( TaskCommand taskCommand, List<String> progressUpdates,
            MessageSender<TaskResult> resultSender, MessageSender<TaskStatusUpdate> statusUpdateSender,
            MessageSender<String> progressUpdateSender, TaskPostProcessing taskPostProcessing ) {
        this.taskCommand = taskCommand;
        this.progressUpdates = progressUpdates;
        this.resultSender = resultSender;
        this.statusUpdateSender = statusUpdateSender;
        this.progressUpdateSender = progressUpdateSender;
        this.taskPostProcessing = taskPostProcessing;
    }

    public void sendTaskResult() {
        if ( future.isDone() ) {
            try {
                resultSender.send( future.get() );
            } catch ( InterruptedException | ExecutionException e ) {
                log.warn( e.getMessage() );
            }
        }
    }

    public void updateStatus( TaskStatusUpdate statusUpdate ) {
        statusUpdateSender.send( statusUpdate );
    }

    public void addProgressUpdate( String message ) {
        progressUpdateSender.send( message );
    }

    public void requestCancellation() {
        boolean cancelled = future.cancel( true );
        if ( cancelled ) {
            TaskStatusUpdate statusUpdate = new TaskStatusUpdate( SubmittedTask.Status.CANCELLING, new Date() );
            statusUpdateSender.send( statusUpdate );
        } // else we keep the old status
    }

    public void addEmailAlertNotificationAfterCompletion() {
        taskPostProcessing.addEmailNotification( future, new EmailNotificationContext( taskCommand.getTaskId(),
                taskCommand.getSubmitter(), taskCommand.getTaskClass().getSimpleName() ) );
    }

    public void setFuture( ListenableFuture future ) {
        this.future = future;
    }
}
