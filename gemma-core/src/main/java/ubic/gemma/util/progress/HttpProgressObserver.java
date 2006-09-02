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

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <hr
 * <p>
 * This class is intended to be stored in the web session for a given user. Receives updates for progressJobs and stores
 * the current progressData.
 * 
 * @author klc
 * @version $Id$
 */

public class HttpProgressObserver implements Observer, Serializable {

    private static final long serialVersionUID = -1346814251664733438L;
    protected static final Log logger = LogFactory.getLog( HttpProgressObserver.class );

    protected ProgressData pData;

    /**
     * Generaly use constructor. Will automatically register for observer the current users progress jobs. todo add
     * support for users with multiple progress jobs
     */
    public HttpProgressObserver() {

        pData = new ProgressData( 0, "Initilizing", false );
        // Not sure the best way to do this. Perpaps subclassing for different types of monitors...
        // SecurityContextHolder.getContext().getAuthentication.getName();
        ProgressManager.addToRecentNotification( SecurityContextHolder.getContext().getAuthentication().getName(), this );
        // ProgressManager.addToNotification( ExecutionContext.get().getHttpServletRequest().getRemoteUser(), this );
    }

    /**
     * A secondary constructor that can be used for testing purposes or when it is desierable to monitor another users
     * processes.
     */
    public HttpProgressObserver( String userName ) {
        pData = new ProgressData( 0, "Initilizing", false );
        ProgressManager.addToNotification( userName, this );

    }

   

    public ProgressData getProgressData() {
        return pData;
    }

    @SuppressWarnings("unused")
    public void update(Observable o, Object pd)
    {
        this.pData = (ProgressData) pd;
    }
    
    /**
     * Tells the observer to stop observering. Remove it self from observations lists. Should be called when the
     * observer is not needed anymore. // todo add code for cleaning up cleaning up notification references. IE stop
     * observering...
     */
    public void finished() {

    }
}
