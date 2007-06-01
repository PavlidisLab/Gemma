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

import java.io.Serializable;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContextFactory;

/**
 * This class is intended to be stored in the web session for a given user. Receives updates for progressJobs and stores
 * the current progressData.
 * 
 * @author klc
 * @version $Id$
 */
public class HttpProgressObserver implements  Serializable {

    private static final long serialVersionUID = -1346814251664733438L;
    protected static final Log logger = LogFactory.getLog( HttpProgressObserver.class );

    protected Queue<ProgressData> pData;

    /**
     * Indicates whether this has a live ProgressJob
     */
    private boolean hasLiveJob = false;

    /**
     * General use constructor. Will automatically register for observer the current users progress jobs. todo add
     * support for users with multiple progress jobs
     */
//    public HttpProgressObserver() {
//
//        pData = new ConcurrentLinkedQueue<ProgressData>();
//
//        pData.add( new ProgressData( 0, "Initializing", false ) );
//        // Not sure the best way to do this. Perpaps subclassing for different types of monitors...
//
//        this.hasLiveJob = ProgressManager.addToRecentNotification( SecurityContextHolder.getContext()
//                .getAuthentication().getName(), this );
//
//        /*
//         * Can fail to get a live job if 1) there is no job or 2) there is no user.
//         */
//        if ( !hasLiveJob ) {
//
//            String sessionId = WebContextFactory.get().getSession().getId();
//            this.hasLiveJob = ProgressManager.addToRecentNotification( sessionId, this );
//
//            logger.debug( "No user associated with security context.  Tried to use session id: " + sessionId
//                    + "  successful = " + hasLiveJob );
//
//        } else
//            logger.debug( "User associated with security context. User = "
//                    + SecurityContextHolder.getContext().getAuthentication().getName() );
//
//    }

    // /**
    // * Use this constructor if the taskId of the Job is known. Best Case situation.
    // */
    // public HttpProgressObserver( String taskId ) {
    // pData = new ConcurrentLinkedQueue<ProgressData>();
    //
    // pData.add( new ProgressData( 0, "Initializing", false ) );
    //
    // this.hasLiveJob = ProgressManager.addToNotification( taskId, this );
    //
    // if ( !hasLiveJob ) {
    //
    // // try again (3 times). If still fails then something is wrong!
    // try {
    // this.wait( 500 );
    // this.hasLiveJob = ProgressManager.addToNotification( taskId, this );
    // if ( this.hasLiveJob ) return;
    // this.wait( 500 );
    // this.hasLiveJob = ProgressManager.addToNotification( taskId, this );
    //
    // } catch ( Exception e ) {
    // logger.warn( "Error while pausing for between attempts at registering for progress info: " + e );
    // }
    //
    // }
    //
    // }
    //
    // public synchronized Queue<ProgressData> getProgressData() {
    // // logger.debug( "Returning Progress Data to client: " + pData.getDescription() + " " + pData.getPercent() );
    // return pData;
    // }
    //
    // @SuppressWarnings("unused")
    // public void update( Observable o, Object pd ) {
    // this.pData.add( ( ProgressData ) pd );
    // }
    //
    // /**
    // * Tells the observer to stop observering. Remove it self from observations lists. Should be called when the
    // * observer is not needed anymore. // todo add code for cleaning up cleaning up notification references. IE stop
    //     * observering...
    //     */
    //    public void finished() {
    //
    //    }
    //
    //    public boolean hasLiveJob() {
    //        return hasLiveJob;
    //    }
}
