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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is created when a multipart request is received (via the CommonsMultipartMonitoredResolver). It starts of a
 * 'progress job',
 * 
 * @author Original : plosson
 * @author pavlidis
 * @version $Id$
 */
public class UploadListener implements OutputStreamListener {

    private static Log log = LogFactory.getLog( UploadListener.class.getName() );

    /**
     * Set this while debugging. A value of 10-50 is sufficient.
     */
    private long delay = 0;

    private long totalToRead = 0;
    private long totalBytesRead = 0;
    private HttpServletRequest request;
    private int totalFiles = -1;

    /**
     * @param request
     * @param debugDelay
     */
    public UploadListener( HttpServletRequest request, long debugDelay ) {
        this( request );
        this.delay = debugDelay;
    }

    public UploadListener( HttpServletRequest request ) {
        this.request = request;
        this.totalToRead = request.getContentLength();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#start()
     */
    @Override
    public void start() {
        updateUploadInfo( "Start" );
        totalFiles++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#bytesRead(int)
     */
    @Override
    public void bytesRead( int bytesRead ) {
        totalBytesRead = totalBytesRead + bytesRead;
        updateUploadInfo( "Reading" );

        /*
         * For debugging, slow things down
         */
        if ( delay > 0 ) {
            try {
                Thread.sleep( delay );
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    private void updateUploadInfo( String status ) {
        request.getSession().setAttribute( "uploadInfo",
                new UploadInfo( totalFiles, totalToRead, totalBytesRead, status ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#error(java.lang.String)
     */
    @Override
    public void error( String message ) {
        log.error( "There was an error in uploading a file in " + this );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#done()
     */
    @Override
    public void done() {
        updateUploadInfo( "done" );
    }

}
