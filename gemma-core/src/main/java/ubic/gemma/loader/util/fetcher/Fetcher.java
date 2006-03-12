package ubic.gemma.loader.util.fetcher;

import java.util.Collection;

import ubic.gemma.model.common.description.LocalFile;

/**
 * Interface for classes that can fetch files from a remote location and copy them to a specified location.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
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

    // /**
    // * @param uri The base URI. This is concatenated with the identifier to form the file URIs to be handled by this.
    // */
    // public void setBaseUri( URI uri );
    //
    // /**
    // * @param uri The location where fetched files will be placed. If it does not exist, and a directory can be
    // created,
    // * it will be created by this.
    // */
    // public void setTargetUri( URI uri );

}