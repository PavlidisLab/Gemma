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
import java.util.GregorianCalendar;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import ubic.gemma.model.common.auditAndSecurity.JobInfo;

/**
 * Implementation of the ProgressJob interface. Used by the client to add hooks for providing feedback to any users
 * 
 * @author klc
 * @author pavlidis
 * @version $Id$
 */
public class ProgressJobImpl extends Observable implements ProgressJob {

    protected Queue<ProgressData> pData;
    protected JobInfo jInfo; // this obj is persisted to DB
    protected int currentPhase;
    protected String forwardingURL;
    protected Object taskId;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "TaskID: " + jInfo.getTaskId();
    }

    /**
     * The factory create method in ProgressManager is the advised way to create a ProgressJob
     * 
     * @param ownerId
     * @param description
     */
    ProgressJobImpl( JobInfo info, String description ) {
        this.pData = new ConcurrentLinkedQueue<ProgressData>();
        this.pData.add( new ProgressData( 0, description, false ) );
        this.jInfo = info;
        this.taskId = info.getTaskId();
        currentPhase = 0;
    }

    /**
     * @return Returns the pData, which can be cleaned out. (this isn't ideal..)
     */
    public Queue<ProgressData> getProgressData() {
        return pData;
    }

    public String getUser() {
        if ( jInfo.getUser() == null ) return null;

        return jInfo.getUser().getUserName();
    }

    /**
     * Updates the percent completion of the job by 1 percent
     */
    public void nudgeProgress() {
        ProgressData d = new ProgressData( pData.peek().getPercent() + 1, "" );
        pData.add( d );
        setChanged();
        notifyObservers( pData );
    }

    /**
     * Updates the progress job by a complete progressData. Used if more than the percent needs to be updates. Updating
     * the entire datapack causes the underlying dao to update its database entry for desciption only
     * 
     * @param pd
     */
    public void updateProgress( ProgressData pd ) {
        this.pData.add( pd );
        updateDescriptionHistory( pd.getDescription() );
        setChanged();
        notifyObservers( pData );
    }

    /**
     * Upates the current progress of the job to the desired percent. doesn't change anything else.
     * 
     * @param newPercent
     */
    public void updateProgress( int newPercent ) {
        ProgressData d = new ProgressData( newPercent, "" );
        pData.add( d );
        setChanged();
        notifyObservers( pData );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.progress.ProgressJob#updateProgress(java.lang.String)
     */
    public void updateProgress( String newDescription ) {
        ProgressData d = new ProgressData( 0, newDescription, false );
        pData.add( d );
        updateDescriptionHistory( newDescription );
        setChanged();
        notifyObservers( pData );
    }

    public void done() {
        Calendar cal = new GregorianCalendar();
        jInfo.setEndTime( cal.getTime() );
        ProgressData d = new ProgressData( 100, "Finished", true );
        pData.add( d );
        notifyObservers( pData );
    }

    public void failed( Throwable cause ) {
        Calendar cal = new GregorianCalendar();
        jInfo.setEndTime( cal.getTime() );
        ProgressData d = new ProgressData( 0, cause.getMessage(), true );
        d.setFailed( true );
        d.setDescription( cause.getMessage() );
        this.pData.add( d );
        setChanged();
        notifyObservers( pData );
    }

    public int getPhase() {
        return currentPhase;
    }

    public void setPhase( int phase ) {
        if ( phase < 0 ) return;

        if ( phase > jInfo.getPhases() ) jInfo.setPhases( phase );

        currentPhase = phase;
    }

    public JobInfo getJobInfo() {
        return this.jInfo;
    }

    /**
     * @return the forwardingURL
     */
    public String getForwardingURL() {
        return forwardingURL;
    }

    /**
     * @param forwardingURL the forwardingURL to set
     */
    public void setForwardingURL( String forwardingURL ) {
        this.forwardingURL = forwardingURL;
    }

    private void updateDescriptionHistory( String message ) {
        if ( this.jInfo.getMessages() == null )
            this.jInfo.setMessages( message );
        else
            this.jInfo.setMessages( this.jInfo.getMessages() + '\n' + message );
    }

    public Object getTaskId() {
        return this.taskId;
    }

}
