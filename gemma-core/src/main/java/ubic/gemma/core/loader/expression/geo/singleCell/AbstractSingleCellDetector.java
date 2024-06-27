package ubic.gemma.core.loader.expression.geo.singleCell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;

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

    protected int maxRetries = 3;
    protected int retryDelayMillis = 1000;
    protected FTPClientFactory ftpClientFactory;
    protected Path downloadDirectory;

    /**
     * Set the maximum number of allowed retries for downloading a file.
     * <p>
     * If zero, no retry is allowed.
     */
    public void setMaxRetries( int maxRetries ) {
        Assert.isTrue( maxRetries >= 0, "The maximum number of retries must be zero or greater." );
        this.maxRetries = maxRetries;
    }

    /**
     * The amount of time to wait between retries.
     * <p>
     * The actual delay is multiplied by a factor of {@code 1.5} for each failed attempt.
     * <p>
     * If zero, retries are immediate.
     */
    public void setRetryDelayMillis( int retryDelayMillis ) {
        Assert.isTrue( retryDelayMillis >= 0, "The retry delay must be zero or greater." );
        this.retryDelayMillis = retryDelayMillis;
    }

    public void setFTPClientFactory( FTPClientFactory ftpClientFactory ) {
        this.ftpClientFactory = ftpClientFactory;
    }

    @Override
    public void setDownloadDirectory( Path downloadDirectory ) {
        this.downloadDirectory = downloadDirectory;
    }

    /**
     * Check if a download can be retried after a given number of attempts and an exception occurred.
     * @param attempts number of attempts
     * @param e an exception
     */
    protected boolean isRetryable( int attempts, Exception e ) {
        Assert.isTrue( attempts >= 0, "The number of attempts must be zero or greater." );
        return attempts < maxRetries
                && e instanceof IOException;
    }

    /**
     * Backoff after a given number of retry attempts.
     */
    protected void backoff( int attempts ) {
        Assert.isTrue( attempts >= 0, "The number of attempts must be zero or greater." );
        try {
            Thread.sleep( ( long ) ( retryDelayMillis * Math.pow( 1.5, attempts ) ) );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Check if a file at a given destination exists and has the size of a remote file.
     */
    protected boolean existsAndHasExpectedSize( Path dest, String remoteFile ) throws IOException {
        Assert.notNull( ftpClientFactory, "An FTP client factory must be set" );
        if ( dest.toFile().exists() ) {
            URLConnection conn = null;
            try {
                URL url = new URL( remoteFile );
                long expectedContentLength;
                if ( url.getProtocol().equals( "ftp" ) ) {
                    expectedContentLength = -1;
                    for ( int i = 0; i <= maxRetries; i++ ) {
                        FTPClient client = ftpClientFactory.getFtpClient( url );
                        try {
                            expectedContentLength = client.mlistFile( url.getPath() ).getSize();
                            ftpClientFactory.recycleClient( url, client );
                        } catch ( IOException e ) {
                            ftpClientFactory.destroyClient( url, client );
                            if ( isRetryable( i, e ) ) {
                                log.warn( String.format( "Checking size of %s failed, retrying...", remoteFile ), e );
                                backoff( i );
                            } else {
                                throw e;
                            }
                        }
                    }
                } else {
                    conn = new URL( remoteFile ).openConnection();
                    expectedContentLength = conn.getContentLengthLong();
                }
                return expectedContentLength != -1 && dest.toFile().length() == expectedContentLength;
            } finally {
                if ( conn instanceof HttpURLConnection ) {
                    ( ( HttpURLConnection ) conn ).disconnect();
                }
            }
        }
        return false;
    }

    /**
     * Open a supplementary file as an input stream, possibly decompressing it.
     */
    protected InputStream openSupplementaryFileAsStream( String filename, boolean decompress ) throws IOException {
        Assert.notNull( ftpClientFactory, "An FTP client factory must be set" );
        URL url = new URL( filename );
        InputStream stream;
        if ( url.getProtocol().equals( "ftp" ) ) {
            stream = ftpClientFactory.openStream( url );
        } else {
            stream = new BufferedInputStream( url.openStream() );
        }
        if ( decompress && filename.endsWith( ".gz" ) ) {
            return new GZIPInputStream( stream );
        } else {
            return stream;
        }
    }
}
