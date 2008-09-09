/* Copyright (c) 2006-2007University of British Columbia
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

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.auditAndSecurity.JobInfo;
import ubic.gemma.model.common.auditAndSecurity.JobInfoDao;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;

/**
 * Singleton for creating observers for monitoring long-running processes.
 * <p>
 * To use, call ProgressManager.createProgressJob(). The returned value can be used by a client object to indicate
 * progress by calling updateProgress on the job that is returned. Observers can register themselves for receiving
 * progress updates by passing themselves into the addToNotification function
 * 
 * @author kelsey
 * @version $Id$
 * @spring.bean id="progressManager"
 * @spring.property name="jobInfoDao" ref="jobInfoDao"
 * @spring.property name="userService" ref="userService"
 */
public class ProgressManager {

    public static final String FORWARD_DEFAULT = "/Gemma/checkJobProgress.html";

    protected static final Log log = LogFactory.getLog( ProgressManager.class );

    /**
     * This is a thread local variable for storing the job id that a given thread is working on. Affects the creation of
     * jobs.
     */
    protected static InheritableThreadLocal<Object> currentJob = new InheritableThreadLocal<Object>();
    /*
     * Must use the getter methods to use these static hashmaps so that i can gaurantee syncronization amongst different
     * threads using the maps.
     */
    private static Map<Object, List<ProgressJob>> progressJobs = new ConcurrentHashMap<Object, List<ProgressJob>>();
    private static Map<Object, ProgressJob> progressJobsByTaskId = new ConcurrentHashMap<Object, ProgressJob>();

    private static JobInfoDao jobInfoDao;
    private static UserService userService;

    public ProgressJob getJob( String taskId ) {
        return progressJobsByTaskId.get( taskId );
    }

    /**
     * @param taskId The id of the job to be started.
     * @param UserId This could be a user name or some kind of sessionID that the user is using. If a user name is not
     *        used then the HTTPSessionID must be used for anonymous users. If it is not used there will be no way for
     *        the Ajax call back to get the progress job that it wants to observer. todo: should session id's be
     *        persisted to the database for anonymous users?
     * @param description (description of the job)
     * @return Use this static method for creating ProgressJobs. if the currently running thread already has a progress
     *         job assciated with it that progress job will be returned.
     */
    public static synchronized ProgressJob createProgressJob( String taskId, String userId, String description ) {

        Collection<ProgressJob> usersJobs;
        ProgressJob newJob = null;

        if ( taskId == null ) {
            throw new IllegalArgumentException( "Task id cannot be null" );
        }

        if ( !progressJobs.containsKey( userId ) ) {
            log.debug( "Creating new progress job(s) with key " + userId );
            progressJobs.put( userId, new Vector<ProgressJob>() );
        } else {
            log.debug( "Already have job with key " + userId );
        }

        usersJobs = progressJobs.get( userId );

        // No job currently assciated with this thread or the job assciated with the thread is no longer valid
        if ( ( currentJob.get() == null ) || ( progressJobsByTaskId.get( currentJob.get() ) == null ) ) {
            JobInfo jobI = createnewJobInfo( taskId, userId, description );

            try {
                JobInfo createdJobI = jobInfoDao.create( jobI );
                newJob = new ProgressJobImpl( createdJobI, description );
            } catch ( Exception e ) {
                log.warn( "Unable to create jobinfo in database: " + e.getMessage() );
                newJob = new ProgressJobImpl( jobI, description );

            }

            currentJob.set( taskId );

            newJob.setPhase( 0 );

            // keep track of these jobs
            usersJobs.add( newJob ); // adds to the progressJobs collection
            progressJobsByTaskId.put( taskId, newJob );
        } else {
            Object oldId = currentJob.get();
            newJob = progressJobsByTaskId.get( oldId );

            assert newJob != null : "newJob is unexpectedly null in progress Manager"; // This should not be the case!
            newJob.setPhase( newJob.getPhase() + 1 );
        }

        ProgressManager.dump();

        return newJob;
    }

