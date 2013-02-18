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
package ubic.gemma.job.executor.worker;

import ubic.gemma.job.TaskCommand;

/**
 *  TODO: document me
 */
public interface RemoteTaskRunningService {

    /**
     * Submit task represented by TaskCommand for execution. The task may be queued for some time since the
     * number of concurrently running tasks is limited.
     * @param taskCommand
     */
    public void submit ( TaskCommand taskCommand );

    /**
     *
     * @param taskId
     * @return
     */
    public SubmittedTaskRemote getSubmittedTask( String taskId );

    /**
     * Attempts to stop all actively executing tasks and halts the processing of waiting tasks.
     * There are no guarantees beyond best-effort attempts to stop processing actively executing tasks.
     * Cancellation is via Thread.interrupt(), so any task that fails to respond to interrupts may never terminate.
     */
    void shutdown();
}

