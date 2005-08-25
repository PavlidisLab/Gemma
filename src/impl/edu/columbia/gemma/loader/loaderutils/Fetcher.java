package edu.columbia.gemma.loader.loaderutils;

import java.util.Collection;

import edu.columbia.gemma.common.description.LocalFile;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface Fetcher {

    /**
     * Fetch files according to the identifier provided.
     * 
     * @param identifier
     */
    public Collection<LocalFile> fetch( String identifier );

    /**
     * Set whether existing files should be overwritten.
     * 
     * @param force
     */
    public void setForce( boolean force );

}