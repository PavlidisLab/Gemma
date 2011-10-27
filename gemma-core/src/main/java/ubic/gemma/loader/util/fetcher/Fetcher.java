package ubic.gemma.loader.util.fetcher;

import java.util.Collection;

import ubic.gemma.model.common.description.LocalFile;

/**
 * Interface for classes that can fetch files from a remote location and copy them to a specified location.
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