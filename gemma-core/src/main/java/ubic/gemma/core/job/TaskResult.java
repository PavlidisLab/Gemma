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
package ubic.gemma.core.job;

import java.io.Serializable;

/**
 * This class describes the result of long-running task. Like a Future, constructed at the time of task completion.
 * 
 * @author keshav
 *
 */
public class TaskResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The actual result object
     */
    private Object answer;

    /**
     * Set if failed.
     */
    private Exception exception;

    /**
     * The task id
     */
    private String taskID;

    public TaskResult( String taskId ) {
        assert taskId != null;
        this.taskID = taskId;
    }

    public TaskResult( TaskCommand command, Object answer ) {
        assert command != null;
        assert command.getTaskId() != null;
        this.taskID = command.getTaskId();
        this.answer = answer;
    }

    public Object getAnswer() {
        return answer;
    }

    /**
     * @return the exception
     */
    public Throwable getException() {
        return exception;
    }

    public String getTaskId() {
        return taskID;
    }

    /**
     * @param exception the exception to set
     */
    public void setException( Exception exception ) {
        this.exception = exception;
    }

}
