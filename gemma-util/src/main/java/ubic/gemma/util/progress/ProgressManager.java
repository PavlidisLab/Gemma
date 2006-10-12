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

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

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

    protected static final Log logger = LogFactory.getLog( ProgressManager.class );

    /**
     * This is a thread local variable for storing the job id that a given thread is working on. Affects the creation of
     * jobs.
     */
    protected static InheritableThreadLocal<Long> currentJob = new InheritableThreadLocal<Long>();
    /*
     * Must use the getter methods to use these static hashmaps so that i can gaurantee syncronization amongst different
     * threads using the maps.
     */
    private static Map<String, List<ProgressJob>> progressJobs = new ConcurrentHashMap<String, List<ProgressJob>>();
    private static Map<Long, ProgressJob> progressJobsById = new ConcurrentHashMap<Long, ProgressJob>();

    private static JobInfoDao jobInfoDao;
    private static UserService userService;

    /**
     * @param ProgresJob
     * @param Observer
     * @return the Simple case. Have a progressJob and want to add themselves for notifications. Could have been done
     *         directly.
     */
    public static synchronized boolean addToNotification( ProgressJob pj, Observer po ) {

        pj.addObserver( po );
        return true;
    }

    /**
     * @param username
     * @param po
     * @return Be careful. This method will add the given observer to receive updates from every progress job for the
     *         given user
     */
    public static synchronized boolean addToNotification( String username, Observer po ) {

        if ( !progressJobs.containsKey( username ) ) return false; // No such user exists with any jobs

        Collection<ProgressJob> pJobs = progressJobs.get( username );
        for ( ProgressJob obs : pJobs ) {
            obs.addObserver( po );
        }

        return true;
    }

    /**
     * @param Job ID
     * @param po
     * @return Given the jobId this method will add the given observer to recieve notifications from that job
     */
    public static synchronized boolean addToNotification( Long jobId, Observer po ) {

        if ( !progressJobsById.containsKey( jobId ) ) return false; // No such job with this id

        progressJobsById.get( jobId ).addObserver( po );

        return true;
    }

    /**
     * @param username
     * @param type
     * @param po
     * @return This adds the observer to only the most recently created progress job for the given user.
     */
    public static synchronized boolean addToRecentNotification( String username, Observer po ) {

        ProgressManager.dump();
        if ( !progressJobs.containsKey( username ) ) return false; // No such user exists with any jobs

        Vector<ProgressJob> pJobs = ( Vector<ProgressJob> ) progressJobs.get( username );

        if ( pJobs.size() == 0 ) return false;

        pJobs.lastElement().addObserver( po );

        return true;
    }

    /**
     * @param id If a valid user name is used for the id the progress job created will be automatically associated with
     *        that user, else there will be no association between the created job and any user. If a user name is not
     *        used then the HTTPSessionID must be used for anonymous users. If it is not used there will be no way for
     *        the Ajax call back to get the progress job that it wants to observer. todo: should session id's be
     *        persisted to the database for anonymous users?
     * @param description (description of the job)
     * @return Use this static method for creating ProgressJobs. if the currently running thread already has a progress
     *         job assciated with it that progress job will be returned.
     */
    public static synchronized ProgressJob createProgressJob( String id, String description ) {

        Collection<ProgressJob> usersJobs;
        ProgressJob newJob;

        assert id != null : "id cannot be null!";

        if ( !progressJobs.containsKey( id ) ) progressJobs.put( id, new Vector<ProgressJob>() );

        usersJobs = progressJobs.get( id );

        // No job currently assciated with this thread or the job assciated with the thread is no longer valid
        if ( ( currentJob.get() == null ) || ( progressJobsById.get( currentJob.get() ) == null ) ) {
            Calendar cal = new GregorianCalendar();
            JobInfo jobI = JobInfo.Factory.newInstance();
            jobI.setRunningStatus( true );
            jobI.setStartTime( cal.getTime() );
            jobI.setDescription( description );

            // User aUser = userService.findByUserName( SecurityContextHolder.getContext().getAuthentication().getName()
            // );
            User aUser = userService.findByUserName( id );
            if ( aUser != null ) jobI.setUser( aUser );

            JobInfo createdJobI = jobInfoDao.create( jobI );

            newJob = new ProgressJobImpl( createdJobI, description );
            currentJob.set( createdJobI.getId() );
            newJob.setPhase( 0 );

            // keep track of these jobs
            usersJobs.add( newJob ); // adds to the progressJobs collection
            progressJobsById.put( createdJobI.getId(), newJob );
        } else {
            Long oldId = currentJob.get();
            newJob = progressJobsById.get( oldId );

            assert newJob != null : "newJob is unexpectedly null in progress Manager"; // This should not be the case!
            newJob.setPhase( newJob.getPhase() + 1 );
            newJob.setDescription( description );
        }

        newJob.setTrackingId( id );
        ProgressManager.dump();

        return newJob;
    }

    // As the progress manager is a singleton leaks and strange behavior are likely.
    // i made this to get a peek at what was going on under the hood at runtime.
    public static synchronized void dump() {
        logger.info( "Dump ProgressMangagers State:" );

        logger.info( "Thread Local variable: " + currentJob.get() );

        logger.info( "ProgressJobs Dump:  " );
        for ( Iterator iter = progressJobs.keySet().iterator(); iter.hasNext(); ) {
            String name = ( String ) iter.next();
            logger.info( "name: " + name );

            for ( Iterator values = progressJobs.get( name ).iterator(); values.hasNext(); ) {
                ProgressJob job = ( ProgressJob ) values.next();
                logger.info( "====> progressJob: " + job );
            }
        }

        logger.info( "ProgressJobsById Dump:  " );
        for ( Iterator iter = progressJobsById.keySet().iterator(); iter.hasNext(); ) {
            Long id = ( Long ) iter.next();
            logger.info( "  Id: " + id + "  ProgressJob: " + progressJobsById.get( id ) );

        }

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

        Long id = currentJob.get();
        threadsJob = progressJobsById.get( id );

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

        Long id = currentJob.get();
        threadsJob = progressJobsById.get( id );

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

        Long id = currentJob.get();
        threadsJob = progressJobsById.get( id );
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

        Long id = currentJob.get();
        threadsJob = progressJobsById.get( id );
        threadsJob.nudgeProgress();
        threadsJob.setDescription( message );
        return true;
    }

    /**
     * @param ajob Removes ProgressJob from notification lists and provides general clean up. Also causes job to be
     *        persisted to db. This method should only be called once per thread. Easiest to put this call in the
     *        controller that needs to provide user with progress information
     */
    public static synchronized boolean destroyProgressJob( ProgressJob ajob ) {

        ajob.updateProgress( new ProgressData( 100, "Finalizing and cleaning up...", true, ajob.getForwardingURL() ) );
        ajob.done();

        if ( progressJobs.containsKey( ajob.getTrackingId() ) ) {
            Collection jobs = progressJobs.get( ajob.getTrackingId() );
            jobs.remove( ajob );
            if ( jobs.isEmpty() ) progressJobs.remove( ajob.getTrackingId() );
        }
        if ( progressJobsById.containsKey( ajob.getId() ) ) progressJobsById.remove( ajob.getId() );

        currentJob.set( null );
        jobInfoDao.update( ajob.getJobInfo() );

        return true;
    }

    public void setJobInfoDao( JobInfoDao jobDao ) {
        jobInfoDao = jobDao;
    }

    public void setUserService( UserService usrService ) {
        userService = usrService;
    }

}
