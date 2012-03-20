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
import java.util.Iterator;
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
 * Singleton for creating observers for monitoring long-running processes.
 * <p>
 * To use, call ProgressManager.createProgressJob(). The returned value can be used by a client object to indicate
 * progress by calling updateProgress on the job that is returned. Observers can register themselves for receiving
 * progress updates by passing themselves into the addToNotification function
 * 
 * @author kelsey
 * @version $Id$
 */
public class ProgressManager {

    public static final String FORWARD_DEFAULT = "/Gemma/checkJobProgress.html";

    /**
     * This is a thread local variable for storing the job id that a given thread is working on. Affects the creation of
     * jobs.
     */
    protected static InheritableThreadLocal<Object> currentJob = new InheritableThreadLocal<Object>();

    protected static final Log log = LogFactory.getLog( ProgressManager.class );

    private static JobInfoService jobInfoService;

    /*
     * Jobs by user.
     */
    private static Map<String, List<ProgressJob>> progressJobs = new ConcurrentHashMap<String, List<ProgressJob>>();

    private static Map<String, ProgressJob> progressJobsByTaskId = new ConcurrentHashMap<String, ProgressJob>();

    private static UserManager userManager;

    /**
     * @param command
     * @return Use this static method for creating ProgressJobs
     */
    public static ProgressJob createProgressJob( TaskCommand command ) {

        assert command.getTaskId() != null;

        ProgressJob newJob = null;
        String taskId = command.getTaskId();

        if ( taskId == null ) {
            throw new IllegalArgumentException( "Task id cannot be null" );
        }

        String userId = userManager.getCurrentUsername();

        if ( !progressJobs.containsKey( userId ) ) {
            log.debug( "Creating new progress job(s) with key " + userId );
            progressJobs.put( userId, new Vector<ProgressJob>() );
        } else {
            log.debug( "Already have job with key " + userId );
        }

        JobInfo jobI = createnewJobInfo( taskId, command.toString() );

        if ( command.getPersistJobDetails() ) {
            try {
                jobI = jobInfoService.create( jobI );
            } catch ( Exception e ) {
                log.warn( "Unable to create jobinfo in database: " + e.getMessage() );
            }
        }

        newJob = new ProgressJobImpl( jobI, "" ); // don't log the task name, see bug 1919.
        currentJob.set( taskId );

        // keep track of these jobs
        Collection<ProgressJob> usersJobs = progressJobs.get( userId );
        usersJobs.add( newJob ); // adds to the progressJobs collection
        progressJobsByTaskId.put( taskId, newJob );

        ProgressManager.dump();

        return newJob;
    }

    /**
     * @param progressJob
     * @param doForward
     * @param cause
     * @return
     */
    public static boolean destroyFailedProgressJob( ProgressJob progressJob, boolean doForward, Throwable cause ) {
        if ( progressJob == null ) {
            log
                    .debug( "ProgressManager.destroyProgressJob received a null reference for a progressJob, hence can't destroy." );
            return false;
        }
        log.debug( "Finishing up failed " + progressJob );

        String toForwardTo = getForwardingUrl( progressJob, doForward );

        ProgressData data = new ProgressData( progressJob.getTaskId(), 100, "Job failed: " + cause.getMessage(), true );
        data.setFailed( true );
        data.setForwardingURL( toForwardTo );
        progressJob.updateProgress( data );
        progressJob.failed( cause );

        currentJob.set( null );
        jobInfoService.update( progressJob.getJobInfo() );

        return true;
    }

    /**
     * @param progressJob
     * @return
     */
    public static boolean destroyProgressJob( ProgressJob progressJob ) {
        return destroyProgressJob( progressJob, false );
    }

    /**
     * Removes ProgressJob from notification lists and provides general clean up. Also causes job to be persisted to db.
     * This method should only be called once per thread. Easiest to put this call in the controller that needs to
     * provide user with progress information
     * 
     * @param ajob
     */
    public static boolean destroyProgressJob( ProgressJob progressJob, boolean doForward ) {

        if ( progressJob == null ) {
            log
                    .debug( "ProgressManager.destroyProgressJob received a null reference for a progressJob, hence can't destroy." );
            return false;
        }
        log.debug( "Destroying " + progressJob );

        String toForwardTo = getForwardingUrl( progressJob, doForward );
        ProgressData progressData = new ProgressData( progressJob.getTaskId(), 100, "", true );
        progressData.setForwardingURL( toForwardTo );
        progressJob.updateProgress( progressData );
        progressJob.done();

        currentJob.set( null );
        if ( progressJob.getJobInfo().getId() != null ) {
            /*
             * In test situations we might have a persistent entry.
             */
            try {
                jobInfoService.update( progressJob.getJobInfo() );
            } catch ( RuntimeException e ) {
                log.warn( "Could not update jobinfo :" + e, e );
            }
        }
        return true;
    }

