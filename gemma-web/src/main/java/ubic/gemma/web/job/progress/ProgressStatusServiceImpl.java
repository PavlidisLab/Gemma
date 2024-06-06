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
package ubic.gemma.web.job.progress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.job.progress.ProgressData;
import ubic.gemma.core.job.progress.SubmittedTaskValueObject;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

/**
 * This class exposes methods for AJAX calls.
 *
 * @author klc
 */
@Component
public class ProgressStatusServiceImpl implements ProgressStatusService {

    private static final Log log = LogFactory.getLog( ProgressStatusServiceImpl.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;

    @Override
    public synchronized void addEmailAlert( String taskId ) {
        if ( taskId == null )
            throw new IllegalArgumentException( "task id cannot be null" );
        taskRunningService.getSubmittedTask( taskId ).addEmailAlert();
    }

    @Override
    public boolean cancelJob( String taskId ) {
        if ( taskId == null )
            throw new IllegalArgumentException( "task id cannot be null" );

        SubmittedTask submittedTask = taskRunningService.getSubmittedTask( taskId );
        if ( submittedTask == null )
            throw new IllegalArgumentException( "Couldn't find task with task id: " + taskId );

        submittedTask.requestCancellation();
        // we can't really say if it is cancelled or not, since task can be running remotely. Client should check
        return true;
    }

    @Override
    public Object checkResult( String taskId ) throws Exception {
        SubmittedTask submittedTask = taskRunningService.getSubmittedTask( taskId );
        if ( submittedTask == null )
            return null;

        TaskResult result = submittedTask.getResult();

        if ( result == null )
            return null;

        Object answer = result.getAnswer();

        if ( answer instanceof ModelAndView ) {
            View view = ( ( ModelAndView ) answer ).getView();
            if ( view instanceof RedirectView ) {
                return ( ( RedirectView ) view ).getUrl();
            }
            return null;
        }

        if ( answer instanceof Exception ) {
            throw ( Exception ) answer;
        }

        return answer;
    }

    @Override
    public synchronized List<ProgressData> getProgressStatus( String taskId ) {
        if ( taskId == null )
            throw new IllegalArgumentException( "task id cannot be null" );
        SubmittedTask task = taskRunningService.getSubmittedTask( taskId );

        List<ProgressData> statusObjects = new Vector<>();

        if ( task == null ) {
            ProgressStatusServiceImpl.log.warn( "It looks like job " + taskId
                    + " has gone missing; assuming it is dead or finished already" );

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
        StringBuilder progressMessage = new StringBuilder();
        while ( !updates.isEmpty() ) {
            String update = updates.poll();
            progressMessage.append( update ).append( "\n" );
        }

        if ( task.isDone() ) {
            ProgressData data;
            switch ( task.getStatus() ) {
                case COMPLETED:
                    ProgressStatusServiceImpl.log.debug( "Job " + taskId + " is done" );
                    data = new ProgressData( taskId, 1, progressMessage + "Done!", true );
                    break;
                case FAILED:
                    data = new ProgressData( taskId, 1, progressMessage + "Failed!", true );
                    data.setFailed( true );
                    break;
                default:
                    data = new ProgressData( taskId, 1, progressMessage + "Possibly canceled.", true );
                    break;
            }
            statusObjects.add( data );
        } else {
            statusObjects.add( new ProgressData( taskId, 1, progressMessage.toString(), false ) );
        }

        return statusObjects;
    }

    @Override
    public SubmittedTaskValueObject getSubmittedTask( String taskId ) {
        SubmittedTask task = taskRunningService.getSubmittedTask( taskId );
        if ( task == null )
            return null;
        return new SubmittedTaskValueObject( task );
    }

    @Override
    public Collection<SubmittedTaskValueObject> getSubmittedTasks() {
        return SubmittedTaskValueObject.convert2ValueObjects( taskRunningService.getSubmittedTasks() );
    }

}
