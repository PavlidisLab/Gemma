/* Copyright (c) 2006-2010 University of British Columbia
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
    protected String forwardingURL;
    protected String taskId;
    boolean forwardWhenDone = true;

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
        this.pData.add( new ProgressData( info.getTaskId(), 0, description, false ) );
        this.jInfo = info;
        assert info.getTaskId() != null;
        this.taskId = info.getTaskId();
    }

    /**
     * @return Returns the pData, which can be cleaned out. (this isn't ideal..)
     */
    @Override
    public Queue<ProgressData> getProgressData() {
        return pData;
    }

    @Override
    public String getUser() {
        if ( jInfo.getUser() == null ) return null;

        return jInfo.getUser().getUserName();
    }

    /**
     * Updates the percent completion of the job by 1 percent
     */
    @Override
    public void nudgeProgress() {
        ProgressData d = new ProgressData( this.taskId, pData.peek().getPercent() + 1, "" );
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
    @Override
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
    @Override
    public void updateProgress( int newPercent ) {
        ProgressData d = new ProgressData( this.taskId, newPercent, "" );
        pData.add( d );
        setChanged();
        notifyObservers( pData );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.progress.ProgressJob#updateProgress(java.lang.String)
     */
    @Override
    public void updateProgress( String newDescription ) {
        ProgressData d = new ProgressData( this.taskId, 0, newDescription, false );
        pData.add( d );
        updateDescriptionHistory( newDescription );
        setChanged();
        notifyObservers( pData );
    }

    /**
     * Signal completion
     */
    @Override
    public void done() {
        Calendar cal = new GregorianCalendar();
        jInfo.setEndTime( cal.getTime() );
        ProgressData d = new ProgressData( this.taskId, 100, "", true );
        pData.add( d );
        notifyObservers( pData );
    }

    @Override
    public void failed( Throwable cause ) {
        Calendar cal = new GregorianCalendar();
        jInfo.setEndTime( cal.getTime() );
        ProgressData d = new ProgressData( this.taskId, 0, cause.getMessage(), true );
        d.setFailed( true );
        d.setDescription( cause.getMessage() );
        this.pData.add( d );
        setChanged();
        notifyObservers( pData );
    }

    @Override
    public JobInfo getJobInfo() {
        return this.jInfo;
    }

    /**
     * @return the forwardingURL
     */
    @Override
    public String getForwardingURL() {
        return forwardingURL;
    }

    /**
     * @param forwardingURL the forwardingURL to set
     */
    @Override
    public void setForwardingURL( String forwardingURL ) {
        this.forwardingURL = forwardingURL;
    }

    private void updateDescriptionHistory( String message ) {
        if ( this.jInfo.getMessages() == null )
            this.jInfo.setMessages( message );
        else
            this.jInfo.setMessages( this.jInfo.getMessages() + '\n' + message );
    }

    @Override
    public String getTaskId() {
        return this.taskId;
    }

    @Override
    public boolean forwardWhenDone() {
        return this.forwardWhenDone;
    }

    @Override
    public void setForwardWhenDone( boolean value ) {
        this.forwardWhenDone = value;
    }

}
