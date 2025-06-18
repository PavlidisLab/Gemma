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
package ubic.gemma.web.controller.job;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.job.progress.ProgressData;

import java.util.Collection;
import java.util.List;

/**
 * These methods are exposed to front-end via DWR.
 *
 * @author paul
 */
@Controller
@SuppressWarnings("unused") // Possible external use
public interface ProgressStatusController {

    /**
     * Set up an email alert for this job; an email will be sent when it has finished (or failed).
     *
     * @param taskId task id
     */
    void addEmailAlert( String taskId );

    /**
     * Attempt to cancel the job.
     *
     * @param taskId tak id
     * @return true if cancelling was error-free, false otherwise.
     */
    @SuppressWarnings("SameReturnValue")
    // Used in frontend, Can also throw an exception
    boolean cancelJob( String taskId );

    Object checkResult( String taskId ) throws Exception;

    /**
     * Get the latest information about how a job is doing.
     *
     * @param taskId id
     * @return progress data
     */
    List<ProgressData> getProgressStatus( String taskId );

    SubmittedTaskValueObject getSubmittedTask( String taskId );

    @Secured({ "GROUP_ADMIN" })
    Collection<SubmittedTaskValueObject> getSubmittedTasks();

}