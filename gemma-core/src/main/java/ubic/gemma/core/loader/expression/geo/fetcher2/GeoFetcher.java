package ubic.gemma.core.loader.expression.geo.fetcher2;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.service.GeoFormat;
import ubic.gemma.core.loader.expression.geo.service.GeoSource;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.SimpleDownloader;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.locking.FileLockManager;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

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

    public void setTaskExecutor( ExecutorService taskExecutor ) {
        this.simpleDownloader.setTaskExecutor( taskExecutor );
    }

    public Path fetchSeriesFamilySoftFile( String accession ) throws IOException {
        Path dest = geoSeriesDownloadPath.resolve( accession ).resolve( accession + ".soft.gz" );
        return retryTemplate.execute( ( ctx ) -> {
            try {
                URL resource = GeoUtils.getUrlForSeriesFamily( accession, GeoSource.FTP, GeoFormat.SOFT );
                simpleDownloader.download( resource, dest, false );
            } catch ( IOException e ) {
                log.warn( "Retrieving SOFT file for " + accession + " via FTP failed, trying HTTPS.", e );
                try {
                    URL resource = GeoUtils.getUrlForSeriesFamily( accession, GeoSource.FTP_VIA_HTTPS, GeoFormat.SOFT );
                    simpleDownloader.download( resource, dest, false );
                } catch ( IOException e1 ) {
                    log.warn( "Retrieving SOFT file for " + accession + " via HTTPS failed, trying via the GEO Browser directly.", e );
                    // last resort, ask GEO to generate the file
                    URL resource = GeoUtils.getUrlForSeriesFamily( accession, GeoSource.QUERY, GeoFormat.SOFT );
                    simpleDownloader.download( resource, dest, false );
                }
            }
            return dest;
        }, "download SOFT file for " + accession );
    }
}
