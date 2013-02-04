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
package ubic.gemma.job.progress;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * These methods are exposed to front-end.
 *
 * @author paul
 * @version $Id$
 */
//TODO: rename it controller or AJAXService or something else. We need some clear way to mark things as exposed to
//TODO: to the front end
//TODO: Also, there background task related methods should be consolidated in one class. see TaskCompletionController
@Component
public interface ProgressStatusService {

    /**
     * Set up an email alert for this job; an email will be sent when it has finished (or failed).
     * 
     * @param taskId
     */
    public void addEmailAlert( String taskId );

    /**
     * Attempt to cancel the job.
     * 
     * @param taskId
     * @return true if cancelling was error-free, false otherwise.
     */
    public boolean cancelJob( String taskId );

    /**
     * Get the latest information about how a job is doing.
     * 
     * @param taskId
     * @return
     */
    public List<ProgressData> getProgressStatus( String taskId );

    /**
     * @return
     * @see ubic.gemma.job.TaskRunningServiceImpl#getSubmittedTasks()
     */
    @Secured({ "GROUP_ADMIN" })
    public Collection<SubmittedTaskValueObject> getSubmittedTasks();


    public Object checkResult (String taskId) throws Exception;

}