    /**
     * @param taskId
     * @param description
     * @return
     */
    private static JobInfo createnewJobInfo( String taskId, String userName, String description ) {

        Calendar cal = new GregorianCalendar();
        JobInfo jobI = JobInfo.Factory.newInstance();
        jobI.setRunningStatus( true );
        jobI.setStartTime( cal.getTime() );
        jobI.setDescription( description );
        jobI.setTaskId( taskId );

        User aUser = userService.findByUserName( userName );

        // Try to use the userName asscciated with the security context
        if ( aUser == null )
            aUser = userService.findByUserName( SecurityContextHolder.getContext().getAuthentication().getName() );

        if ( aUser != null )
            jobI.setUser( aUser );
        else {
            jobI.setUser( null );
            log
                    .debug( "No user assciated with job. Client side observer will have no way to receive progress messages.  Use sesison ID for anonymous users." );
        }

        return jobI;
    }

    // As the progress manager is a singleton leaks and strange behavior are likely.
    // i made this to get a peek at what was going on under the hood at runtime.
    public static synchronized void dump() {

        if ( !log.isDebugEnabled() ) return;

        log.debug( "Dump ProgressMangagers State:" );

        log.debug( "Thread Local variable: " + currentJob.get() );

        log.debug( "ProgressJobs Dump:  " );
        for ( Iterator iter = progressJobs.keySet().iterator(); iter.hasNext(); ) {
            String name = ( String ) iter.next();
            log.debug( "name: " + name );

            for ( Iterator values = progressJobs.get( name ).iterator(); values.hasNext(); ) {
                ProgressJob job = ( ProgressJob ) values.next();
                log.debug( "====> progressJob: " + job.getTaskId() );
            }
        }

        log.debug( "ProgressJobsById Dump:  " );
    }

    /**
     * This will send an update to the current threads progress job, if it has one. Should be used for adding progress
     * updates with minimal intrusion into objects that are long running.
     * 
     * @param pData a progress data instance.
     * @return true if the thread had a progress job already and it was successful in updating it, false otherwise.
     */
    public static synchronized boolean updateCurrentThreadsProgressJob( ProgressData pData ) {

        ProgressJob threadsJob = null;

        if ( currentJob.get() == null ) return false;

        Object id = currentJob.get();
        threadsJob = progressJobsByTaskId.get( id );

        if ( threadsJob == null ) {
            log.debug( "Current threads job id not found in active job list. Current threads job id is invalid. id =: "
                    + id );
            return false;
        }
        if ( pData == null )
            threadsJob.nudgeProgress();
        else
            threadsJob.updateProgress( pData );

        return true;
    }

    /**
     * @param message
     * @return
     */
    public static synchronized boolean updateCurrentThreadsProgressJob( String message ) {

        ProgressJob threadsJob = null;

        if ( currentJob.get() == null ) return false;

        Object id = currentJob.get();
        threadsJob = progressJobsByTaskId.get( id );

        if ( threadsJob == null ) return false;

        threadsJob.updateProgress( message );

        return true;
    }

    /**
     * Increase the progress state of the job by 1 percent.
     * 
     * @return true if there is a progress job.
     */
    public static synchronized boolean nudgeCurrentThreadsProgressJob() {
        ProgressJob threadsJob = null;

        if ( currentJob.get() == null ) return false;

        Object id = currentJob.get();
        threadsJob = progressJobsByTaskId.get( id );
        threadsJob.nudgeProgress();
        return true;
    }

    /**
     * Increase the progress state of the job by 1 percent and update the description
     * 
     * @param message The new description for the job.
     * @return true if there is a progress job.
     */
    public static synchronized boolean nudgeCurrentThreadsProgressJob( String message ) {
        ProgressJob threadsJob = null;

        if ( currentJob.get() == null ) return false;

        Object id = currentJob.get();
        threadsJob = progressJobsByTaskId.get( id );
        threadsJob.nudgeProgress();
        // threadsJob.setDescription( message );
        return true;
    }

    /**
     * @param progressJob
     * @return
     */
    public static synchronized boolean destroyProgressJob( ProgressJob progressJob ) {
        return destroyProgressJob( progressJob, false );
    }

