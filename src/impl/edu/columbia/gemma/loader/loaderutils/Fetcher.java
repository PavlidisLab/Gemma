package edu.columbia.gemma.loader.loaderutils;

import java.io.File;

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
     * Fetch a file from the indicated by the
     * 
     * @param identifier
     */
    public File fetch( String identifier );

}