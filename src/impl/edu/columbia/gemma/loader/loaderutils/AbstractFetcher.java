package edu.columbia.gemma.loader.loaderutils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractFetcher implements Fetcher {

    protected static Log log = LogFactory.getLog( AbstractFetcher.class.getName() );
    protected String localBasePath = null;
    protected String baseDir = null;
    protected boolean success = false;
    protected boolean force = false;

    /**
     * Set to true if downloads should proceed even if the file already exists.
     * 
     * @param force
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * Create a directory according to the current accession number and set path information.
     * 
     * @param accession
     * @return
     * @throws IOException
     */
    protected File mkdir( String accession ) throws IOException {
        File newDir = new File( localBasePath + "/" + accession );
        if ( !newDir.exists() ) {
            success = newDir.mkdir();
            if ( !success ) {
                throw new IOException( "Could not create output directory " + newDir );
            }
            log.info( "Created directory " + newDir.getAbsolutePath() );
        }

        if ( !newDir.canWrite() ) {
            throw new IOException( "Cannot write to target directory " + newDir.getAbsolutePath() );
        }

        return newDir;
    }

}
