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
package edu.columbia.gemma.web.util.upload;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Original : plosson on 05-janv.-2006 10:46:33 - Last modified by Author: plosson $ on $Date: 2006/01/05
 *         10:09:38
 * @author pavlidis*
 * @version  $Id$
 */
public class UploadListener implements OutputStreamListener {

    private static Log log = LogFactory.getLog( UploadListener.class.getName() );

    private HttpServletRequest request;
    private long delay = 0;
    private long startTime = 0;
    private int totalToRead = 0;
    private int totalBytesRead = 0;
    private int totalFiles = -1;

    /**
     * @param request
     * @param debugDelay
     */
    public UploadListener( HttpServletRequest request, long debugDelay ) {
        this.request = request;
        this.delay = debugDelay;
        totalToRead = request.getContentLength();
        this.startTime = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.web.util.upload.OutputStreamListener#start()
     */
    public void start() {
        totalFiles++;
        updateUploadInfo( "start" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.web.util.upload.OutputStreamListener#bytesRead(int)
     */
    public void bytesRead( int bytesRead ) {
        totalBytesRead = totalBytesRead + bytesRead;
        updateUploadInfo( "progress" );

        try {
            Thread.sleep( delay );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.web.util.upload.OutputStreamListener#error(java.lang.String)
     */
    @SuppressWarnings("unused")
    public void error( String message ) {
        updateUploadInfo( "error" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.web.util.upload.OutputStreamListener#done()
     */
    public void done() {
        updateUploadInfo( "done" );
    }

    // private long getDelta() {
    // return ( System.currentTimeMillis() - startTime ) / 1000;
    // }

    private void updateUploadInfo( String status ) {
        assert request != null;
        long delta = ( System.currentTimeMillis() - startTime ) / 1000;
        if ( log.isDebugEnabled() ) {
            log.debug( "Updating info: Status=" + status + ", " + totalBytesRead + " bytes read" );
        }
        request.getSession().setAttribute( "uploadInfo",
                new UploadInfo( totalFiles, totalToRead, totalBytesRead, delta, status ) );
    }
}
