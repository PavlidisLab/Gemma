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
import ubic.gemma.job.SubmittedTask;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.TaskRunningService;
import ubic.gemma.job.TaskCommandValueObject;

/**
 * This class exposes methods for AJAX calls.
 * 
 * @author klc
 * @version $Id$
 */
@Component
public class ProgressStatusServiceImpl implements ProgressStatusService {

    private static Log log = LogFactory.getLog( ProgressStatusServiceImpl.class.getName() );

    @Autowired private TaskRunningService taskRunningService;

    @Override
    public synchronized void addEmailAlert( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );
        taskRunningService.getSubmittedTask( taskId ).addEmailAlert();
    }

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

    @Override
    public synchronized List<ProgressData> getProgressStatus( String taskId ) {
        if ( taskId == null ) throw new IllegalArgumentException( "task id cannot be null" );
        SubmittedTask task = taskRunningService.getSubmittedTask( taskId );

        List<ProgressData> statusObjects = new Vector<ProgressData>();

        if ( task == null ) {
            log.warn( "It looks like job " + taskId + " has gone missing; assuming it is dead or finished already" );

            // We should assume it is dead.
            ProgressData data = new ProgressData();
            data.setTaskId( taskId );
            data.setDone( true );
            data.setDescription( "The job has gone missing; it has already finished or failed." );
            statusObjects.add( data );

            return statusObjects;
        }

        assert task.getTaskId() != null;
        assert task.getTaskId().equals( taskId );

        Queue<String> updates = task.getProgressUpdates();
        String progressMessage = "";
        while ( !updates.isEmpty() ) {
            String update = updates.poll();
            progressMessage += update +"\n";
        }

        if (task.isDone()) {
            ProgressData data;
            if ( task.getStatus() == SubmittedTask.Status.DONE ) {
                log.debug( "Job " + taskId + " is done" );
                data = new ProgressData( taskId, 1, progressMessage+"Done!", true );
            } else if ( task.getStatus() == SubmittedTask.Status.FAILED ) {
                data = new ProgressData( taskId, 1, progressMessage+"Failed!", true );
                data.setFailed( true );
            } else {
                data = new ProgressData( taskId, 1, progressMessage+"Possibly canceled.", true );
            }
            statusObjects.add( data );
        } else {
            statusObjects.add( new ProgressData( taskId, 1, progressMessage, false ) );
        }

        return statusObjects;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.job.progress.ProgressStatusService#getSubmittedTasks()
     */
    @Override
    public Collection<SubmittedTaskValueObject> getSubmittedTasks() {
        return SubmittedTaskValueObject.convert2ValueObjects( taskRunningService.getSubmittedTasks());
    }

}
