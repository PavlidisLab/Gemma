/* Copyright (c) 2006 University of British Columbia
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

package ubic.gemma.job.progress;

import ubic.gemma.model.common.auditAndSecurity.JobInfo;
import java.util.Queue;

/**
 * All progressJobs must implement the following functionality. ProgressJobs are used by the client to provide hooks for
 * providing feedback to a user for long running processes
 * 
 * @author klc
 * @version $Id$
 */
public interface JobProgress {

    public Queue<ProgressData> getProgressData();

    // Updates the current progress of the job with the desired updated message. Doesn't change anything else.
    public void updateProgress( String message );

    // Updates the progress job by a complete progressData. In case a few things need to be updated
    public void updateProgress( ProgressData pd );

    public void done();
    public void failed( Throwable cause );

    public JobInfo getJobInfo();
    public String getTaskId();
    public String getUser();

    public String getForwardingURL();
    public void setForwardingURL( String forwardingURL );
    public boolean forwardWhenDone();

}