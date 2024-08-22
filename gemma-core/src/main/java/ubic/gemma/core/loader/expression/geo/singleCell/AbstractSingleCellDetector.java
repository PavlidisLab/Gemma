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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public abstract class AbstractSingleCellDetector implements SingleCellDetector {

    protected final Log log = LogFactory.getLog( getClass() );

    private FTPClientFactory ftpClientFactory;
    private Path downloadDirectory;

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

    /**
     * Retry the given callable a certain number of times before giving up.
     * @see SimpleRetry
     */
    protected <T> T retry( SimpleRetryCallable<T, IOException> callable, String what ) throws IOException {
        return new SimpleRetry<T, IOException>( what, 3, 1000, 1.5, IOException.class, getClass().getName() )
                .execute( callable );
    }

    /**
     * Check if a file at a given destination exists and has the size of a remote file.
     */
    protected boolean existsAndHasExpectedSize( Path dest, String remoteFile ) throws IOException {
        Assert.notNull( ftpClientFactory, "An FTP client factory must be set" );
        if ( !dest.toFile().exists() ) {
            return false;
        }
        long expectedContentLength = getSizeInBytes( remoteFile );
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
        InputStream stream;
        if ( url.getProtocol().equals( "ftp" ) ) {
            stream = new ProgressInputStream( ftpClientFactory.openStream( url ), what, getClass().getName(), getSizeInBytes( filename ) );
        } else {
            stream = new ProgressInputStream( new BufferedInputStream( url.openStream() ), what, getClass().getName(), getSizeInBytes( filename ) );
        }
        if ( decompressIfNeeded && filename.endsWith( ".gz" ) ) {
            return new GZIPInputStream( stream );
        } else {
            return stream;
        }
    }

    private long getSizeInBytes( String remoteFile ) throws IOException {
        URL url = new URL( remoteFile );
        long expectedContentLength;
        if ( url.getProtocol().equals( "ftp" ) ) {
            expectedContentLength = retry( ( attempt, lastAttempt ) -> {
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
