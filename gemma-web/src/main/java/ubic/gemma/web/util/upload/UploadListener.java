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
 * Credit:
 *   If you're nice, you'll leave this bit:
 *
 *   Class by Pierre-Alexandre Losson -- http://www.telio.be/blog
 *   email : plosson@users.sourceforge.net
 */
package ubic.gemma.web.util.upload;

import java.util.Queue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;

/**
 * @author Original : plosson on 05-janv.-2006 10:46:33 - Last modified by Author: plosson $ on $Date: 2006/01/05
 *         10:09:38
 * @author pavlidis
 * @version $Id$
 */
public class UploadListener implements OutputStreamListener {

    private static Log log = LogFactory.getLog( UploadListener.class.getName() );

    private long delay = 0;
    private long totalToRead = 0;
    private long totalBytesRead = 0;
    private ProgressJob pJob;
    private String taskId;
    private HttpSession session;

    /**
     * @param request
     * @param debugDelay
     */
    public UploadListener( HttpServletRequest request, long debugDelay ) {
        this( request );
        this.delay = debugDelay;
    }

    public UploadListener( HttpServletRequest request ) {
        this.totalToRead = request.getContentLength();
        this.session = request.getSession();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#start()
     */
    public void start() {
        /*
         * FIXME this is a temporary fallback until we can get reverse ajax working.
         */
        this.taskId = ( String ) this.session.getAttribute( "tmpTaskId" );

        if ( taskId == null ) throw new IllegalStateException( "Task Id could not be located" );

        this.pJob = ProgressManager.createProgressJob( taskId, SecurityContextHolder.getContext().getAuthentication()
                .getName(), "File Upload" );

        pJob.setForwardWhenDone( false );
        pJob.updateProgress( new ProgressData( 0, "Uploading File..." ) );
        log.debug( "Upload monitor started" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#bytesRead(int)
     */
    public void bytesRead( int bytesRead ) {
        Queue<ProgressData> progressData = pJob.getProgressData();

        if ( progressData == null || progressData.size() == 0 ) {
            return; // probably it is finshed.
        }
        int oldPercent = progressData.peek().getPercent();

        this.totalBytesRead = totalBytesRead + bytesRead;
        int newPercent = ( new Double( ( ( double ) totalBytesRead / totalToRead ) * 100.00 ) ).intValue();
        if ( newPercent > oldPercent + 5 || newPercent == 100 ) {
            pJob.updateProgress( newPercent );
            // FIXME the oldPercent is always zero.
            log.debug( newPercent + "% read (" + totalBytesRead + "/" + totalToRead + " bytes) old percent=" + oldPercent );
        }
       
        if ( delay > 0 ) {
            try {
                Thread.sleep( delay );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#error(java.lang.String)
     */
    @SuppressWarnings("unused")
    public void error( String message ) {
        log.error( "There was an error in uploading a file in " + this );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#done()
     */

    public void done() {
        pJob.updateProgress( new ProgressData( 100, "Finished Uploading. Processing File...", true ) );
        ProgressManager.destroyProgressJob( pJob );

    }

    public String getTaskId() {
        return taskId;
    }

}
