package edu.columbia.gemma.loader.smd.model;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.FileFormat;
import edu.columbia.gemma.common.description.LocalFileImpl;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SMDFile {

    private String downloadDate;
    private String downloadURL;
    private String localPath;
    private long size;

    public LocalFile toFile( FileFormat form ) {
        LocalFile f = new LocalFileImpl();

        f.setSize( ( int ) this.size );
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