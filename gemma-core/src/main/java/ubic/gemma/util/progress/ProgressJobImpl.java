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

import ubic.gemma.model.common.auditAndSecurity.JobInfo;

/**
 * <hr>
 * Implementation of the ProgressJob interface. Used by the client to add hooks for providing feedback to any users
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */
/**
 * TODO - DOCUMENT ME
 * 
 * @author pavlidis
 * @version $Id$
 */
/**
 * TODO - DOCUMENT ME
 * 
 * @author pavlidis
 * @version $Id$
 */
/**
 * TODO - DOCUMENT ME
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProgressJobImpl extends Observable implements ProgressJob {

    protected ProgressData pData;
    protected JobInfo jInfo;
    protected int currentPhase;
    protected String trackingId;
    protected String forwardingURL;

    /**
     * The factory create method in ProgressManager is the advised way to create a ProgressJob
     * 
     * @param ownerId
     * @param description
     */
    ProgressJobImpl( JobInfo info, String description ) {
        this.pData = new ProgressData( 0, description, false );
        this.jInfo = info;
        currentPhase = 0;
    }

    /**
     * @return Returns the pData.
     */
    public ProgressData getProgressData() {
        return pData;
    }

    /**
     * @param data The pData to set.
     */
    public void setProgressData( ProgressData data ) {
        pData = data;
    }

    /**
     * @return Returns the runningStatus.
     */
    public boolean isRunningStatus() {
        return jInfo.getRunningStatus();
    }

    /**
     * @param runningStatus The runningStatus to set.
     */
    public void setRunningStatus( boolean runningStatus ) {
        jInfo.setRunningStatus( runningStatus );
        if ( !jInfo.getRunningStatus() ) this.pData.setDone( false );
    }

    public String getUser() {
        if ( jInfo.getUser() == null ) return null;

        return jInfo.getUser().getUserName();
    }

    /**
     * Updates the percent completion of the job by 1 percent
     */
    public void nudgeProgress() {
        pData.setPercent( pData.getPercent() + 1 );
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
        setProgressData( pd );
        this.jInfo.setDescription( pd.getDescription() );
        setChanged();
        notifyObservers( pData );
    }

    /**
     * Upates the current progress of the job to the desired percent. doesn't change anything else.
     * 
     * @param newPercent
     */
    public void updateProgress( int newPercent ) {
        pData.setPercent( newPercent );
        setChanged();
        notifyObservers( pData );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.progress.ProgressJob#updateProgress(java.lang.String)
     */
    public void updateProgress( String newDescription ) {
        pData.setDescription( newDescription );
        setChanged();
        notifyObservers( pData );
    }

    /**
     * returns the id of the current job
     */
    public Long getId() {
        return jInfo.getId();
    }

    public void done() {

        Calendar cal = new GregorianCalendar();
        jInfo.setEndTime( cal.getTime() );

    }

    public int getPhase() {
        return currentPhase;

    }

    public void setPhase( int phase ) {
        if ( phase < 0 ) return;

        if ( phase > jInfo.getPhases() ) jInfo.setPhases( phase );

        currentPhase = phase;
    }

    public void setDescription( String description ) {
        this.pData.setDescription( description );
        this.jInfo.setDescription( description );
    }

    public String getDescription() {
        return this.pData.getDescription();
    }

    public JobInfo getJobInfo() {
        return this.jInfo;
    }

    /**
     * @return the anonymousId
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     * @param anonymousId the anonymousId to set
     */
    public void setTrackingId( String trackingId ) {
        this.trackingId = trackingId;
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

}
