package ubic.gemma.core.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

/**
 * Network / transfer-rate helpers.
 * <p>
 * The FTP helpers ({@link #checkForFile}, {@link #connect}, {@link #ftpDownloadFile},
 * {@link #ftpFileSize}) were ported in-tree from {@code ubic.basecode.util.NetUtils}
 * as part of the Phase 3 baseCode util retirement (see
 * {@code BASECODE_DEP_AUDIT.md}). They have been merged into the existing in-tree
 * {@code NetUtils} (which already housed {@link #bytePerSecondToDisplaySize}) rather
 * than living in a separate class — the audit hint and the call-site layout both
 * point at one network-utility home.
 */
public class NetUtils {

    private static final Logger log = LoggerFactory.getLogger( NetUtils.class );

    public static String bytePerSecondToDisplaySize( double bytesPerSecond ) {
        if ( bytesPerSecond < 1e3 ) {
            return String.format( "%.2f B/s", bytesPerSecond );
        } else if ( bytesPerSecond < 1e6 ) {
            return String.format( "%.2f KB/s", ( bytesPerSecond / 1e3 ) );
        } else if ( bytesPerSecond < 1e9 ) {
            return String.format( "%.2f MB/s", ( bytesPerSecond / 1e6 ) );
        } else {
            return String.format( "%.2f GB/s", ( bytesPerSecond / 1e9 ) );
        }
    }

    /**
     * Determine if a file exists on the remote server.
     *
     * @return the size of the file
     * @throws FileNotFoundException if the file does not exist.
     * @throws IOException           on other IO errors.
     */
    public static long checkForFile( FTPClient f, String seekFile ) throws IOException {
        f.enterLocalPassiveMode();
        FTPFile[] allfilesInGroup = f.listFiles( seekFile );
        if ( allfilesInGroup == null || allfilesInGroup.length == 0 ) {
            throw new FileNotFoundException( "File " + seekFile + " does not seem to exist on the remote host" );
        }
        return allfilesInGroup[0].getSize();
    }

    /**
     * Convenience method to get an FTP connection.
     */
    public static FTPClient connect( int mode, String host, String loginName, String password ) throws SocketException,
        IOException {
        FTPClient f = new FTPClient();
        f.enterLocalPassiveMode();
        f.setBufferSize( 32 * 2 ^ 20 );
        boolean success = false;
        f.connect( host );
        int reply = f.getReplyCode();
        if ( FTPReply.isPositiveCompletion( reply ) ) success = f.login( loginName, password );
        if ( !success ) {
            f.disconnect();
            throw new IOException( "Couldn't connect to " + host );
        }
        f.setFileType( mode );
        log.debug( "Connected to " + host );
        return f;
    }

    /**
     * Download a file via FTP, skipping if a complete local copy already exists.
     *
     * @return boolean indicating success or failure.
     */
    public static boolean ftpDownloadFile( FTPClient f, String seekFile, File outputFile, boolean force )
        throws IOException {
        boolean success;

        assert f != null && f.isConnected() : "No FTP connection is available";
        f.enterLocalPassiveMode();

        long expectedSize = checkForFile( f, seekFile );

        if ( outputFile.exists() && outputFile.length() == expectedSize && !force ) {
            log.info( "Output file " + outputFile + " already exists with correct size. Will not re-download" );
            return true;
        }

        OutputStream os = new FileOutputStream( outputFile );

        log.debug( "Seeking file " + seekFile + " with size " + expectedSize + " bytes" );
        success = f.retrieveFile( seekFile, os );
        os.close();
        if ( !success ) {
            throw new IOException( "Failed to complete download of " + seekFile );
        }
        return success;
    }

    /**
     * Download a file via FTP to the given local path.
     *
     * @return boolean indicating success or failure.
     */
    public static boolean ftpDownloadFile( FTPClient f, String seekFile, String outputFileName, boolean force )
        throws IOException {
        f.enterLocalPassiveMode();
        return ftpDownloadFile( f, seekFile, new File( outputFileName ), force );
    }

    /**
     * Get the size of a remote file.
     */
    public static long ftpFileSize( FTPClient f, String seekFile ) throws IOException {
        if ( f == null || !f.isConnected() ) {
            throw new IOException( "No FTP connection" );
        }

        f.enterLocalPassiveMode();

        int maxTries = 3;
        for ( int i = 0; i < maxTries; i++ ) {
            FTPFile[] files = f.listFiles( seekFile );
            if ( files.length == 1 ) {
                return files[0].getSize();
            } else if ( files.length > 1 ) {
                throw new IOException( files.length + " files found when expecting one" );
            } // otherwise keep trying.
        }

        throw new FileNotFoundException( "Didn't get expected file information for " + seekFile + " (" + maxTries
            + " attempts)" );
    }
}
