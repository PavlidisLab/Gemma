/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.util.gemmaspaces;

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.progress.ProgressManager;

import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.ExternalEntry;

/**
 * This observer receives notifications from a java space. Most likely, these notification entires were written to the
 * space by a worker.
 * <p>
 * A client instantiating this class would then pass it to the {@link GigaSpacesTemplate} with a call to
 * addNotifyDelegatorListener.
 * 
 * @author keshav
 * @version $Id$
 */
public class GemmaSpacesJobObserver implements RemoteEventListener {
    private Log log = LogFactory.getLog( this.getClass() );
    String taskId = null;

    /**
     * @param taskId
     */
    public GemmaSpacesJobObserver( String taskId ) {
        this.taskId = taskId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {
        log.debug( "Notification received from space ..." );

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;
            log.debug( "event: " + arrivedRemoteEvent );

            ExternalEntry entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );
            log.debug( "entry: " + entry );
            log.debug( "id: " + arrivedRemoteEvent.getID() );
            log.debug( "sequence number: " + arrivedRemoteEvent.getSequenceNumber() );
            log.debug( "notify type: " + arrivedRemoteEvent.getNotifyType() );

            // ThreadUtil.visitAllRunningThreads();

            /* updated the progress with message from notification */
            String message = ( String ) entry.getFieldValue( "message" );

            ProgressManager.updateJob( this.taskId, message );

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
