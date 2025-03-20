package ubic.gemma.core.loader.util.fetcher;

import java.io.File;
import java.util.Collection;

/**
 * Interface for classes that can fetch files from a remote location and copy them to a specified location.
 *
 * @author pavlidis
 */
public interface Fetcher {

    /**
     * Fetch files according to the identifier provided.
     *
     * @param identifier identifier
     * @return local files
     */
    Collection<File> fetch( String identifier );

    /**
     * Set whether existing files should be overwritten.
     *
     * @param force new force value
     */
    @SuppressWarnings({ "unused", "WeakerAccess" })
    // Ensures consistency
    void setForce( boolean force );

}