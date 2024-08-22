package ubic.gemma.core.loader.util.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Factory for creating {@link FTPClient}.
 * @author poirigui
 */
public interface FTPClientFactory {

    /**
     * Set the maximum number of idle FTP connections to keep in the pool.
     */
    void setMaxIdleConnections( int maxIdle );

    /**
     * Set the maximum number of FTP connections.
     */
    void setMaxTotalConnections( int maxTotal );

    /**
     * Create an FTP client for the given URL.
     * <p>
     * Once you're done with the client, you should consider recycling it with {@link #recycleClient(URL, FTPClient)}.
     * However, if you do so, make sure that all pending commands have completed with {@link FTPClient#completePendingCommand()}.
     */
    FTPClient getFtpClient( URL url ) throws IOException;

    /**
     * Destroy an FTP client that is known to be no-longer valid.
     */
    void destroyClient( URL url, FTPClient client );

    /**
     * Recycle the FTP client so that it might be reused in the future.
     */
    void recycleClient( URL url, FTPClient client );

    /**
     * Open an input stream for the given FTP URL.
     * <p>
     * The FTP client will be recycled once the stream is closed.
     */
    InputStream openStream( URL url ) throws IOException;
}
