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

package ubic.gemma.core.job.executor.common;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;

/**
 * Implements a long-running task in its own thread. Implementations simply implement processJob.
 * Deprecated: Use Task/AbstractTask instead.
 *
 * @author klc
 */
@Deprecated
@SuppressWarnings("unused") // Possible external use
public abstract class BackgroundJob<T extends TaskCommand, R extends TaskResult> {

    private T command;
    private String taskId;

    public BackgroundJob( T commandObj ) {
        assert commandObj != null;

        this.taskId = commandObj.getTaskId();
        assert this.taskId != null;

        this.command = commandObj;
    }

    public T getCommand() {
        return this.command;
    }

    /**
     * @return the taskId
     */
    public String getTaskId() {
        return this.taskId;
    }

    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    /**
     * Implement to do the work of the job. This will be run in a separate thread.
     *
     * @return result
     */
    protected abstract R processJob();

}
