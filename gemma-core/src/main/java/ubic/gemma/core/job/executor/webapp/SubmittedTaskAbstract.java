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
package ubic.gemma.core.job.executor.webapp;

import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;

import java.util.Date;

/**
 * Shared code between SubmittedTaskLocal and SubmittedTaskProxy. author: anton date: 08/02/13
 */
public abstract class SubmittedTaskAbstract<T extends TaskResult> implements SubmittedTask<T> {

    final String taskId;
    final TaskCommand taskCommand;
    // The time at which the job was first submitted to the running queue.
    final Date submissionTime;
    boolean emailAlert;
    // The time the job was actually started.
    Date startTime;
    // The time at which job completed/failed.
    Date finishTime;

    Status status;

    SubmittedTaskAbstract( TaskCommand taskCommand ) {
        this.taskId = taskCommand.getTaskId();
        this.taskCommand = taskCommand;

        // This can be changed by the user AFTER the task was submitted.
        this.emailAlert = taskCommand.isEmailAlert();

        this.status = Status.QUEUED;
        this.submissionTime = new Date();
    }

    @Override
    public String getTaskId() {
        return this.taskId;
    }

    @Override
    public TaskCommand getTaskCommand() {
        return this.taskCommand;
    }

    @Override
    public boolean isDone() {
        return ( this.status.equals( Status.COMPLETED ) || this.status.equals( Status.FAILED ) );
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
