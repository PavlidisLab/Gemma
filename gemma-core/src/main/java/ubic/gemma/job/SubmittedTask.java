/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

/**
 * Interface representing handler on a task submitted to TaskRunningService.
 * It provides access to task status, task progress updates and task result.
 * It allows to request task cancellation, add email notification of task completion.
 *
 * @param <T> TaskResult (or subclass) representing type of result of submitted task.
 */
public interface SubmittedTask <T extends TaskResult> {

    static enum Status { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLING, UNKNOWN }

    public String getTaskId();
    public TaskCommand getTaskCommand();

    /**
     * Returns queue of log statements from the running task.
     * TODO: Support multiple clients accessing this.
     * @return Queue of log statements from the running task.
     */
    public Queue<String> getProgressUpdates();

    public String getLastProgressUpdates();

    /**
     * @return timestamp of task submission to TaskRunningService.
     */
    public Date getSubmissionTime();

    /**
     * @return time when task started executing.
     */
    public Date getStartTime();

    /**
     * @return time when tasks completed or failed.
     */
    public Date getFinishTime();

    /**
     * @return current status of the task
     */
    public Status getStatus();

    /**
     * @return true if task has completed or failed
     */
    boolean isDone();

    /**
     * Get the result of the task. The call blocks until the result is retrieved.
     *
     * @return result of produced by the task.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public T getResult() throws ExecutionException, InterruptedException;

    /**
     * Send cancellation request. It's not guaranteed that this will cancel the task
     * but it will request task cancellation.
     * Client can check task's status at some later point to verify that task got cancelled.
     */
    public void requestCancellation();

    /**
     * Add email notification of task completion. User who submitted the task will receive an email.
     */
    public void addEmailAlert();

    /**
     * @return true if email notification of task completion will be sent to the user.
     */
    public boolean isEmailAlert();

    /**
     * @return true if the task is running remotely (inside another JVM)
     */
    public boolean isRunningRemotely();
}