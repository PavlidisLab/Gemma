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
package ubic.gemma.job.grid.util;

import net.jini.core.entry.UnusableEntryException;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.progress.ProgressManager;
import ubic.gemma.job.progress.grid.SpacesProgressAppender;
import ubic.gemma.util.ConfigUtils;

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
 * @see SpacesProgressAppender
 */
public class SpacesJobObserver implements RemoteEventListener {
    private Log log = LogFactory.getLog( this.getClass() );
    String taskId = null;

    /**
     * Whether we should log messages in the usual fashion as well as updating the ProgressManager. If true, this means
     * that all remote logging messages will appear in the client logs (e.g., catalina.out).
     */
    private boolean doLoggingToo = false;

    /**
     * @param taskId
     */
    public SpacesJobObserver( String taskId ) {
        assert taskId != null;
        this.taskId = taskId;
        this.doLoggingToo = ConfigUtils.getBoolean( "gemma.grid.poollogs", false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    @Override
    public void notify( RemoteEvent remoteEvent ) {

        if ( remoteEvent instanceof EntryArrivedRemoteEvent ) {

            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;

            ExternalEntry entry;
            try {
                entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );

                if ( log.isDebugEnabled() ) {
                    log.debug( "Event: " + arrivedRemoteEvent );
                    log.debug( "Entry: " + entry );
                    log.debug( "Id: " + arrivedRemoteEvent.getID() );
                    log.debug( "Sequence number: " + arrivedRemoteEvent.getSequenceNumber() );
                    log.debug( "notify type: " + arrivedRemoteEvent.getNotifyType() );
                }

                /* updated the progress with message from notification */
                String message = ( String ) entry.getFieldValue( "message" );
                if ( StringUtils.isNotBlank( message ) ) {
                    if ( this.doLoggingToo ) {
                        log.info( message + " [Remote task: " + taskId + "]" );
                    }
                    ProgressManager.updateJob( this.taskId, message );
                }
            } catch ( UnusableEntryException e ) {
                log.warn( e );
            }
        } else {
            log.warn( "Don't know how to deal with a " + remoteEvent.getClass().getName() );
        }

    }

}
