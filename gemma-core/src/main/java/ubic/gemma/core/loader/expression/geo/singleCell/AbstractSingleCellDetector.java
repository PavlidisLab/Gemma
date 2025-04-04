package ubic.gemma.core.loader.expression.geo.singleCell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.ProgressInputStream;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryCallable;
import ubic.gemma.core.util.SimpleRetryPolicy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public abstract class AbstractSingleCellDetector implements SingleCellDetector {

    protected final Log log = LogFactory.getLog( getClass() );

    private FTPClientFactory ftpClientFactory;
    private Path downloadDirectory;
    private SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( new SimpleRetryPolicy( 3, 1000, 1.5 ), IOException.class, getClass().getName() );

    /**
     * Set the FTP client factory to use for downloading data over FTP.
     */
    public void setFTPClientFactory( FTPClientFactory ftpClientFactory ) {
        this.ftpClientFactory = ftpClientFactory;
    }

    public Path getDownloadDirectory() {
        return downloadDirectory;
    }

    /**
     * Set the download directory to use for storing single-cell data.
     * <p>
     * It may be left unset if downloading is not intended or needed.
     */
    @Override
    public void setDownloadDirectory( Path downloadDirectory ) {
        this.downloadDirectory = downloadDirectory;
    }

    @Override
    public void setRetryPolicy( SimpleRetryPolicy retryPolicy ) {
        this.retryTemplate = new SimpleRetry<>( retryPolicy, IOException.class, getClass().getName() );
    }

    /**
     * Retry the given callable a certain number of times before giving up.
     * @see SimpleRetry
     */
    protected <T> T retry( SimpleRetryCallable<T, IOException> callable, String what ) throws IOException {
        return retryTemplate.execute( callable, what );
    }

    /**
     * Check if a file at a given destination exists and has the size of a remote file.
     * @param dest                  file at destination
     * @param remoteFile            filename at origin
     * @param expectedContentLength expected size, or -1 if unknown
     * @param decompressIfNeeded    if true and the remote file is gzipped, will only check if the local file exists
     * @param storeCompressed       if true and the remote file is *not* gzipped, will only check if the local file exists
     */
    protected boolean existsAndHasExpectedSize( Path dest, String remoteFile, long expectedContentLength, boolean decompressIfNeeded, boolean storeCompressed ) throws IOException {
        if ( !Files.exists( dest ) ) {
            return false;
        }
        if ( decompressIfNeeded && remoteFile.endsWith( ".gz" ) ) {
            log.warn( "Cannot check if " + dest + " has expected size since it was decompressed." );
            return true;
        }
        if ( storeCompressed && !remoteFile.endsWith( ".gz" ) ) {
            log.warn( "Cannot check if " + dest + " has expected size since it was compressed for storage." );
            return true;
        }
        return expectedContentLength != -1 && dest.toFile().length() == expectedContentLength;
    }

    /**
     * Open a supplementary file as an input stream, possibly decompressing it.
     */
    protected InputStream openSupplementaryFileAsStream( String filename, int attempt, boolean decompressIfNeeded ) throws IOException {
        Assert.notNull( ftpClientFactory, "An FTP client factory must be set" );
        URL url = new URL( filename );
        String what = filename;
        if ( attempt > 0 ) {
            what += " (attempt #" + ( attempt + 1 ) + ")";
        }
        long sizeInBytes = getSizeInBytes( filename );
        InputStream stream;
        if ( url.getProtocol().equals( "ftp" ) ) {
            stream = new BufferedInputStream( new ProgressInputStream( ftpClientFactory.openStream( url ), what, getClass().getName(), sizeInBytes ) );
        } else {
            stream = new BufferedInputStream( new ProgressInputStream( url.openStream(), what, getClass().getName(), sizeInBytes ) );
        }
        if ( decompressIfNeeded && filename.endsWith( ".gz" ) ) {
            return new GZIPInputStream( stream );
        } else {
            return stream;
        }
    }

    protected long getSizeInBytes( String remoteFile ) throws IOException {
        Assert.notNull( ftpClientFactory, "An FTP client factory must be set for checking remote file size." );
        URL url = new URL( remoteFile );
        long expectedContentLength;
        if ( url.getProtocol().equals( "ftp" ) ) {
            expectedContentLength = retry( ( ctx ) -> {
                FTPClient client = ftpClientFactory.getFtpClient( url );
                try {
                    FTPFile res = client.mlistFile( url.getPath() );
                    long ret = res != null ? res.getSize() : -1;
                    ftpClientFactory.recycleClient( url, client );
                    return ret;
                } catch ( Exception e ) {
                    ftpClientFactory.destroyClient( url, client );
                    throw e;
                }
            }, "checking the size of " + remoteFile );
        } else {
            URLConnection conn = null;
            try {
                conn = new URL( remoteFile ).openConnection();
                expectedContentLength = conn.getContentLengthLong();
            } finally {
                if ( conn instanceof HttpURLConnection ) {
                    ( ( HttpURLConnection ) conn ).disconnect();
                }
            }
        }
        return expectedContentLength;
    }
}
