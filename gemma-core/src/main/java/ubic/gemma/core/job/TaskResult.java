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

import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * This class describes the result of long-running task. Like a Future, constructed at the time of task completion.
 *
 * @author keshav
 *
 */
public final class TaskResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The task id
     */
    private final String taskId;

    /**
     * The actual result object
     */
    @Nullable
    private final Serializable answer;

    /**
     * Set if failed.
     */
    private final Exception exception;

    public TaskResult( String taskId, @Nullable Serializable answer ) {
        Assert.notNull( taskId );
        this.taskId = taskId;
        this.answer = answer;
        this.exception = null;
    }

    public TaskResult( String taskId, Exception exception ) {
        Assert.notNull( taskId );
        Assert.notNull( exception );
        this.taskId = taskId;
        this.answer = null;
        this.exception = exception;
    }

    public String getTaskId() {
        return taskId;
    }

    @Nullable
    public Serializable getAnswer() {
        return answer;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }
}
