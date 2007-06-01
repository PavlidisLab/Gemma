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

package ubic.gemma.util.progress;

import java.util.List;
import java.util.Queue;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class exposes methods for AJAX calls.
 * 
 * @spring.bean id="progressStatusService"
 * @spring.property name="taskRunningService" ref="taskRunningService"
 * @spring.property name="progressManager" ref="progressManager"
 * @author klc
 * @version $Id$
 */
public class ProgressStatusService {

    private ProgressStatusService() {
    }

    private static Log log = LogFactory.getLog( ProgressStatusService.class.getName() );
    protected static final Log logger = LogFactory.getLog( ProgressStatusService.class );

    private TaskRunningService taskRunningService;
    private ProgressManager progressManager;

    public void setProgressManager( ProgressManager progressManager ) {
        this.progressManager = progressManager;
    }

    /**
     * Get the latest information about how a job is doing.
     * 
     * @param taskId
     * @return
     */
    public List<ProgressData> getProgressStatus( String taskId ) {
        List<ProgressData> result = new Vector<ProgressData>();

        ProgressJob job = progressManager.getJob( taskId );

        if ( job == null ) {
            log.debug( "No job with id " + taskId + " is live." );
            ProgressData progressData = new ProgressData( 100, "No Live Job Found", true,
                    ProgressManager.FORWARD_DEFAULT );
            progressData.setDone( true ); // might have died before first check.
            result.add( progressData );
            return result;
        }

        Queue<ProgressData> pd = job.getProgressData();

        // if progress has finished need to remove observer from session and get make sure observer cleans up after
        // itself.
        while ( !pd.isEmpty() ) {
            ProgressData data = pd.poll();
            result.add( data );
            if ( data.isDone() ) {
                log.debug( "Job is done! forward to " + data.getForwardingURL() );
            }
        }

        return result;
    }

    /**
     * Attempt to cancel the job.
     * 
     * @param taskId
     * @return true if cancelling was error-free, false otherwise.
     */
    public boolean cancelJob( String taskId ) {
        try {
            log.debug( "Got cancellation for " + taskId );
            taskRunningService.cancelTask( taskId, false );
        } catch ( Exception e ) {
            return false;
        }
        return true;
    }

    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

}
