/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.job.executor.webapp;

import io.micrometer.core.instrument.binder.MeterBinder;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.tasks.Task;

import java.util.Collection;

/**
 * @author paul, anton
 */
public interface TaskRunningService extends MeterBinder {

    SubmittedTask getSubmittedTask( String taskId );

    /**
     * @return the submittedTasks
     */
    Collection<SubmittedTask> getSubmittedTasks();

    <T extends Task<?>> String submitTask( T task );

    /**
     * Submit a task and track its progress. When it is finished, the results can be retrieved with checkResult(). Tasks
     * can be cancelled with cancelTask().
     *
     * @param taskCommand The command to run. The submissionTime of the task is set after this call. This does not mean
     *                    that the job has started - it might be queued.
     * @param <C>         task command implementation
     * @return string
     */
    <C extends TaskCommand> String submitTaskCommand( C taskCommand );
}