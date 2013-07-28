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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;

/**
 * This command class is used to allow communication of parameters for a task between a client and task running service,
 * which might be on a different computer.
 * <p>
 * This class can be used directly, or extended to create a command object to pass parameters for a specific task. See
 * for example {@link ExpressionExperimentLoadTaskCommand}. A entityId field is provided as a convenience for the case
 * when a primary key is all that is really needed. TODO: Make sure it is immutable. TODO: Rename to TaskContext.
 * 
 * @author keshav
 * @version $Id$
 */
public class TaskCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    // For now, this how we map from TaskCommand to Task that actually runs it.
    // We have to have this mapping somewhere until we make Tasks themselves serializable. Tasks are not readily
    // serializable because they have dependencies to spring services.
    // at which point TaskCommand can be deprecated(or remain as TaskContext).
    public Class<?> getTaskClass() {
        return null;
    }

    /**
     * Should an email be sent to the user when the job is done?
     */
    private boolean emailAlert = false;

    /**
     * Convenience field to handle the common case where a primary key is all that is needed.
     */
    private Long entityId = null;

    /**
     * If true, the jobDetails associated with this task will be persisted in the database. Consider setting to false
     * for test jobs or other super-frequent maintenance tasks.
     */
    private Boolean persistJobDetails = true;

    /**
     * Used to propagate security to grid workers.
     */
    private SecurityContext securityContext;
    private String submitter;

    private String taskId;

    // How long we will wait for a started task before giving up waiting for it. Tasks running longer than this will be
    // cancelled. This does not include time spent queued.
    public static final int MAX_RUNTIME_MINUTES = 60;

    /**
     * How long we will queue a task before giving up and cancelling it (default value)
     */
    public static final int MAX_QUEUING_MINUTES = 60 * 2;

    /**
     * How long we will allow this task to be queued before giving up.
     */
    private Integer maxQueueMinutes = MAX_QUEUING_MINUTES;
    private int maxRuntime = MAX_RUNTIME_MINUTES;

    /**
     * For tasks that use too much resources and must be run remotely.
     */
    protected boolean remoteOnly;

    public TaskCommand() {
        // The taskId is assigned on creation.
        this.taskId = TaskUtils.generateTaskId();

        // security details.
        SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;
        this.securityContext = context;

        Authentication authentication = context.getAuthentication();
        // can happen in test situations.
        if ( authentication != null ) this.submitter = authentication.getName();
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
     * @return the persistJobDetails
     */
    public Boolean getPersistJobDetails() {
        return persistJobDetails;
    }

    public SecurityContext getSecurityContext() {
        return this.securityContext;
    }

    public boolean isEmailAlert() {
        return emailAlert;
    }

    /**
     * @return the submitter
     */
    public String getSubmitter() {
        return submitter;
    }

    /**
     * @param taskId
     */
    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    public Integer getMaxQueueMinutes() {
        return maxQueueMinutes;
    }

    /**
     * @return the maxRuntime in minutes
     */
    public int getMaxRuntime() {
        return maxRuntime;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public void setEmailAlert( boolean emailAlert ) {
        this.emailAlert = emailAlert;
    }

    public void setEntityId( Long entityId ) {
        this.entityId = entityId;
    }

    /**
     * @param persistJobDetails the persistJobDetails to set
     */
    public void setPersistJobDetails( Boolean persistJobDetails ) {
        this.persistJobDetails = persistJobDetails;
    }

    /**
     * How long we will allow this task to be queued before giving up. Default = TaskRunningService.MAX_QUEUING_MINUTES
     * 
     * @param maxQueueMinutes
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

    public boolean isRemoteOnly() {
        return remoteOnly;
    }

    public void setRemoteOnly( boolean remoteOnly ) {
        this.remoteOnly = remoteOnly;
    }
}
