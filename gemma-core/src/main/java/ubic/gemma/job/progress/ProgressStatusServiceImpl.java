/* Copyright (c) 2006-2010 University of British Columbia
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

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.TaskRunningService;

/**
 * This class exposes methods for AJAX calls.
 * 
 * @author klc
 * @version $Id$
 */
@Component
public class ProgressStatusServiceImpl implements ProgressStatusService {

    private static Log log = LogFactory.getLog( ProgressStatusServiceImpl.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#addEmailAlert(java.lang.String)
     */
    @Override
    public synchronized void addEmailAlert( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );
        taskRunningService.addEmailAlert( taskId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#cancelJob(java.lang.String)
     */
    @Override
    public boolean cancelJob( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );

        try {
            return taskRunningService.cancelTask( taskId );
        } catch ( Exception e ) {
            log.error( e, e );
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#getCancelledTasks()
     */
    @Override
    public Collection<TaskCommand> getCancelledTasks() {
        return taskRunningService.getCancelledTasks();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#getFailedTasks()
     */
    @Override
    public Collection<TaskResult> getFailedTasks() {
        return taskRunningService.getFailedTasks();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#getFinishedTasks()
     */
    @Override
    public Collection<TaskResult> getFinishedTasks() {
        return taskRunningService.getFinishedTasks();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#getProgressStatus(java.lang.String)
     */
    @Override
    public synchronized List<ProgressData> getProgressStatus( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );

        List<ProgressData> statusObjects = new Vector<ProgressData>();

        ProgressJob job = ProgressManager.getJob( taskId );

        if ( job == null ) {

            log.warn( "It looks like job " + taskId + " has gone missing; assuming it is dead or finished already" );

            // We should assume it is dead.
            ProgressData data = new ProgressData();
            data.setTaskId( taskId );
            data.setDone( true );
            data.setDescription( "The job has gone missing; it has already finished or failed." );
            statusObjects.add( data );

            return statusObjects;
        }

        // normal situation: deal with accumulated results.
        Queue<ProgressData> pd = job.getProgressData();

        boolean didCleanup = false;
        while ( !pd.isEmpty() ) {
            ProgressData data = pd.poll();

            assert data.getTaskId() != null;
            assert data.getTaskId().equals( taskId );

            statusObjects.add( data );

            if ( !didCleanup && data.isDone() ) {
                log.debug( "Job " + taskId + " is done" );
                if ( data.getForwardingURL() != null ) {
                    log.debug( "forward to " + data.getForwardingURL() );
                }
                ProgressManager.cleanupJob( taskId );
                didCleanup = true;
                // Do not break, even if the job is done. keep adding any stored data to the results.
            }
        }

        return statusObjects;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#getSubmittedTasks()
     */
    @Override
    public Collection<TaskCommand> getSubmittedTasks() {
        return taskRunningService.getSubmittedTasks();
    }

}
