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

package ubic.gemma.web.util.progress;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    protected static final Log logger = LogFactory.getLog( ProgressManager.class );

    /*
     * Must use the getter methods to use these static hashmaps so that i can gaurantee syncronization amongst different
     * threads using the maps.
     */
    private static Map<String, List<ProgressJob>> progressJobs = new ConcurrentHashMap<String, List<ProgressJob>>();

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
     * @param username
     * @param type
     * @param po
     * @return This adds the observer to only the most recently created progress job of the specified type for the given
     *         user.
     */
    public static synchronized boolean addToNotification( String username, int progressType, Observer po ) {

        if ( !progressJobs.containsKey( username ) ) return false; // No such user exists with any jobs

        Collection<ProgressJob> pJobs = progressJobs.get( username );
        for ( ProgressJob obs : pJobs ) {
            if ( obs.getProgressType() == progressType ) obs.addObserver( po );
        }

        return true;
    }

    /**
     * @param username
     * @param type
     * @param po
     * @return This adds the observer to only the most recently created progress job for the given user.
     */
    public static synchronized boolean addToRecentNotification( String username, Observer po ) {

        if ( !progressJobs.containsKey( username ) ) return false; // No such user exists with any jobs

        Vector<ProgressJob> pJobs = ( Vector ) progressJobs.get( username );
        pJobs.lastElement().addObserver( po );
        
        return true;
    }

    /**
     * @param username
     * @param type
     * @param po
     * @return This adds the observer to all of a given users progress jobs that are of a given type. types are defined
     *         in the ProgressJob interface
     */
    public static synchronized boolean addToRecentNotification( String username, int progressType, Observer po ) {

        if ( !progressJobs.containsKey( username ) ) return false; // No such user exists with any jobs

        Collection<ProgressJob> pJobs = progressJobs.get( username );

        // need to iterate backwards through the list to get the most recently added of a given type.
        ProgressJob[] arrayOfPJs = pJobs.toArray( new ProgressJob[pJobs.size()] );
        int i = pJobs.size();
        while ( i >= 0 ) {
            i--;
            if ( arrayOfPJs[i].getProgressType() == progressType ) {
                arrayOfPJs[i].addObserver( po );
                return true;
            }
        }

        return false;
    }

    /**
     * @param userName (owner of the job)
     * @param description (description of the job)
     * @return Use this static method for creating ProgressJobs.
     */
    public static synchronized ProgressJob createProgressJob( String userName, int progressType, String description ) {

        Collection<ProgressJob> usersJobs;
        ProgressJob newJob;

        if ( !progressJobs.containsKey( userName ) ) {
            progressJobs.put( userName, new Vector<ProgressJob>() );
        }

        usersJobs = progressJobs.get( userName );
        newJob = new ProgressJobImpl( userName, description, progressType );
        usersJobs.add( newJob );

        return newJob;
    }

    /**
     * @param ajob Removes ProgressJob from notification lists and provides general clean up. EveryJob that is created
     *        should be destroyed.
     */
    public static synchronized boolean destroyProgressJob( ProgressJob ajob ) {

        if ( progressJobs.containsKey( ajob.getUser() ) ) {
            Collection jobs = progressJobs.get( ajob.getUser() );
            jobs.remove( ajob );
            if ( jobs.isEmpty() ) progressJobs.remove( ajob.getUser() );
        }

        return true;
    }

}
