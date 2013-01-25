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
import java.util.Date;
import java.util.HashSet;
import ubic.gemma.job.TaskCommand;

public class TaskCommandValueObject implements java.io.Serializable {
    private static final long serialVersionUID = 6639263336536087102L;

    private String taskId;
    private String submitter;
    private String taskClass;

    public static Collection<TaskCommandValueObject> convert2ValueObjects( Collection<TaskCommand> commands ) {

        Collection<TaskCommandValueObject> converted = new HashSet<TaskCommandValueObject>();
        if ( commands == null ) return converted;

        for ( TaskCommand command : commands ) {
            converted.add( new TaskCommandValueObject( command ) );
        }

        return converted;
    }

    public TaskCommandValueObject() {
    }

    public TaskCommandValueObject( TaskCommand taskCommand ) {
        this.taskId = taskCommand.getTaskId();
        this.submitter = taskCommand.getSubmitter();
        this.taskClass = taskCommand.getTaskClass() == null ? "Not specified" : taskCommand.getTaskClass().getSimpleName();

    }

    public String getTaskClass() {
        return taskClass;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getSubmitter() {
        return submitter;
    }

}