    /**
     * @param progressJob
     * @param doForward
     * @param cause
     * @return
     */
    public static synchronized boolean destroyFailedProgressJob( ProgressJob progressJob, boolean doForward,
            Throwable cause ) {
        if ( progressJob == null ) {
            log
                    .debug( "ProgressManager.destroyProgressJob received a null reference for a progressJob, hence can't destroy." );
            return false;
        }
        log.debug( "Finishing up failed " + progressJob );

        String toForwardTo = getForwardingUrl( progressJob, doForward );

        ProgressData data = new ProgressData( 100, "Job failed.", true, toForwardTo );
        data.setFailed( true );
        progressJob.updateProgress( data );
        progressJob.failed( cause );

        currentJob.set( null );
        jobInfoDao.update( progressJob.getJobInfo() );

        return true;
    }

    /**
     * To be called ONLY if the task is completely done with and no clients care any more. This is a hack! But if we
     * don't do this, we will leak memory.
     * 
     * @param progressJob
     */
    public void cleanupJob( Object taskId ) {

        ProgressJob progressJob = progressJobsByTaskId.get( taskId );

        if ( progressJob == null ) return;

        // stored by user?
        if ( ( progressJob.getUser() != null ) && ( progressJobs.containsKey( progressJob.getUser() ) ) ) {
            Collection jobs = progressJobs.get( progressJob.getUser() );
            jobs.remove( progressJob );
            if ( jobs.isEmpty() ) progressJobs.remove( progressJob.getUser() );
        }

        progressJobsByTaskId.remove( taskId );
        log.debug( "Completed cleanup of job: " + taskId );
    }

    /**
     * Removes ProgressJob from notification lists and provides general clean up. Also causes job to be persisted to db.
     * This method should only be called once per thread. Easiest to put this call in the controller that needs to
     * provide user with progress information
     * 
     * @param ajob
     */
    public static synchronized boolean destroyProgressJob( ProgressJob progressJob, boolean doForward ) {

        if ( progressJob == null ) {
            log
                    .debug( "ProgressManager.destroyProgressJob received a null reference for a progressJob, hence can't destroy." );
            return false;
        }
        log.debug( "Destroying " + progressJob );

        String toForwardTo = getForwardingUrl( progressJob, doForward );
        ProgressData progressData = new ProgressData( 100, "Job completed.", true, toForwardTo );
        progressData.setPayload( progressJob.getPayload() );// must be a better way.
        progressJob.updateProgress( progressData );
        progressJob.done();

        currentJob.set( null );
        try {
            jobInfoDao.update( progressJob.getJobInfo() );
        } catch ( RuntimeException e ) {
            log.warn( "Could not update jobinfo :" + e );
        }
        return true;
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

    public void setJobInfoDao( JobInfoDao jobDao ) {
        jobInfoDao = jobDao;
    }

    public void setUserService( UserService usrService ) {
        userService = usrService;
    }

    /**
     * @param key
     */
    public static synchronized void signalDone( Object key ) {
        log.debug( key + " Done!" );
        ProgressJob job = progressJobsByTaskId.get( key );
        assert job != null : "No job of id " + key;
        destroyProgressJob( job, job.forwardWhenDone() );
    }

    /**
     * @param key
     */
    public static synchronized void signalCancelled( Object key ) {
        log.debug( key + " Cancelled" );
        ProgressJob job = progressJobsByTaskId.get( key );
        if ( job != null ) job.getJobInfo().setFailedMessage( "Cancellation was signalled by user" );
        destroyProgressJob( job, false ); // never forward.
    }

    /**
     * @param key
     * @param cause
     */
    public static synchronized void signalFailed( Object key, Throwable cause ) {
        log.error( key + " Failed: " + cause.getMessage() );
        ProgressJob job = progressJobsByTaskId.get( key );
        if ( job != null ) {
            job.getJobInfo().setFailedMessage( cause.toString() );
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

    public static ProgressJob createProgressJob( String taskId, String name, String string, boolean doForward ) {
        ProgressJob job = createProgressJob( taskId, name, string );
        job.setForwardWhenDone( doForward );
        return job;
    }

}
