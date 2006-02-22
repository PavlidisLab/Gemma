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

/**
 * Holds information on how far an upload has proceeded.
 * 
 * @author Original : plosson on 05-janv.-2006 10:46:33 - Last modified by Author: plosson $ on $Date: 2006/01/05
 *         10:09:38
 * @author pavlidis
 * @version  $Id$
 */
public class UploadInfo {
    private long totalSize = 0;
    private long bytesRead = 0;
    private long elapsedTime = 0;
    private String status = "done";
    private int fileIndex = 0;

    public UploadInfo() {
    }

    public UploadInfo( int fileIndex, long totalSize, long bytesRead, long elapsedTime, String status ) {
        this.fileIndex = fileIndex;
        this.totalSize = totalSize;
        this.bytesRead = bytesRead;
        this.elapsedTime = elapsedTime;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus( String status ) {
        this.status = status;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize( long totalSize ) {
        this.totalSize = totalSize;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public void setBytesRead( long bytesRead ) {
        this.bytesRead = bytesRead;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime( long elapsedTime ) {
        this.elapsedTime = elapsedTime;
    }

    public boolean isInProgress() {
        return "progress".equals( status ) || "start".equals( status );
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex( int fileIndex ) {
        this.fileIndex = fileIndex;
    }
}
