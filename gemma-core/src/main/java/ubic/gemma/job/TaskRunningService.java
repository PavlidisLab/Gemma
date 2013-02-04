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

/**
 *
 *
 *
 *
 * @author paul
 * @version $Id$
 */
public interface TaskRunningService {

    /**
     * How long we will queue a task before giving up and cancelling it (default value)
     */
    public static final int MAX_QUEUING_MINUTES = 60 * 2;

    /**
     * @return the submittedTasks
     */
    public Collection<SubmittedTask> getSubmittedTasks(); //TODO: Make this user specific

    /**
     *
     *
     * @param taskId
     * @return
     */
    public SubmittedTask getSubmittedTask(String taskId);

    /**
     * Submit a task and track its progress. When it is finished, the results can be retrieved with checkResult(). Tasks
     * can be cancelled with cancelTask().
     * 
     * @param taskCommand The command to run. The submissionTime of the task is set after this call. This does not mean that the job
     *        has started - it might be queued.
     * @throws ConflictingTaskException if the task is disallowed due to another conflicting task (e.g., two tasks of
     *         the same type by the same user).
     */
    public String submitLocalTask( TaskCommand taskCommand ) throws ConflictingTaskException;

    /**
     *
     *
     * @param taskCommand
     * @return
     * @throws ConflictingTaskException
     */
    public String submitRemoteTask( TaskCommand taskCommand ) throws ConflictingTaskException;

    public String submitLocalJob( BackgroundJob job ) throws ConflictingTaskException;

    /**
     * Attempt to cancel task
     *
     * @param task
     */
    void cancelTask( SubmittedTask task );
}