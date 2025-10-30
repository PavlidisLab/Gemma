package ubic.gemma.core.loader.expression.geo.fetcher2;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import ubic.gemma.core.loader.expression.geo.service.GeoFormat;
import ubic.gemma.core.loader.expression.geo.service.GeoSource;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.ProgressReporterFactory;
import ubic.gemma.core.util.SimpleDownloader;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.locking.FileLockManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@CommonsLog
public class GeoFetcher {

    private final SimpleRetry<IOException> retryTemplate;
    private final SimpleDownloader simpleDownloader;
    private final Path geoSeriesDownloadPath;

    public GeoFetcher( SimpleRetryPolicy retryPolicy, Path geoSeriesDownloadPath ) {
        this.retryTemplate = new SimpleRetry<>( retryPolicy, IOException.class, getClass().getName() );
        // not retrying within the downloader itself since we want to use different fallback method to get the SOFT
        // files
        this.simpleDownloader = new SimpleDownloader( null );
        this.simpleDownloader.setCheckArchiveIntegrity( true );
        this.geoSeriesDownloadPath = geoSeriesDownloadPath;
    }

    public void setFtpClientFactory( FTPClientFactory ftpClientFactory ) {
        this.simpleDownloader.setFtpClientFactory( ftpClientFactory );
    }

    public void setFileLockManager( FileLockManager fileLockManager ) {
        this.simpleDownloader.setFileLockManager( fileLockManager );
    }

    public void setProgressReporterFactory( ProgressReporterFactory progressReporterFactory ) {
        this.simpleDownloader.setProgressReporterFactory( progressReporterFactory );
    }

    public void setTaskExecutor( ExecutorService taskExecutor ) {
        this.simpleDownloader.setTaskExecutor( taskExecutor );
    }

    public Path fetchSeriesFamilySoftFile( String accession ) throws IOException {
        Path dest = geoSeriesDownloadPath.resolve( accession ).resolve( accession + ".soft.gz" );
        return retryTemplate.execute( ( ctx ) -> {
            try {
                URL resource = GeoUtils.getUrlForSeriesFamily( accession, GeoSource.FTP, GeoFormat.SOFT );
                simpleDownloader.download( resource, dest, false );
                return dest;
            } catch ( IOException e ) {
                log.warn( "Retrieving SOFT file for " + accession + " via FTP failed, trying HTTPS.", e );
                try {
                    URL resource = GeoUtils.getUrlForSeriesFamily( accession, GeoSource.FTP_VIA_HTTPS, GeoFormat.SOFT );
                    simpleDownloader.download( resource, dest, false );
                    return dest;
                } catch ( IOException e1 ) {
                    log.warn( "Retrieving SOFT file for " + accession + " via HTTPS failed, trying via the GEO Browser directly.", e );
                    try {
                        return fetchSeriesFamilySoftFileFromGeoQuery( accession );
                    } catch ( IOException e2 ) {
                        e.addSuppressed( e1 );
                        e.addSuppressed( e2 );
                        throw e;
                    }
                }
            }
        }, "download SOFT file for " + accession );
    }

    Path fetchSeriesFamilySoftFileFromGeoQuery( String accession ) throws IOException {
        Path dest = geoSeriesDownloadPath.resolve( accession ).resolve( accession + ".soft.gz" );
        // last resort, ask GEO to generate the file
        URL resource = GeoUtils.getUrlForSeriesFamily( accession, GeoSource.DIRECT, GeoFormat.SOFT );
        Path uncompressedDest = geoSeriesDownloadPath.resolve( accession ).resolve( accession + ".soft" );
        simpleDownloader.download( resource, uncompressedDest, false );
        try {
            validateSoftFile( accession, uncompressedDest );
            try ( InputStream is = Files.newInputStream( uncompressedDest ); OutputStream out = new GZIPOutputStream( Files.newOutputStream( dest ) ) ) {
                long bytesWritten = IOUtils.copyLarge( is, out );
                log.info( "Compressed " + uncompressedDest + " to " + dest + ", final size is " + FileUtils.byteCountToDisplaySize( bytesWritten ) + "." );
            }
            return dest;
        } finally {
            log.info( "Deleting " + uncompressedDest );
            Files.delete( uncompressedDest );
        }
    }

    private void validateSoftFile( String accession, Path uncompressedDest ) throws IOException {
        // validate the soft file
        String firstLine;
        try ( Stream<String> s = Files.lines( uncompressedDest ) ) {
            firstLine = s.findFirst().orElse( null );
        }
        if ( firstLine == null || !firstLine.equals( "^SERIES = " + accession ) ) {
            throw new IOException( "Invalid SOFT file downloaded for " + accession + ", first line is: '" + firstLine + "', expected " + "'^SERIES = " + accession + "'." );
        }
    }
}
