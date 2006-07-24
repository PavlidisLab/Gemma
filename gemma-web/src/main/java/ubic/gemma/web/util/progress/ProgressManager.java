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
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Singleton for creating observers for monitoring long-running processes.
 * <p>
 * To use, call ProgressManager.createProgressJob(). The returned value can be used by a client object to indicate
 * progress by calling ... (FIXME, describe how to use)
 * 
 * @author kelsey
 * @version $Id$
 */
public class ProgressManager {

    protected static final Log logger = LogFactory.getLog( ProgressManager.class );

    protected static Map<ProgressJob, Collection<ProgressObserver>> notificationListByJob = new Hashtable<ProgressJob, Collection<ProgressObserver>>();
    protected static Map<String, Collection<ProgressObserver>> notificationListByUser = new Hashtable<String, Collection<ProgressObserver>>();
    protected static Map<String, Collection<ProgressJob>> progressJobs = new Hashtable<String, Collection<ProgressJob>>();

    /**
     * @param pj
     * @param po
     * @return
     */
    public static boolean addToNotification( ProgressJob pj, ProgressObserver po ) {

        if ( !notificationListByJob.containsKey( pj ) ) return false;

        notificationListByJob.get( pj ).add( po );
        return true;
    }

    /**
     * @param username
     * @param po
     * @return
     */
    public static boolean addToNotification( String username, ProgressObserver po ) {

        if ( !notificationListByUser.containsKey( username ) ) return false;

        notificationListByUser.get( username ).add( po );
        return true;
    }

    /**
     * @param userName
     * @param description
     * @return
     */
    public static ProgressJob createProgressJob( String userName, String description ) {

        Collection<ProgressJob> usersJobs;
        ProgressJob newJob;

        if ( !progressJobs.containsKey( userName ) ) progressJobs.put( userName, new Vector<ProgressJob>() );

        usersJobs = progressJobs.get( userName );
        newJob = new ProgressJobImpl( userName, description );
        usersJobs.add( newJob );

        return newJob;

    }

    // static boolean Notify( String UserName ) {
    //        
    // if (!notificationListByJob.containsKey(pj))
    // return false;
    //        
    // for ( Iterator iter = notificationListByJob.get(UserName)).iterator(); iter.hasNext();) {
    // ((ProgressObserver) iter.next()).progressUpdate());
    //        
    // }

    /**
     * @param ajob
     */
    static boolean destroyProgressJob( ProgressJob ajob ) {

        if ( notificationListByJob.containsKey( ajob ) ) notificationListByJob.remove( ajob );

        if ( notificationListByUser.containsKey( ajob.getUser() ) ) notificationListByUser.remove( ajob.getUser() );

        if ( progressJobs.containsKey( ajob.getUser() ) ) {
            Collection jobs = progressJobs.get( ajob.getUser() );
            jobs.remove( ajob );
        }

        return true;
    }

    /**
     * @param pj
     * @return
     */
    static boolean notify( ProgressJob pj ) {

        if ( !notificationListByJob.containsKey( pj ) ) return false;

        for ( ProgressObserver observer : notificationListByJob.get( pj ) ) {
            observer.progressUpdate( pj.getProgressData() );
        }

        return true;

    }

}
