package ubic.gemma.core.loader.util.fetcher2;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.ProgressInputStream;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractFetcher {

    protected final Log log = LogFactory.getLog( getClass() );

    private final FTPClientFactory ftpClientFactory;

    protected AbstractFetcher( FTPClientFactory ftpClientFactory ) {
        this.ftpClientFactory = ftpClientFactory;
    }

    protected void downloadViaFtp( String accession, URL resource, Path dest ) throws IOException {
        FTPClient client = ftpClientFactory.getFtpClient( resource );
        boolean download = true;
        long expectedLength;
        try {
            FTPFile f = client.mlistFile( resource.getFile() );
            expectedLength = f != null ? f.getSize() : -1;
            if ( expectedLength != -1 && Files.exists( dest ) && Files.size( dest ) == expectedLength ) {
                log.info( accession + ": Using existing SOFT file " + dest + "." );
                download = false;
            }
            ftpClientFactory.recycleClient( resource, client );
        } catch ( Exception e ) {
            ftpClientFactory.destroyClient( resource, client );
            throw e;
        }
        if ( download ) {
            log.info( accession + ": Downloading " + resource + " to " + dest + "..." );
            PathUtils.createParentDirectories( dest );
            StopWatch timer = StopWatch.createStarted();
            try ( InputStream in = new ProgressInputStream( ftpClientFactory.openStream( resource ), accession + ".soft.gz", getClass().getName(), expectedLength ); OutputStream out = Files.newOutputStream( dest ) ) {
                long downloadedBytes = IOUtils.copyLarge( in, out );
                if ( expectedLength != -1 && downloadedBytes != expectedLength ) {
                    throw new IOException( "Unexpected size of downloaded file: " + dest + ". Expected " + expectedLength + " bytes, but got " + downloadedBytes + " bytes." );
                }
                log.info( String.format( "%s: Done downloading file (%s in %s @ %.3f MB/s).", accession,
                        FileUtils.byteCountToDisplaySize( downloadedBytes ), timer,
                        ( 1000.0 / ( 1000.0 * 1000.0 ) ) * ( downloadedBytes / timer.getTime() ) ) );
            } catch ( IOException e ) {
                if ( Files.exists( dest ) ) {
                    log.warn( accession + ": An I/O error occurred while downloading the file, removing " + dest + "...", e );
                    Files.delete( dest );
                }
                throw e;
            }
        }
    }

    protected void downloadViaHttps( String accession, URL resource, Path dest ) throws IOException {
        URLConnection connection = resource.openConnection();
        try {
            int expectedLength = connection.getContentLength();
            if ( expectedLength != -1 && Files.exists( dest ) && Files.size( dest ) == expectedLength ) {
                log.info( accession + ": Using existing file " + dest + "." );
                return;
            }
            log.info( accession + ": Downloading " + resource + " to " + dest + "..." );
            try ( InputStream in = new ProgressInputStream( connection.getInputStream(), accession + ".soft.gz", getClass().getName(), expectedLength ); OutputStream out = Files.newOutputStream( dest ) ) {
                long downloadedBytes = IOUtils.copyLarge( in, out );
                if ( expectedLength != -1 && downloadedBytes != expectedLength ) {
                    throw new IOException( "Unexpected size of downloaded file: " + dest + ". Expected " + expectedLength + " bytes, but got " + downloadedBytes + " bytes." );
                }
            }
        } catch ( IOException e ) {
            if ( Files.exists( dest ) ) {
                log.warn( accession + ": An I/O error occurred while downloading the file, removing " + dest + "...", e );
                Files.delete( dest );
            }
            throw e;
        } finally {
            if ( connection instanceof HttpsURLConnection ) {
                ( ( HttpsURLConnection ) connection ).disconnect();
            }
        }
    }
}
