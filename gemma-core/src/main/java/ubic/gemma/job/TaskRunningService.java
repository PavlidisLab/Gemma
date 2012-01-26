/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.job;

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author paul
 * @version $Id$
 */
public interface TaskRunningService extends InitializingBean {

    /**
     * Use this to access the task id in web requests.
     */
    public final static String JOB_ATTRIBUTE = "taskId";
    /**
     * How long we will queue a task before giving up and cancelling it (default value)
     */
    public static final int MAX_QUEUING_MINUTES = 60 * 2;

    /**
     * Tell the system that an email should be sent when the given task is completed (if it hasn't already finished).
     * 
     * @param taskId
     */
    public abstract void addEmailAlert( String taskId );

    /**
     * Signal that a task should be cancelled.
     * 
     * @param taskId
     */
    public abstract boolean cancelTask( String taskId );

    /**
     * Determine if a task is done. If it is done, the results is retrieved. Results can only be retrieved once, after
     * which the service releases them.
     * 
     * @param taskId
     * @return null if the task is still running or was cancelled, or the result object
     */
    public abstract TaskResult checkResult( String taskId ) throws Exception;

    /**
     * @return the cancelledTasks
     */
    public abstract Collection<TaskCommand> getCancelledTasks();

    /**
     * @return the failedTasks
     */
    public abstract Collection<TaskResult> getFailedTasks();

    /**
     * @return the finishedTasks
     */
    public abstract Collection<TaskResult> getFinishedTasks();

    /**
     * @return the submittedTasks
     */
    public abstract Collection<TaskCommand> getSubmittedTasks();

    /**
     * Submit a task and track its progress. When it is finished, the results can be retrieved with checkResult(). Tasks
     * can be cancelled with cancelTask().
     * 
     * @param job The job to run. The submissionTime of the task is set after this call. This does not mean that the job
     *        has started - it might be queued.
     * @throws ConflictingTaskException if the task is disallowed due to another conflicting task (e.g., two tasks of
     *         the same type by the same user).
     */
    public abstract void submitTask( BackgroundJob<? extends TaskCommand> job ) throws ConflictingTaskException;

}