/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.io.Serializable;
import java.util.Date;

/**
 * This class describes the result of long-running task.
 * 
 * @author keshav
 * @version $Id$
 */
public class TaskResult implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * The answer
     */
    private Object answer = null; // result
    private boolean cancelled = false;
    /**
     * Set if failed.
     */
    private Exception exception = null;

    private boolean failed = false;

    /**
     * Time when the job is finished, failed or cancelled.
     */
    private Date finishTime;

    /**
     * True if it was run using the grid.
     */
    private Boolean ranInSpace = true;

    private boolean sendEmailIfNotRetrieved = false;

    /**
     * The system time the job was actually started.
     */
    private Date startTime = null;

    /**
     * The time at which the job was first submitted to the running queue.
     */
    private Date submissionTime = null;

    private String submitter;

    private String taskInterface;
    private String taskMethod;

    /**
     * The task id
     */
    private String taskID;

    public TaskResult( TaskCommand command, Object answer ) {
        assert command != null;
        assert command.getTaskId() != null;
        this.taskID = command.getTaskId();
        this.answer = answer;
        this.submissionTime = command.getSubmissionTime();
        this.startTime = command.getStartTime(); // usually not filled in!
        this.finishTime = new Date();
        this.submitter = command.getSubmitter();
        this.taskInterface = command.getTaskInterface();
        this.taskMethod = command.getTaskMethod();
    }

    /**
     * @return the taskInterface
     */
    public String getTaskInterface() {
        return taskInterface;
    }

    /**
     * @return the taskMethod
     */
    public String getTaskMethod() {
        return taskMethod;
    }

    public Object getAnswer() {
        return answer;
    }

    /**
     * @return the exception
     */
    public Exception getException() {
        return exception;
    }

    public Date getFinishTime() {
        return this.finishTime;
    }

    public Boolean getRanInSpace() {
        return ranInSpace;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * @return the submissionTime
     */
    public Date getSubmissionTime() {
        return submissionTime;
    }

    /**
     * @return the submitter
     */
    public String getSubmitter() {
        return submitter;
    }

    public String getTaskID() {
        return taskID;
    }

    /**
     * @return the cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * @return the failed
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * @return the sendEmailIfNotRetrieved
     */
    public boolean isSendEmailIfNotRetrieved() {
        return sendEmailIfNotRetrieved;
    }

    /**
     * @param cancelled the cancelled to set
     */
    public void setCancelled( boolean cancelled ) {
        this.cancelled = cancelled;
    }

    /**
     * @param exception the exception to set
     */
    public void setException( Exception exception ) {
        this.exception = exception;
    }

    /**
     * @param failed the failed to set
     */
    public void setFailed( boolean failed ) {
        this.failed = failed;
    }

    public void setRanInSpace( Boolean ranInSpace ) {
        this.ranInSpace = ranInSpace;
    }

    /**
     * @param sendEmailIfNotRetrieved the sendEmailIfNotRetrieved to set
     */
    public void setSendEmailIfNotRetrieved( boolean sendEmailIfNotRetrieved ) {
        this.sendEmailIfNotRetrieved = sendEmailIfNotRetrieved;
    }

    public void setStartTime( Date time ) {
        this.startTime = time;

    }
}
