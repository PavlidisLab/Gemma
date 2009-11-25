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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.grid.javaspaces.util.SpacesUtil;

/**
 * This class exposes methods for AJAX calls.
 * 
 * @author klc
 * @version $Id$
 */
@Service
public class ProgressStatusService {

    private ProgressStatusService() {
    }

    private static Log log = LogFactory.getLog( ProgressStatusService.class.getName() );
    protected static final Log logger = LogFactory.getLog( ProgressStatusService.class );

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ProgressManager progressManager;
    @Autowired
    private SpacesUtil spacesUtil;

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
        List<ProgressData> statusObjects = new Vector<ProgressData>();

        ProgressJob job = progressManager.getJob( taskId );

        if ( job == null ) {
            // Lost job? Or we might just need to wait a little while for the job to register.
            try {
                Thread.sleep( 1000 );
                log.warn( "It looks like job " + taskId + " has gone missing; assuming it is dead or finished already" );
                job = progressManager.getJob( taskId );
                if ( job == null ) {
                    // We should assume it is dead.
                    ProgressData data = new ProgressData();
                    data.setTaskId( taskId );
                    data.setFailed( true );
                    data.setDescription( "The job has already finished or failed but results are unavailable" );
                    statusObjects.add( data );
                }
            } catch ( InterruptedException e ) {
                  log.error (e);
            }
            return statusObjects;
        }

        // normal situation: deal with accumulated results.
        Queue<ProgressData> pd = job.getProgressData();

        boolean isDone = false;
        synchronized ( pd ) {
            boolean didCleanup = false;
            while ( !pd.isEmpty() ) {
                ProgressData data = pd.poll();
                statusObjects.add( data );

                if ( !didCleanup && ( isDone || data.isDone() ) ) {
                    log.info( "Job " + taskId + " is done!" );
                    if ( data.getForwardingURL() != null ) {
                        log.debug( "forward to " + data.getForwardingURL() );
                    }
                    progressManager.cleanupJob( taskId );
                    isDone = true;
                    didCleanup = true;
                    // Do not break, even if hte job is done. keep adding any stored data to the results.
                }
            }
        }

        return statusObjects;
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
            spacesUtil.cancel( taskId );
            taskRunningService.cancelTask( taskId );
            progressManager.cleanupJob( taskId );
        } catch ( Exception e ) {
            log.error( e, e );
            return false;
        }
        return true;
    }

    public void setTaskRunningService( TaskRunningService taskRunningService ) {
        this.taskRunningService = taskRunningService;
    }

    public void setSpacesUtil( SpacesUtil spacesUtil ) {
        this.spacesUtil = spacesUtil;
    }

}
