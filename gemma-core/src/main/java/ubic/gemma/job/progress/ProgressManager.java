/* Copyright (c) 2006-2008 University of British Columbia
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

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.model.common.auditAndSecurity.JobInfo;
import ubic.gemma.model.common.auditAndSecurity.JobInfoService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.security.authentication.UserManager;

/**
 * To use, call ProgressManager.createProgressJob(). The returned value can be used by a client object to indicate
 * progress by calling updateProgress on the job that is returned. Observers can register themselves for receiving
 * progress updates by passing themselves into the addToNotification function
 * 
 * @author kelsey
 * @version $Id$
 */
public class ProgressManager {

    private static final String FORWARD_DEFAULT = "/Gemma/checkJobProgress.html";

    private static final Map<String, List<JobProgress>> progressJobsByUserId = new ConcurrentHashMap<String, List<JobProgress>>();
    private static final Map<String, JobProgress> progressJobsByTaskId = new ConcurrentHashMap<String, JobProgress>();

    private static UserManager userManager;
    private static JobInfoService jobInfoService;

    private static final Log log = LogFactory.getLog( ProgressManager.class );

    /**
     * @param command
     * @return Use this static method for creating ProgressJobs
     */
    public static JobProgress createProgressJob( TaskCommand command ) {

        assert command.getTaskId() != null;

        String taskId = command.getTaskId();
        if ( taskId == null ) {
            throw new IllegalArgumentException( "Task id cannot be null" );
        }

        JobInfo jobI = createNewJobInfo(taskId, command.toString());
        if ( command.getPersistJobDetails() ) {
            try {
                jobI = jobInfoService.create( jobI );
            } catch ( Exception e ) {
                log.warn( "Unable to create jobinfo in database: " + e.getMessage() );
            }
        }
//        currentJob.set( taskId );

        String userId = userManager.getCurrentUsername();

        JobProgress jobProgress = new JobProgressImpl( jobI, "" ); // don't log the task name, see bug 1919.

        // keep track of these jobs
        List<JobProgress> usersJobs = progressJobsByUserId.get( userId );
        if ( usersJobs == null ) {
            usersJobs = new Vector<JobProgress>();
            progressJobsByUserId.put(userId, usersJobs);
        }
        usersJobs.add( jobProgress );

        progressJobsByTaskId.put( taskId, jobProgress );

        return jobProgress;
    }

    private static boolean destroyFailedProgressJob(JobProgress jobProgress, boolean doForward, Throwable cause) {
        if ( jobProgress == null ) {
            log.debug( "ProgressManager.destroyProgressJob received a null reference for a jobProgress, hence can't destroy." );
            return false;
        }
        log.debug( "Finishing up failed " + jobProgress);

        String toForwardTo = getForwardingUrl( jobProgress, doForward );

		final String errorMessage;
		if ( cause.getCause() instanceof AccessDeniedException ) {
		    if ( userManager.loggedIn() ) {
		    	errorMessage = "Access is denied.";
		    } else {
		    	errorMessage = "You are not logged in. Please log in to try again.";
		    }
		} else {
		    errorMessage = cause.getMessage();
		}
        
		ProgressData data = new ProgressData( jobProgress.getTaskId(), 100, "Job failed: " + errorMessage, true );
        data.setFailed( true );
        data.setForwardingURL( toForwardTo );
        jobProgress.updateProgress( data );
        jobProgress.failed( cause );

//        currentJob.set( null );
        jobInfoService.update( jobProgress.getJobInfo() );

        return true;
    }

    /**
     * @param taskId
     * @param description
     * @return
     */
    private static JobInfo createNewJobInfo(String taskId, String description) {

        Calendar cal = new GregorianCalendar();
        JobInfo jobI = JobInfo.Factory.newInstance();
        jobI.setRunningStatus( true );
        jobI.setStartTime( cal.getTime() );
        jobI.setDescription( description );
        jobI.setTaskId( taskId );

        try {
            User u = userManager.getCurrentUser();
            jobI.setUser( u );
        } catch ( UsernameNotFoundException e ) {
            // note: getCurrentUser no longer throws an exception.
            log.warn( "No current user, using null for anon user." );
            // JobInfo.user = null just means job was run by anonymous user
            jobI.setUser( null );
        } catch ( AccessDeniedException e ) {
            log.warn( "Access denied" );
        }

        return jobI;
    }

    /**
     * @param jobProgress
     * @param doForward
     * @return
     */
    private static String getForwardingUrl( JobProgress jobProgress, boolean doForward ) {
        String toForwardTo = doForward ? FORWARD_DEFAULT : null;
        if ( doForward && StringUtils.isNotBlank( jobProgress.getForwardingURL() ) ) {
            toForwardTo = jobProgress.getForwardingURL();
        }
        return toForwardTo;
    }
}
