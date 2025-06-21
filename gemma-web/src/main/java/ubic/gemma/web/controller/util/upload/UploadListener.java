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
package ubic.gemma.web.controller.util.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * This is created when a multipart request is received (via the CommonsMultipartMonitoredResolver). It starts of a
 * 'progress job',
 *
 * @author Original : plosson
 * @author pavlidis
 */
public class UploadListener implements OutputStreamListener {

    private static final Log log = LogFactory.getLog( UploadListener.class.getName() );

    private final HttpServletRequest request;
    private final long totalToRead;
    private long totalBytesRead = 0;
    private int totalFiles = -1;

    public UploadListener( HttpServletRequest request ) {
        this.request = request;
        this.totalToRead = request.getContentLength();
    }

    @Override
    public void bytesRead( int bytesRead ) {
        totalBytesRead = totalBytesRead + bytesRead;
        updateUploadInfo( "Reading" );
    }

    @Override
    public void done() {
        updateUploadInfo( "done" );
    }

    @Override
    public void error( String message ) {
        log.error( "There was an error in uploading a file in " + this );
    }

    @Override
    public void start() {
        updateUploadInfo( "Start" );
        totalFiles++;
    }

    private void updateUploadInfo( String status ) {
        request.getSession()
                .setAttribute( "uploadInfo", new UploadInfo( totalFiles, totalToRead, totalBytesRead, status ) );
    }
}
