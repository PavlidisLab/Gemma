package edu.columbia.gemma.loader;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class FileName {
    private String externalDb;
    private String fileName;

    /**
     * @return Returns the externalDb.
     */
    public String getExternalDb() {
        return externalDb;
    }

    /**
     * @return Returns the fileName.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param externalDb The externalDb to set.
     */
    public void setExternalDb( String externalDb ) {
        this.externalDb = externalDb;
    }

    /**
     * @param fileName The fileName to set.
     */
    public void setFileName( String fileName ) {
        this.fileName = fileName;
    }
}