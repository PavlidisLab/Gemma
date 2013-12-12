/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.common.auditAndSecurity;

/**
 * 
 */
public abstract class JobInfo implements java.io.Serializable, gemma.gsec.model.SecuredNotChild {

    /**
     * Constructs new instances of {@link JobInfo}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link JobInfo}.
         */
        public static JobInfo newInstance() {
            return new JobInfoImpl();
        }

        /**
         * Constructs a new instance of {@link JobInfo}, taking all required and/or read-only properties as arguments.
         */
        public static JobInfo newInstance( Boolean runningStatus, java.util.Date startTime, Integer phases ) {
            final JobInfo entity = new JobInfoImpl();
            entity.setRunningStatus( runningStatus );
            entity.setStartTime( startTime );
            entity.setPhases( phases );
            return entity;
        }

        /**
         * Constructs a new instance of {@link JobInfo}, taking all possible properties (except the identifier(s))as
         * arguments.
         */
        public static JobInfo newInstance( Boolean runningStatus, String failedMessage, java.util.Date startTime,
                java.util.Date endTime, Integer phases, String description, String messages, String taskId, User user ) {
            final JobInfo entity = new JobInfoImpl();
            entity.setRunningStatus( runningStatus );
            entity.setFailedMessage( failedMessage );
            entity.setStartTime( startTime );
            entity.setEndTime( endTime );
            entity.setPhases( phases );
            entity.setDescription( description );
            entity.setMessages( messages );
            entity.setTaskId( taskId );
            entity.setUser( user );
            return entity;
        }
    }

    private Boolean runningStatus = Boolean.valueOf( true );

    private String failedMessage;

    private java.util.Date startTime;

    private java.util.Date endTime;

    private Integer phases = Integer.valueOf( 1 );

    private String description;

    private String messages;

    private String taskId;

    private Long id;

    private User user;

    /**
     * Returns <code>true</code> if the argument is an JobInfo instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof JobInfo ) ) {
            return false;
        }
        final JobInfo that = ( JobInfo ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 
     */
    public java.util.Date getEndTime() {
        return this.endTime;
    }

    /**
     * 
     */
    public String getFailedMessage() {
        return this.failedMessage;
    }

    /**
     * 
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * <p>
     * A field to store all the message progress updates
     * </p>
     */
    public String getMessages() {
        return this.messages;
    }

    /**
     * 
     */
    public Integer getPhases() {
        return this.phases;
    }

    /**
     * 
     */
    public Boolean getRunningStatus() {
        return this.runningStatus;
    }

    /**
     * 
     */
    public java.util.Date getStartTime() {
        return this.startTime;
    }

    /**
     * <p>
     * An ID by which this job's results can be found
     * </p>
     */
    public String getTaskId() {
        return this.taskId;
    }

    /**
     * <p>
     * The user who started the job. Can be left null to indicate job was run by an anonymous user.
     * </p>
     */
    public User getUser() {
        return this.user;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setEndTime( java.util.Date endTime ) {
        this.endTime = endTime;
    }

    public void setFailedMessage( String failedMessage ) {
        this.failedMessage = failedMessage;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setMessages( String messages ) {
        this.messages = messages;
    }

    public void setPhases( Integer phases ) {
        this.phases = phases;
    }

    public void setRunningStatus( Boolean runningStatus ) {
        this.runningStatus = runningStatus;
    }

    public void setStartTime( java.util.Date startTime ) {
        this.startTime = startTime;
    }

    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    public void setUser( User user ) {
        this.user = user;
    }

}