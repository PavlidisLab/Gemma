package edu.columbia.gemma.loader.loaderutils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;

public class FtpFetcher extends Fetcher {

    protected FTPClient f;

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
                f.disconnect();
                throw new IOException( "Could not create output directory " + newDir );
            }
            log.info( "Created directory " + newDir.getAbsolutePath() );
        }
        return newDir;
    }

}
