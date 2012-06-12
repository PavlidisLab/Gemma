/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

import java.util.Collection;
import java.util.HashSet;
import ubic.gemma.job.TaskCommand;

public class TaskCommandValueObject implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6639263336536087102L;

    public static Collection<TaskCommandValueObject> convert2ValueObjects( Collection<TaskCommand> commands ) {

        Collection<TaskCommandValueObject> converted = new HashSet<TaskCommandValueObject>();
        if ( commands == null ) return converted;

        for ( TaskCommand c : commands ) {
            converted.add( new TaskCommandValueObject(c) );
        }

        return converted;
    }

    public TaskCommandValueObject() {
    }

    public TaskCommandValueObject( TaskCommand taskCommand ) {
        this.taskId = taskCommand.getTaskId();
        this.submissionTime = taskCommand.getSubmissionTime();
        this.startTime = taskCommand.getStartTime();
        this.submitter = taskCommand.getSubmitter();
        this.taskInterface = taskCommand.getTaskInterface();
        this.taskMethod = taskCommand.getTaskMethod();
        this.willRunOnGrid = taskCommand.isWillRunOnGrid();
    }

    private String taskId;

    private java.util.Date submissionTime;

    private java.util.Date startTime;

    private String submitter;

    private String taskInterface;

    private String taskMethod;

    private boolean willRunOnGrid;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    public java.util.Date getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime( java.util.Date submissionTime ) {
        this.submissionTime = submissionTime;
    }

    public java.util.Date getStartTime() {
        return startTime;
    }

    public void setStartTime( java.util.Date startTime ) {
        this.startTime = startTime;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter( String submitter ) {
        this.submitter = submitter;
    }

    public String getTaskInterface() {
        return taskInterface;
    }

    public void setTaskInterface( String taskInterface ) {
        this.taskInterface = taskInterface;
    }

    public String getTaskMethod() {
        return taskMethod;
    }

    public void setTaskMethod( String taskMethod ) {
        this.taskMethod = taskMethod;
    }

    public boolean isWillRunOnGrid() {
        return willRunOnGrid;
    }

    public void setWillRunOnGrid( boolean willRunOnGrid ) {
        this.willRunOnGrid = willRunOnGrid;
    }

}