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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This command class is used to allow communication of parameters for a task between a client and task running service,
 * which might be on a different computer.
 * <p>
 * This class can be used directly, or extended to create a command object to pass parameters for a specific task. See
 * for example {@link ExpressionExperimentLoadTaskCommand}. A entityId field is provided as a convenience for the case
 * when a primary key is all that is really needed.
 * 
 * @author keshav
 * @version $Id$
 */
public class TaskCommand implements Serializable {

    /**
     * How long we will wait for a started task before giving up waiting for it. Tasks running longer than this will be
     * cancelled. This does not include time spent queued.
     */
    public static final int MAX_RUNTIME_MINUTES = 60;

    private static final long serialVersionUID = 1L;

    /**
     * Set to false to force this job to run on the grid (or to not run at all). Default = true
     */
    private Boolean allowedToRunInProcess = true;

    /**
     * Should an email be sent to the user when the job is done?
     */
    private boolean emailAlert = false;

    /**
     * Convenience field to handle the common case where a primary key is all that is needed.
     */
    private Long entityId = null;

    /**
     * How long we will allow this task to be queued before giving up.
     */
    private Integer maxQueueMinutes = TaskRunningService.MAX_QUEUING_MINUTES;

    private int maxRuntime = MAX_RUNTIME_MINUTES;

    /**
     * Used during task tracking to determine if a task is in trouble.
     */
    private boolean mayHaveFailed = false;

    /**
     * If true, the jobDetails associated with this task will be persisted in the database. Consider setting to false
     * for test jobs or other super-frequent maintenance tasks.
     */
    private Boolean persistJobDetails = true;

    /**
     * Used to propagate security to grid workers.
     */
    private SecurityContext securityContext;

    /**
     * The system time the job was actually started.
     */
    private Date startTime = null;

    /**
     * The time at which the job was first submitted to the running queue.
     */
    private Date submissionTime = null;

    private String submitter;

    private String taskId = null;

    /**
     * Name of the task interface this task is firing against.
     */
    private String taskInterface;

    /**
     * Name of the method from the taskInterface.
     */
    private String taskMethod;

    /**
     * If this task is going to run on the grid (that is, that the grid is apparently available and/or the job is
     * actually running on the grid)
     */
    private boolean willRunOnGrid = false;

    /**
     * The taskId is assigned on creation.
     */
    public TaskCommand() {
        this.taskId = TaskUtils.generateTaskId();

        /*
         * security details.
         */
        SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;
        this.securityContext = context;
        Authentication authentication = context.getAuthentication();
        assert authentication != null;
        this.submitter = authentication.getName();

    }

    /**
     * Convenience constructor for case where all the job needs to know is the id.
     * 
     * @param entityId
     */
    public TaskCommand( Long entityId ) {
        this();
        this.entityId = entityId;
    }

    public Long getEntityId() {
        return entityId;
    }

    /**
     * @return
     */
    public Integer getMaxQueueMinutes() {
        return maxQueueMinutes;
    }

    /**
     * @return the maxRuntime in minutes
     */
    public int getMaxRuntime() {
        return maxRuntime;
    }

    /**
     * @return the persistJobDetails
     */
    public Boolean getPersistJobDetails() {
        return persistJobDetails;
    }

    public SecurityContext getSecurityContext() {
        return this.securityContext;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * @return the submissionTime - when the job was submitted to the job queue. It may not have actually been started
     *         immediately.
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

    /**
     * @return
     */
    public String getTaskId() {
        return taskId;
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

    /**
     * @return the allowedToRunInProcess
     */
    public Boolean isAllowedToRunInProcess() {
        return allowedToRunInProcess;
    }

    public boolean isEmailAlert() {
        return emailAlert;
    }

    public boolean isMayHaveFailed() {
        return mayHaveFailed;
    }

    public boolean isWillRunOnGrid() {
        return willRunOnGrid;
    }

    /**
     * Set to false to force this job to run on the grid (or to not run at all). Default = true
     * 
     * @param allowedToRunInProcess
     */
    public void setAllowedToRunInProcess( Boolean allowedToRunInProcess ) {
        this.allowedToRunInProcess = allowedToRunInProcess;
    }

    public void setEmailAlert( boolean emailAlert ) {
        this.emailAlert = emailAlert;
    }

    public void setEntityId( Long entityId ) {
        this.entityId = entityId;
    }

    /**
     * How long we will allow this task to be queued before giving up. Default = TaskRunningService.MAX_QUEUING_MINUTES
     * 
     * @param maxQueueMinutes
     * @see ubic.gemma.job.TaskRunningService.MAX_QUEUING_MINUTES
     */
    public void setMaxQueueMinutes( Integer maxQueueMinutes ) {
        this.maxQueueMinutes = maxQueueMinutes;
    }

    /**
     * @param maxRuntime the maxRuntime to set (in minutes) before we bail. Default is MAX_RUNTIME_MINUTES
     */
    public void setMaxRuntime( int maxRuntime ) {
        this.maxRuntime = maxRuntime;
    }

    public void setMayHaveFailed( boolean mayHaveFailed ) {
        this.mayHaveFailed = mayHaveFailed;
    }

    /**
     * @param persistJobDetails the persistJobDetails to set
     */
    public void setPersistJobDetails( Boolean persistJobDetails ) {
        this.persistJobDetails = persistJobDetails;
    }

    /**
     * Sets the start time to <em>Now</em>.
     */
    public void setStartTime() {
        this.startTime = new Date();
    }

    /**
     * Used to set the start time when we only find it out later.
     * 
     * @param date
     */
    public void setStartTime( Date date ) {
        this.startTime = date;
    }

    /**
     * Sets the submission time to <em>Now</em>.
     */
    public void setSubmissionTime() {
        this.submissionTime = new Date();
    }

    /**
     * @param taskId
     */
    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    /**
     * @param taskInterface the taskInterface to set
     */
    public void setTaskInterface( String taskInterface ) {
        this.taskInterface = taskInterface;
    }

    /**
     * @param taskMethod the taskMethod to set
     */
    public void setTaskMethod( String taskMethod ) {
        this.taskMethod = taskMethod;
    }

    /**
     * @param willRunOnGrid
     * @see willRunOnGrid
     */
    public void setWillRunOnGrid( boolean willRunOnGrid ) {
        this.willRunOnGrid = willRunOnGrid;
    }

}
