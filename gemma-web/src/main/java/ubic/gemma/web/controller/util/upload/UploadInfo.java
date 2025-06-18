/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.web.controller.util.upload;

import java.io.Serializable;

/* Licence:
 *   Use this however/wherever you like, just don't blame me if it breaks anything.
 *
 * Credit:
 *   If you're nice, you'll leave this bit:
 *
 *   Class by Pierre-Alexandre Losson -- http://www.telio.be/blog
 *   email : plosson@users.sourceforge.net
 */

/**
 * @author Original : plosson on 06-janv.-2006 12:19:14 - Last modified by $Author$ on $Date: 2004/11/26 22:43:57
 *         $
 *
 */
public class UploadInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private long totalSize = 0;
    private long bytesRead = 0;
    private String status = "done";
    private int fileIndex = 0;

    public UploadInfo() {
    }

    public UploadInfo( int fileIndex, long totalSize, long bytesRead, String status ) {
        this.fileIndex = fileIndex;
        this.totalSize = totalSize;
        this.bytesRead = bytesRead;
        this.status = status;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public String getStatus() {
        return status;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public boolean isInProgress() {
        return "progress".equals( status ) || "start".equals( status );
    }

    public void setBytesRead( long bytesRead ) {
        this.bytesRead = bytesRead;
    }

    public void setFileIndex( int fileIndex ) {
        this.fileIndex = fileIndex;
    }

    public void setStatus( String status ) {
        this.status = status;
    }

    public void setTotalSize( long totalSize ) {
        this.totalSize = totalSize;
    }
}
