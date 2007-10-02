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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Commons FileItem that uses a MonitoredOutputStream
 * 
 * @author Original : plosson on 05-janv.-2006 10:46:33 - Last modified by Author: plosson on $Date: 2006/01/05 10:09:38
 * @author pavlidis
 * @version $Id$
 */
public class MonitoredDiskFileItem extends DiskFileItem {

    /**
     * 
     */
    private static final long serialVersionUID = 769886623757245271L;

    private static Log log = LogFactory.getLog( MonitoredDiskFileItem.class.getName() );

    private MonitoredOutputStream mos = null;
    private OutputStreamListener listener;

    /**
     * @param fieldName
     * @param contentType
     * @param isFormField
     * @param fileName
     * @param sizeThreshold
     * @param repositoryc
     * @param listener
     */
    public MonitoredDiskFileItem( String fieldName, String contentType, boolean isFormField, String fileName,
            int sizeThreshold, File repository, OutputStreamListener listener ) {
        super( fieldName, contentType, isFormField, fileName, sizeThreshold, repository );
        log.debug( "Creating " + getClass().getName() );
        this.listener = listener;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        log.debug( "Getting outputStream" );
        if ( mos == null ) {
            mos = new MonitoredOutputStream( super.getOutputStream(), listener );
        }
        return mos;
    }
}