    // As the progress manager is a singleton leaks and strange behavior are likely.
    // i made this to get a peek at what was going on under the hood at runtime.
    public static void dump() {

        if ( !log.isDebugEnabled() ) return;

        log.debug( "Dump ProgressMangagers State:" );

        log.debug( "Thread Local variable: " + currentJob.get() );

        log.debug( "ProgressJobs Dump:  " );
        for ( Iterator<String> iter = progressJobs.keySet().iterator(); iter.hasNext(); ) {
            String name = iter.next();
            log.debug( "name: " + name );

            for ( Iterator<ProgressJob> values = progressJobs.get( name ).iterator(); values.hasNext(); ) {
                ProgressJob job = values.next();
                log.debug( "====> progressJob: " + job.getTaskId() );
            }
        }

        log.debug( "ProgressJobsById Dump:  " );
    }

    /**
     * @param taskId
     * @param string
     */
    public static void setForwardingURL( String taskId, String string ) {
        ProgressJob job = progressJobsByTaskId.get( taskId );
        if ( job != null ) job.setForwardingURL( string );
    }

    /**
     * @param jobInfoService the jobInfoService to set
     */
    public void setJobInfoService( JobInfoService j ) {
        jobInfoService = j;
    }

    /**
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager u ) {
        userManager = u;
    }

    /**
     * @param key
     */
    public static void signalCancelled( Object key ) {
        log.debug( key + " Cancelled" );
        ProgressJob job = progressJobsByTaskId.get( key );
        if ( job != null ) job.getJobInfo().setFailedMessage( "Cancellation was signalled by user" );
        destroyProgressJob( job, false ); // never forward.
    }

    /**
     * @param key
     */
    public static void signalDone( Object key ) {
        log.debug( key + " Done!" );
        ProgressJob job = progressJobsByTaskId.get( key );
        assert job != null : "No job of id " + key;
        destroyProgressJob( job, job.forwardWhenDone() );
    }

    /**
     * @param key
     * @param cause
     */
    public static void signalFailed( Object key, Throwable cause ) {
        log.error( key + " Failed: " + cause.getMessage() );
        ProgressJob job = progressJobsByTaskId.get( key );
        if ( job != null ) {
            job.getJobInfo().setFailedMessage( cause.getMessage() );
        } else {
            log.warn( "No job of id " + key );
            return;
        }
        destroyFailedProgressJob( job, false, cause );
    }

    /**
     * Updates a job with the given taskId with the message.
     * 
     * @param taskId Task Id of the job.
     * @param message The message to update with.
     */
    public static void updateJob( Object taskId, String message ) {
        ProgressJob job = progressJobsByTaskId.get( taskId );
        if ( job != null ) job.updateProgress( message );
    }

    /**
     * @param taskId
     * @param description
     * @return
     */
    private static JobInfo createnewJobInfo( String taskId, String description ) {

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
            log.warn( "No current user, using null for anon user." );
            // JobInfo.user = null just means job was run by anonymous user
            jobI.setUser( null );
        } catch ( AccessDeniedException e ) {
            log.warn( "Access denied" );
        }

        return jobI;
    }

    /**
     * @param progressJob
     * @param doForward
     * @return
     */
    private static String getForwardingUrl( ProgressJob progressJob, boolean doForward ) {
        String toForwardTo = doForward == true ? FORWARD_DEFAULT : null;
        if ( doForward && StringUtils.isNotBlank( progressJob.getForwardingURL() ) ) {
            toForwardTo = progressJob.getForwardingURL();
        }
        return toForwardTo;
    }

    /**
     * To be called ONLY if the task is completely done with and no clients care any more (canceling included). If we
     * don't do this, we will leak memory.
     * 
     * @param progressJob
     */
    public static void cleanupJob( Object taskId ) {

        ProgressJob progressJob = progressJobsByTaskId.get( taskId );

        if ( progressJob == null ) return;

        // stored by user?
        if ( ( progressJob.getUser() != null ) && ( progressJobs.containsKey( progressJob.getUser() ) ) ) {
            Collection<ProgressJob> jobs = progressJobs.get( progressJob.getUser() );
            jobs.remove( progressJob );
            if ( jobs.isEmpty() ) progressJobs.remove( progressJob.getUser() );
        }

        progressJobsByTaskId.remove( taskId );
        log.debug( "Completed cleanup of job: " + taskId );
    }

    public static ProgressJob getJob( String taskId ) {
        if ( taskId == null ) return null;
        return progressJobsByTaskId.get( taskId );
    }

}
