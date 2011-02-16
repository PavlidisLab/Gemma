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
package ubic.gemma.loader.expression.smd.model;

import ubic.gemma.model.common.description.FileFormat;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.LocalFileImpl;

/**
 * <hr>
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SMDFile {

    private String downloadDate;
    private String downloadURL;
    private String localPath;
    private long size;

    public LocalFile toFile( FileFormat form ) {
        LocalFile f = new LocalFileImpl();

        f.setSize( new Long( this.size ) );
        f.setFormat( form );
        return f;
    }

    /**
     * @return Returns the downloadDate.
     */
    public String getDownloadDate() {
        return downloadDate;
    }

    /**
     * @param downloadDate The downloadDate to set.
     */
    public void setDownloadDate( String downloadDate ) {
        this.downloadDate = downloadDate;
    }

    /**
     * @return Returns the downloadURL.
     */
    public String getDownloadURL() {
        return downloadURL;
    }

    /**
     * @param downloadURL The downloadURL to set.
     */
    public void setDownloadURL( String downloadURL ) {
        this.downloadURL = downloadURL;
    }

    /**
     * @return Returns the localPath.
     */
    public String getLocalPath() {
        return localPath;
    }

    /**
     * @param localPath The localPath to set.
     */
    public void setLocalPath( String localPath ) {
        this.localPath = localPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize( long size ) {
        this.size = size;
    }
}